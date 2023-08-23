package com.github.classops.exoplayer.ext.silk

import java.nio.ByteBuffer

object Silk {

    const val MAGIC_SILK = "#!SILK_V3"
    const val SILK_MIME_TYPE = "audio/slk"
    const val MAX_INPUT_FRAMES = 5
    const val FRAME_LENGTH_MS = 20
    const val MAX_API_FS_KHZ = 48
    const val MAX_FRAME_SIZE = (FRAME_LENGTH_MS * MAX_API_FS_KHZ shl 1) * MAX_INPUT_FRAMES

    init {
        System.loadLibrary("ext-silk")
    }

    external fun createSilkDecoder(sampleRate: Int): Long

    external fun getFrameSize(psDec: Long): Int

    external fun getMoreInternalDecoderFrames(psDec: Long): Int

    external fun getFramesPerPacket(psDec: Long): Int

    /**
     * 解码SILK帧
     * @return 返回解码字节数（而非采样数）
     */
    external fun decode(
        pDec: Long, lostFlag: Int, inData: ByteArray, inOffset: Int, length: Int,
        outData: ByteArray, outOffset: Int
    ): Int

    /**
     * 解码SILK帧
     * @return 返回解码字节数（而非采样数）
     */
    external fun decodeByteBuffer(
        pDec: Long, lostFlag: Int, inData: ByteArray, inOffset: Int, length: Int,
        outData: ByteBuffer
    ): Int

    external fun searchForLBRR(
        payloadBuff: ByteArray, offset: Int, length: Int,
        lossOffset: Int, payloadFEC: ByteArray
    ): Int

    external fun releaseDecoder(pDec: Long)

    external fun pcmToSilk(src: String?, dest: String?, rate: Int): Int
}