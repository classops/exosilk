package com.github.classops.exoplayer.ext.silk

import android.util.Log
import com.google.android.exoplayer2.decoder.DecoderInputBuffer
import com.google.android.exoplayer2.decoder.SimpleDecoder
import com.google.android.exoplayer2.decoder.SimpleDecoderOutputBuffer
import com.google.android.exoplayer2.util.Util


/**
 * 文件名：SilkDecoder <br/>
 * 描述：Silk解码，补充数据获取是否 lost
 *
 * @author wangmingshuo
 * @since 2023/01/09 14:16
 */
class SilkDecoder(numInputBuffers: Int, numOutputBuffers: Int) :
    SimpleDecoder<DecoderInputBuffer, SimpleDecoderOutputBuffer, SilkDecoderException?>(
        arrayOfNulls<DecoderInputBuffer>(numInputBuffers) as Array<out DecoderInputBuffer>,
        arrayOfNulls<SimpleDecoderOutputBuffer>(numOutputBuffers)  as Array<out SimpleDecoderOutputBuffer>
    ) {

    companion object {
        private const val TAG = "SilkDecoder"
    }

    private val psDec = Silk.createSilkDecoder(24_000)

    override fun getName(): String {
        return "libsilk"
    }

    override fun decode(
        inputBuffer: DecoderInputBuffer,
        outputBuffer: SimpleDecoderOutputBuffer,
        reset: Boolean
    ): SilkDecoderException? {
        if (reset) {
            flush()
        }
        val lost = if (inputBuffer.hasSupplementalData()) {
            val supplementalData = Util.castNonNull(inputBuffer.supplementalData)
            supplementalData[0] != 0.toByte()
        } else {
            false
        }
        val inputDataBuffer = Util.castNonNull(inputBuffer.data)
        val inputData = inputDataBuffer.array()
        val outputData = outputBuffer.init(inputBuffer.timeUs, Silk.MAX_FRAME_SIZE)
        if (lost) {
            Log.e(TAG, "loss")
            var outPos = 0
            for (i in 0 until Silk.getFramesPerPacket(psDec)) {
                val nBytes = if (outputData.isDirect) {
                    Silk.decodeByteBuffer(psDec, 1, inputData, 0, inputData.size, outputData)
                } else {
                    Silk.decode(psDec, 1, inputData, 0, inputData.size, outputData.array(), 0)
                }
                if (nBytes > 0) {
                    outPos += nBytes
                }
            }
            if (outPos > 0) {
                outputData.position(0)
                outputData.limit(outPos)
            } else {
                Log.e(TAG, "loss")
            }
        } else {
            do {
                val nBytes = if (outputData.isDirect) {
                    Silk.decodeByteBuffer(psDec, 0, inputData, 0, inputData.size, outputData)
                } else {
                    Silk.decode(psDec, 0, inputData, 0, inputData.size, outputData.array(), 0)
                }
                // Log.v(TAG, "decode input: ${inputData.size}, output: $nBytes")
                outputData.position(0)
                outputData.limit(nBytes)
            } while (Silk.getMoreInternalDecoderFrames(psDec) > 0)
        }
        return null
    }

    override fun createInputBuffer(): DecoderInputBuffer {
        return DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL)
    }

    override fun createOutputBuffer(): SimpleDecoderOutputBuffer {
        return SimpleDecoderOutputBuffer(this::releaseOutputBuffer)
    }

    override fun createUnexpectedDecodeException(error: Throwable): SilkDecoderException {
        return SilkDecoderException("Unexpected decode error", error);
    }

    override fun release() {
        super.release()
        Silk.releaseDecoder(this.psDec)
    }

}