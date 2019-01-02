package com.halilibo.sample

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.halilibo.bvpkotlin.BetterVideoPlayer
import com.halilibo.bvpkotlin.VideoCallback
import com.halilibo.bvpkotlin.captions.CaptionsView

class MainActivity : AppCompatActivity() {

    lateinit var bvp: BetterVideoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

        findViewById<View>(R.id.background_activity_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, BackgroundActivity::class.java))
        }

        findViewById<View>(R.id.fulscreen_activity_button).setOnClickListener {
            startActivity(Intent(this@MainActivity, FullscreenActivity::class.java))
        }

        bvp = findViewById(R.id.bvp)!!

        if (savedInstanceState == null) {
            bvp.setAutoPlay(true)
            bvp.setSource(Uri.parse("android.resource://" + packageName + "/" + R.raw.video))
            bvp.setCaptions(R.raw.sub, CaptionsView.SubMime.SUBRIP)
        }

        bvp.setHideControlsOnPlay(true)

        bvp.getToolbar().inflateMenu(R.menu.menu_dizi)
        bvp.getToolbar().overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_settings_white_24dp)
        bvp.getToolbar().setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_enable_swipe -> bvp.enableSwipeGestures(window)
                R.id.action_enable_double_tap -> bvp.enableDoubleTapGestures(5000)
                R.id.action_disable_swipe -> bvp.disableGestures()
                R.id.action_show_bottombar -> bvp.setBottomProgressBarVisibility(true)
                R.id.action_hide_bottombar -> bvp.setBottomProgressBarVisibility(false)
                R.id.action_show_captions -> bvp.setCaptions(R.raw.sub, CaptionsView.SubMime.SUBRIP)
                R.id.action_hide_captions -> bvp.removeCaptions()
            }
            false
        }

        bvp.enableSwipeGestures(window)

        bvp.setCallback(object : VideoCallback {
            override fun onStarted(player: BetterVideoPlayer) {
                Log.i(TAG, "Started")
            }

            override fun onPaused(player: BetterVideoPlayer) {
                Log.i(TAG, "Paused")
            }

            override fun onPreparing(player: BetterVideoPlayer) {
                Log.i(TAG, "Preparing")
            }

            override fun onPrepared(player: BetterVideoPlayer) {
                Log.i(TAG, "Prepared")
            }

            override fun onBuffering(percent: Int) {
                Log.i(TAG, "Buffering $percent")
            }

            override fun onError(player: BetterVideoPlayer, e: Exception) {
                Log.i(TAG, "Error " +e.message)
            }

            override fun onCompletion(player: BetterVideoPlayer) {
                Log.i(TAG, "Completed")
            }

            override fun onToggleControls(player: BetterVideoPlayer, isShowing: Boolean) {

            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            supportActionBar?.hide()
        } else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            supportActionBar?.show()
        }
    }

    public override fun onPause() {
        bvp.pause()
        super.onPause()
    }

    companion object {
        const val TAG = "MainActivity"
    }
}