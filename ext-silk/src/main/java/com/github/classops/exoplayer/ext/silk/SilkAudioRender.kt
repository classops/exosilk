package com.github.classops.exoplayer.ext.silk

import android.os.Handler
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.audio.DecoderAudioRenderer
import com.google.android.exoplayer2.decoder.CryptoConfig
import com.google.android.exoplayer2.util.Util


/**
 * 文件名：SilkAudioRender <br/>
 * 描述：Silk音频渲染，匹配格式支持，创建 Decoder 进行解码
 *
 * @author wangmingshuo
 * @since 2023/01/09 14:24
 */
class SilkAudioRender : DecoderAudioRenderer<SilkDecoder> {
    constructor(
        eventHandler: Handler?,
        eventListener: AudioRendererEventListener?,
        vararg audioProcessors: AudioProcessor
    ) : super(eventHandler, eventListener, *audioProcessors)

    constructor(
        eventHandler: Handler?,
        eventListener: AudioRendererEventListener?,
        audioSink: AudioSink
    ) : super(eventHandler, eventListener, audioSink)

    companion object {
        private const val TAG = "SilkAudioRender"
        const val AUDIO_SILK = "audio/slk"
    }

    override fun getName(): String {
        return TAG
    }

    override fun supportsFormatInternal(format: Format): Int {
        val drmIsSupported: Boolean = true
        return if (!AUDIO_SILK.equals(format.sampleMimeType, ignoreCase = true)) {
            C.FORMAT_UNSUPPORTED_TYPE
        } else if (!sinkSupportsFormat(
                Util.getPcmFormat(C.ENCODING_PCM_16BIT, format.channelCount, format.sampleRate)
            )
        ) {
            C.FORMAT_UNSUPPORTED_SUBTYPE
        } else if (!drmIsSupported) {
            C.FORMAT_UNSUPPORTED_DRM
        } else {
            C.FORMAT_HANDLED
        }
    }

    override fun createDecoder(format: Format, cryptoConfig: CryptoConfig?): SilkDecoder {
        return SilkDecoder(
            Silk.MAX_INPUT_FRAMES,
            1
        )
    }

    override fun getOutputFormat(decoder: SilkDecoder): Format {
        Log.w(TAG, "getOutputFormat")
        return Util.getPcmFormat(C.ENCODING_PCM_16BIT, 1, 24_000)
    }

}