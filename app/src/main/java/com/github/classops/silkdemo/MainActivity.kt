package com.github.classops.silkdemo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.classops.silkdemo.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.RawResourceDataSource


class MainActivity : AppCompatActivity(), Player.Listener {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val player by lazy {
        ExoPlayer.Builder(
            this,
            MyRendersFactory(this),
            DefaultMediaSourceFactory(this, MyExtractorsFactory())
        )
            .setTrackSelector(
                DefaultTrackSelector(
                    this,
                    AdaptiveTrackSelection.Factory()
                )
            )
            .build().apply {
            addListener(this@MainActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnNav.setOnClickListener {
            playSilk()
        }

    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        Log.e("test", "player state: ${playbackState}")
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        error.printStackTrace()
    }

    private fun playSilk() {
        val rawRes = RawResourceDataSource(this).apply {
            open(DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.test)))
        }
        player.setMediaItem(MediaItem.fromUri(rawRes.uri!!))
        player.prepare()
        player.play()
    }

}