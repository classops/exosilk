# ExoPlayer Extension Silk

**支持SILK格式的ExoPlayer2拓展，另外也支持微信、QQ语音slk文件播放（本身就是SILK格式，多了个STX的字符）。**


### 使用方法

库里实现了Silk的Extractor和Render，用于解码播放。
创建Player时，传入自定义的 ExtractorsFactory 和 RenderFactory, 和其他音频一样使用播放即可。

添加JitPack仓库
```agsl
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
添加依赖
```agsl
dependencies {
    implementation 'com.github.classops:exosilk:1.0'
}
```

#### 1. 添加自定义的ExtractorsFactory，添加 SilkExtractor
```kotlin
class MyExtractorsFactory : ExtractorsFactory {

    private val extractorsFactory = DefaultExtractorsFactory()

    override fun createExtractors(): Array<Extractor> {
        val list = arrayListOf<Extractor>(
            SilkExtractor()
        )
        list.addAll(extractorsFactory.createExtractors())
        return list.toTypedArray()
    }

}
```
#### 2. 添加自定义的RenderersFactory，添加 SilkAudioRender
```kotlin
class MyRendersFactory(context: Context) : DefaultRenderersFactory(context) {

    init {
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
        setMediaCodecSelector(object : MediaCodecSelector {
            override fun getDecoderInfos(
                mimeType: String,
                requiresSecureDecoder: Boolean,
                requiresTunnelingDecoder: Boolean
            ): MutableList<MediaCodecInfo> {
                return MediaCodecSelector.DEFAULT.getDecoderInfos(
                    mimeType,
                    requiresSecureDecoder,
                    requiresTunnelingDecoder
                )
            }
        })
    }

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        super.buildAudioRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            enableDecoderFallback,
            audioSink,
            eventHandler,
            eventListener,
            out
        )
        out.add(
            0,
            SilkAudioRender(eventHandler, eventListener, audioSink)
        )
    }
}
```
#### 3. 创建 Player，添加上面的Factory
```kotlin
val player = ExoPlayer.Builder(
    this,
    MyRendersFactory(this),
    DefaultMediaSourceFactory(this, MyExtractorsFactory())
).setTrackSelector(
    DefaultTrackSelector(
        this,
        AdaptiveTrackSelection.Factory()
    )
).build()
```

其他，播放只需添加silk源，即可完成播放。具体可参考Demo。

### 实现原理步骤

1. 注册 Extractor 完成原始帧的提取
2. 添加 JNI SILK 库，注册 Decoder 实现 silk 的帧解码
3. 添加 Render 完成 Decoder 的使用，并注册生效