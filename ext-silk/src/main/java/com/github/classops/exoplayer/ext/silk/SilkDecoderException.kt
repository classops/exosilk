package com.github.classops.exoplayer.ext.silk

import com.google.android.exoplayer2.decoder.DecoderException

/**
 * 文件名：SilkDecoderException <br/>
 * 描述：Silk解码异常
 *
 * @author wangmingshuo
 * @since 2023/01/09 14:17
 */
class SilkDecoderException : DecoderException {

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable?) : super(message, cause)

}