package com.halilibo.bvpkotlin

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.Window
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import androidx.appcompat.widget.Toolbar
import com.halilibo.bvpkotlin.captions.CaptionsView

/**
 * @author Aidan Follestad (halilozercan)
 * This interface defines which methods should be available to
 * library users. The methods found here constitutes the actual
 * interface of the player.
 */
internal interface IBetterVideoPlayer {

    /**
     * This enables users to have access to inner Toolbar that is
     * located on top bar.
     * @return current toolbar inside BVP
     */
    fun getToolbar(): Toolbar

    fun getCurrentPosition(): Int

    fun getDuration(): Int

    var hideControlsDuration: Int // defaults to 2 seconds.

    fun isPrepared(): Boolean

    fun isPlaying(): Boolean

    fun isControlsShown(): Boolean

    fun setSource(source: Uri)

    fun setSource(source: Uri, headers: Map<String, String> = mapOf())

    fun setCallback(callback: VideoCallback)

    fun setProgressCallback(callback: VideoProgressCallback)

    /**
     * There are 3 default buttons in BetterVideoPlayer: play, pause, restart.
     * You can set their drawable by using this method.
     * @param   type    Which button is being targeted.
     * @param   drawable    Drawable object to use for styling.
     */
    fun setButtonDrawable(type: BetterVideoPlayer.ButtonType, drawable: Drawable)

    fun setHideControlsOnPlay(hide: Boolean)

    fun setAutoPlay(autoPlay: Boolean)

    fun setVolume(@FloatRange(from = 0.0, to = 1.0) leftVolume: Float,
                  @FloatRange(from = 0.0, to = 1.0) rightVolume: Float)

    fun setLoop(loop: Boolean)

    fun setInitialPosition(pos: Int)

    fun setBottomProgressBarVisibility(isShowing: Boolean)

    /**
     * Enable swipe gestures which are volume control for up and down at right side of the player.
     * Horizontal swipes changes progress of the player.
     */
    fun enableSwipeGestures()

    /**
     * This method enables swipe gestures with brightness feature.
     * Reference to Current window is necessary to adjust brightness
     * @param window  Current windows e.g. getActivity().getWindow()
     */
    fun enableSwipeGestures(window: Window)

    /**
     * Double tap gestures are Youtube like. Seek forward or backward
     * after double tapping at one side.
     * @param seek
     */
    fun enableDoubleTapGestures(seek: Int)

    fun disableGestures()

    fun showToolbar()

    fun hideToolbar()

    fun showControls()

    fun hideControls()

    fun toggleControls()

    fun enableControls()

    fun disableControls()

    fun start()

    fun seekTo(pos: Int)

    fun pause()

    fun stop()

    fun reset()

    fun release()

    // TODO: move captions outside of BetterVideoPlayer.
    fun setCaptions(source: Uri?, subMime: CaptionsView.SubMime)

    fun setCaptions(@RawRes resId: Int, subMime: CaptionsView.SubMime)

    fun setCaptionLoadListener(listener: CaptionsView.CaptionsViewLoadListener?)

    fun removeCaptions()
}