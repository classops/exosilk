package com.github.classops.exoplayer.ext.silk

import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.extractor.Extractor
import com.google.android.exoplayer2.extractor.ExtractorInput
import com.google.android.exoplayer2.extractor.ExtractorOutput
import com.google.android.exoplayer2.extractor.PositionHolder
import com.google.android.exoplayer2.extractor.SeekMap
import com.google.android.exoplayer2.extractor.TrackOutput
import com.google.android.exoplayer2.util.ParsableByteArray
import com.google.android.exoplayer2.util.Util
import java.io.EOFException
import java.io.IOException


class SilkExtractor : Extractor {

    companion object {
        private const val TAG = "SilkExtractor"
        private const val MAX_LBRR_DELAY = 2
        private const val MAX_BYTES_PER_FRAME = 1024
        private const val MAX_INPUT_FRAMES = 5
    }

    enum class State {
        INIT,

        /**
         * 读取帧到缓冲
         */
        READ_FRAME,

        /**
         * 帧读取完毕，处理最后 MAX_LBRR_DELAY 个 帧
         */
        READ_RCV_BUFF,

        /**
         * 彻底读取结束
         */
        EOF
    }

    private var timeOffsetUs = 0L
    private var currentSampleTimeUs = 0L

    private lateinit var extractorOutput: ExtractorOutput
    private lateinit var trackOutput: TrackOutput
    private var isTencent = false
    private val frameScratch = ParsableByteArray(ByteArray(4))
    private lateinit var seekMap: SeekMap
    private var hasOutputSeekMap = false
    private var payloadPos: Int = 0
    private var payloadBuff = ParsableByteArray(MAX_BYTES_PER_FRAME * MAX_INPUT_FRAMES * (MAX_LBRR_DELAY + 1))
    private var payloadFECBuff = ParsableByteArray(MAX_BYTES_PER_FRAME * MAX_INPUT_FRAMES)
    private val sampleSizeData = ParsableByteArray()
    private val supplementalData = ParsableByteArray(8)
    private var state = State.INIT
    private var lastFrameIndex = MAX_LBRR_DELAY
    private var nBytesPerPacket = IntArray(MAX_LBRR_DELAY + 1)

    override fun sniff(input: ExtractorInput): Boolean {
        val bytes = ByteArray(10)
        input.peekFully(bytes, 0, bytes.size, true)
        return when {
            String(bytes, 0, Silk.MAGIC_SILK.length) == Silk.MAGIC_SILK -> {
                isTencent = false
                true
            }

            String(bytes, 1, Silk.MAGIC_SILK.length) == Silk.MAGIC_SILK -> {
                isTencent = true
                true
            }

            else -> false
        }
    }

    override fun init(output: ExtractorOutput) {
        this.extractorOutput = output
        this.trackOutput = output.track(0, C.TRACK_TYPE_AUDIO)
        output.endTracks()
    }

    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        when (this.state) {
            State.INIT -> {
                this.state = State.READ_FRAME
                return readHeaderAndLBRR(input)
            }

            State.READ_FRAME -> {
                return extractFrame(input)
            }

            State.READ_RCV_BUFF -> {
                return extractFrame(input)
            }

            else -> return Extractor.RESULT_END_OF_INPUT
        }
    }

    private fun readHeaderAndLBRR(input: ExtractorInput): Int {
        try {
            input.skipFully(
                if (isTencent) {
                    Silk.MAGIC_SILK.length + 1
                } else {
                    Silk.MAGIC_SILK.length
                }
            )
            // 预读取 MAX_LBRR_DELAY 个帧
            repeat(MAX_LBRR_DELAY) { index ->
                readNextFrame(input, index)
            }
        } catch (eof: EOFException) {
            return Extractor.RESULT_END_OF_INPUT
        } catch (e: IOException) {
            e.printStackTrace()
            return Extractor.RESULT_END_OF_INPUT
        }
        return Extractor.RESULT_CONTINUE
    }

    private fun extractFrame(input: ExtractorInput): Int {
        try {
            if (this.state == State.READ_FRAME) {
                // EOF 返回 -1
                if (readNextFrame(input, MAX_LBRR_DELAY) < 0) {
                    this.state = State.READ_RCV_BUFF
                    this.lastFrameIndex = 1
                    return Extractor.RESULT_CONTINUE
                }
            } else {
                lastFrameIndex--
                if (lastFrameIndex < 0) {
                    this.state = State.EOF
                }
            }

            var lost: Boolean
            var payloadToDec: ParsableByteArray? = null
            var nBytes = 0
            val currentNumBytes = nBytesPerPacket[0]
            if (currentNumBytes == 0) {
                lost = true
                Log.w(TAG, "loss, search LBRR")
                var payloadSearchPos = 0
                /* Packet loss. Search after FEC in next packets. Should be done in the jitter buffer */
                for (i in 1..MAX_LBRR_DELAY) {
                    if (nBytesPerPacket[i] > 0) {
                        val nBytesFEC = Silk.searchForLBRR(
                            payloadBuff.data,
                            payloadSearchPos,
                            nBytesPerPacket[i],
                            i,
                            payloadFECBuff.data
                        )
                        if (nBytesFEC > 0) {
                            payloadToDec = payloadFECBuff
                            nBytes = nBytesFEC
                            lost = false
                            break
                        }
                    }
                    payloadSearchPos += nBytesPerPacket[i]
                }
            } else {
                lost = false
                nBytes = currentNumBytes
                payloadToDec = payloadBuff
            }

            if (payloadToDec == null) return Extractor.RESULT_CONTINUE

            outputSample(payloadToDec, nBytes, lost)

            var totalBytes = 0
            for (i in 0 until MAX_LBRR_DELAY) {
                totalBytes += nBytesPerPacket[i + 1]
            }

            payloadPos -= currentNumBytes
            if (currentNumBytes > 0) {
                // memmove
                val srcData = payloadBuff.data
                System.arraycopy(
                    srcData,
                    currentNumBytes,
                    srcData,
                    0,
                    payloadPos
                )
            }
            // 移动数据
            System.arraycopy(
                nBytesPerPacket,
                1,
                nBytesPerPacket,
                0,
                MAX_LBRR_DELAY
            )
            nBytesPerPacket[MAX_LBRR_DELAY] = 0
        } catch (e: Exception) {
            e.printStackTrace()
            return Extractor.RESULT_END_OF_INPUT
        }

        return Extractor.RESULT_CONTINUE
    }

    private fun outputSample(
        outData: ParsableByteArray, length: Int, lost: Boolean, lastFrame: Boolean = false
    ) {
        trackOutput.format(
            Format.Builder()
                .setSampleMimeType(Silk.SILK_MIME_TYPE)
                .setChannelCount(1)
                .setPcmEncoding(Util.getPcmEncoding(C.ENCODING_PCM_16BIT))
                .setSampleRate(24_000)
                .build()
        )

        outData.position = 0
        currentSampleTimeUs += 20_000L

        supplementalData.reset(1)
        supplementalData.data[0] = if (lost) 1 else 0
        val supplementalSize = supplementalData.limit()
        sampleSizeData.reset(4)
        sampleSizeData.data[0] = length.shr(24).and(0xFF).toByte()
        sampleSizeData.data[1] = length.shr(16).and(0xFF).toByte()
        sampleSizeData.data[2] = length.shr(8).and(0xFF).toByte()
        sampleSizeData.data[3] = length.shr(0).and(0xFF).toByte()
        val totalDataSize = length + supplementalSize + sampleSizeData.limit()
        // 如果有 supplementalData 数据，采样数据的结构：
        // encryption data (if any) || sample size (4 bytes) || sample data || supplemental data
        trackOutput.sampleData(sampleSizeData, 4, TrackOutput.SAMPLE_DATA_PART_SUPPLEMENTAL)
        trackOutput.sampleData(outData, length)
        trackOutput.sampleData(supplementalData, supplementalSize, TrackOutput.SAMPLE_DATA_PART_SUPPLEMENTAL)
        val flags = if (lastFrame) {
            C.BUFFER_FLAG_KEY_FRAME or C.BUFFER_FLAG_HAS_SUPPLEMENTAL_DATA
        } else {
            C.BUFFER_FLAG_KEY_FRAME or C.BUFFER_FLAG_HAS_SUPPLEMENTAL_DATA or
                    C.BUFFER_FLAG_LAST_SAMPLE
        }
        trackOutput.sampleMetadata(
            currentSampleTimeUs,
            flags,
            totalDataSize,
            0,
            null
        )
        maybeSeekMap()
    }

    private fun maybeSeekMap() {
        if (hasOutputSeekMap) {
            return
        }
        seekMap = SeekMap.Unseekable(C.TIME_UNSET)
        extractorOutput.seekMap(seekMap)
        hasOutputSeekMap = true
    }

    override fun seek(position: Long, timeUs: Long) {
        // 暂时不做处理
        Log.d(TAG, "seek pos: $position, timeUs: $timeUs")
        currentSampleTimeUs = 0L
        timeOffsetUs = 0L
    }

    override fun release() {
        Log.d(TAG, "release")
    }

    /**
     * 读取数据
     */
    private fun readNextFrame(input: ExtractorInput, pos: Int = MAX_LBRR_DELAY): Int {
        if (pos < 0 || pos > MAX_LBRR_DELAY) return -1
        // read header
        val success = input.readFully(frameScratch.data, 0, 2, true)
        if (!success) {
            nBytesPerPacket[pos] = 0
            return -1
        }
        frameScratch.position = 0
        val len = frameScratch.readLittleEndianShort().toInt()
        if (len <= 0) {
            nBytesPerPacket[pos] = 0
            return 0
        }
        // read payload
        return if (input.readFully(payloadBuff.data, payloadPos, len, true)) {
            nBytesPerPacket[pos] = len
            payloadPos += len
            len
        } else {
            nBytesPerPacket[pos] = 0
            -1
        }
    }

}