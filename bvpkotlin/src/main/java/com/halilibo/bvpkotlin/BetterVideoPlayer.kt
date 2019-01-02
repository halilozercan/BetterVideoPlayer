package com.halilibo.bvpkotlin

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.annotation.CheckResult
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.halilibo.bvpkotlin.captions.CaptionsView
import com.halilibo.bvpkotlin.helpers.Util
import java.io.IOException
import java.util.*

/**
 * @author Aidan Follestad
 * Modified and improved by Halil Ozercan
 */
class BetterVideoPlayer @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr),
        IBetterVideoPlayer, TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnErrorListener,
        View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    override var hideControlsDuration: Int = 2000

    private var mPlayer: MediaPlayer? = null
    private var am: AudioManager? = null
    private var mSurface: Surface? = null
    private var mSubViewTextSize: Int = 0
    private var mSubViewTextColor: Int = 0

    /**
     * Window that holds the player. Necessary for setting brightness.
     */
    private var mWindow: Window? = null

    private lateinit var mControlsFrame: View
    private lateinit var mProgressFrame: View
    private lateinit var mClickFrame: View
    private lateinit var mToolbarFrame: View

    private lateinit var mToolbar: Toolbar
    private lateinit var mSubView: CaptionsView
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mPositionTextView: TextView
    private lateinit var viewForward: TextView
    private lateinit var viewBackward: TextView
    private lateinit var mTextureView: TextureView
    private lateinit var mSeeker: SeekBar
    private lateinit var mBottomProgressBar: ProgressBar
    private lateinit var mLabelPosition: TextView
    private lateinit var mLabelDuration: TextView
    private lateinit var mBtnPlayPause: ImageButton

    private var mSurfaceAvailable: Boolean = false
    private var mIsPrepared: Boolean = false
    private var mWasPlaying: Boolean = false
    private var mInitialTextureWidth: Int = 0
    private var mInitialTextureHeight: Int = 0
    private var mHandler: Handler? = null

    private var mSource: Uri? = null
    private var headers: Map<String, String> = HashMap()

    private var mCallback: VideoCallback? = null
    private var mProgressCallback: VideoProgressCallback? = null

    private var mPlayDrawable: Drawable? = null
    private var mPauseDrawable: Drawable? = null
    private var mRestartDrawable: Drawable? = null

    private var mTitle: String? = null
    private var mLoop = false
    private var mHideControlsOnPlay = false
    private var mShowTotalDuration = true
    private var mBottomProgressBarVisibility = false
    private var mShowToolbar = true
    private var mGestureType = GestureType.NoGesture
    private var mAutoPlay = false
    private var mControlsDisabled = false
    private var mInitialPosition = -1
    private var mDoubleTapSeekDuration: Int = 0

    private var hideControlsRunnable: Runnable = Runnable { hideControls() }

    private var clickFrameSwipeListener = object : OnSwipeTouchListener(true) {

        var diffTime = -1f
        var finalTime = -1f
        var startVolume: Int = 0
        var maxVolume: Int = 0
        var startBrightness: Int = 0
        var maxBrightness: Int = 0

        override fun onMove(dir: OnSwipeTouchListener.Direction, diff: Float) {
            // If swipe is not enabled, move should not be evaluated.
            if (mGestureType != GestureType.SwipeGesture)
                return

            if (dir == OnSwipeTouchListener.Direction.LEFT || dir == OnSwipeTouchListener.Direction.RIGHT) {

                mPlayer?.let { player ->

                    diffTime = if (player.duration <= 60) {
                        player.duration.toFloat() * diff / mInitialTextureWidth.toFloat()
                    } else {
                        60000.toFloat() * diff / mInitialTextureWidth.toFloat()
                    }
                    if (dir == OnSwipeTouchListener.Direction.LEFT) {
                        diffTime *= -1f
                    }
                    finalTime = player.currentPosition + diffTime
                    if (finalTime < 0) {
                        finalTime = 0f
                    } else if (finalTime > player.duration) {
                        finalTime = player.duration.toFloat()
                    }
                    diffTime = finalTime - player.currentPosition

                    val progressText = Util.getDurationString(finalTime.toLong(), false) +
                            " [" + (if (dir == OnSwipeTouchListener.Direction.LEFT) "-" else "+") +
                            Util.getDurationString(Math.abs(diffTime).toLong(), false) +
                            "]"
                    mPositionTextView.text = progressText

                }

            } else {

                finalTime = -1f
                if (initialX >= mInitialTextureWidth / 2 || mWindow == null) {

                    var diffVolume: Float
                    var finalVolume: Int

                    diffVolume = maxVolume.toFloat() * diff / (mInitialTextureHeight.toFloat() / 2)
                    if (dir == OnSwipeTouchListener.Direction.DOWN) {
                        diffVolume = -diffVolume
                    }
                    finalVolume = startVolume + diffVolume.toInt()
                    if (finalVolume < 0)
                        finalVolume = 0
                    else if (finalVolume > maxVolume)
                        finalVolume = maxVolume

                    val progressText = String.format(
                            resources.getString(R.string.volume), finalVolume
                    )
                    mPositionTextView.text = progressText
                    am?.setStreamVolume(AudioManager.STREAM_MUSIC, finalVolume, 0)

                } else if (initialX < mInitialTextureWidth / 2) {

                    var diffBrightness: Float
                    var finalBrightness: Int

                    diffBrightness = maxBrightness.toFloat() * diff / (mInitialTextureHeight.toFloat() / 2)
                    if (dir == OnSwipeTouchListener.Direction.DOWN) {
                        diffBrightness = -diffBrightness
                    }
                    finalBrightness = startBrightness + diffBrightness.toInt()
                    if (finalBrightness < 0)
                        finalBrightness = 0
                    else if (finalBrightness > maxBrightness)
                        finalBrightness = maxBrightness

                    val progressText = String.format(
                            resources.getString(R.string.brightness), finalBrightness
                    )
                    mPositionTextView.text = progressText

                    val layout = mWindow?.attributes
                    layout?.screenBrightness = finalBrightness.toFloat() / 100
                    mWindow?.attributes = layout

                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putInt(BETTER_VIDEO_PLAYER_BRIGHTNESS, finalBrightness)
                            .apply()
                }
            }
        }

        override fun onClick() {
            toggleControls()
        }

        override fun onDoubleTap(event: MotionEvent) {
            if (mGestureType == GestureType.DoubleTapGesture) {
                val seekSec = mDoubleTapSeekDuration / 1000
                viewForward.text = String.format(resources.getString(R.string.seconds), seekSec)
                viewBackward.text = String.format(resources.getString(R.string.seconds), seekSec)
                if (event.x > mInitialTextureWidth / 2) {
                    viewForward.let {
                        animateViewFade(it, 1)
                        Handler().postDelayed({
                            animateViewFade(it, 0)
                        }, 500)
                    }
                    seekTo(getCurrentPosition() + mDoubleTapSeekDuration)
                } else {
                    viewBackward.let {
                        animateViewFade(it, 1)
                        Handler().postDelayed({
                            animateViewFade(it, 0)
                        }, 500)
                    }
                    seekTo(getCurrentPosition() - mDoubleTapSeekDuration)
                }
            }
        }

        override fun onAfterMove() {
            if (finalTime >= 0 && mGestureType == GestureType.SwipeGesture) {
                seekTo(finalTime.toInt())
                if (mWasPlaying) mPlayer?.start()
            }
            mPositionTextView.visibility = View.GONE
        }

        override fun onBeforeMove(dir: OnSwipeTouchListener.Direction) {
            if (mGestureType != GestureType.SwipeGesture)
                return
            if (dir == OnSwipeTouchListener.Direction.LEFT || dir == OnSwipeTouchListener.Direction.RIGHT) {
                mWasPlaying = isPlaying()
                mPlayer?.pause()
                mPositionTextView.visibility = View.VISIBLE
            } else {
                maxBrightness = 100
                mWindow?.attributes?.let {
                    startBrightness = (it.screenBrightness * 100).toInt()
                }
                maxVolume = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
                startVolume = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 100
                mPositionTextView.visibility = View.VISIBLE
            }
        }
    }

    // Runnable used to run code on an interval to update counters and seeker
    private val mUpdateCounters = object : Runnable {
        override fun run() {

            if (!mIsPrepared)
                return

            mPlayer?.let { player ->
                var pos = player.currentPosition.toLong()
                val dur = player.duration.toLong()
                if (pos > dur) pos = dur
                mLabelPosition.text = Util.getDurationString(pos, false)
                if (mShowTotalDuration) {
                    mLabelDuration.text = Util.getDurationString(dur, false)
                } else {
                    mLabelDuration.text = Util.getDurationString(dur - pos, true)
                }
                val position = pos.toInt()
                val duration = dur.toInt()

                mSeeker.progress = position
                mSeeker.max = duration

                mBottomProgressBar.progress = position
                mBottomProgressBar.max = duration

                mProgressCallback?.onProgressUpdate(position, duration)
                mHandler?.postDelayed(this, UPDATE_INTERVAL.toLong())
            }
        }
    }

    init {
        setBackgroundColor(Color.BLACK)
        attrs?.let {
            val a = context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.BetterVideoPlayer,
                    0, 0)
            try {

                a.getString(R.styleable.BetterVideoPlayer_bvp_source)?.let { source ->
                    if (source.trim().isNotEmpty()) {
                        mSource = Uri.parse(source)
                    }
                }

                a.getString(R.styleable.BetterVideoPlayer_bvp_title)?.let { title ->
                    if (title.trim().isNotEmpty()) {
                        mTitle = title
                    }
                }

                mPlayDrawable = a.getDrawable(
                        R.styleable.BetterVideoPlayer_bvp_playDrawable)
                mPauseDrawable = a.getDrawable(
                        R.styleable.BetterVideoPlayer_bvp_pauseDrawable)
                mRestartDrawable = a.getDrawable(
                        R.styleable.BetterVideoPlayer_bvp_restartDrawable)
                hideControlsDuration = a.getInteger(
                        R.styleable.BetterVideoPlayer_bvp_hideControlsDuration, hideControlsDuration)

                mHideControlsOnPlay = a.getBoolean(
                        R.styleable.BetterVideoPlayer_bvp_hideControlsOnPlay, false)
                mAutoPlay = a.getBoolean(
                        R.styleable.BetterVideoPlayer_bvp_autoPlay, false)
                mLoop = a.getBoolean(
                        R.styleable.BetterVideoPlayer_bvp_loop, false)
                mShowTotalDuration = a.getBoolean(
                        R.styleable.BetterVideoPlayer_bvp_showTotalDuration, false)
                mBottomProgressBarVisibility = a.getBoolean(
                        R.styleable.BetterVideoPlayer_bvp_showBottomProgressBar, false)
                mGestureType = GestureType.values()[
                        a.getInt(
                                R.styleable.BetterVideoPlayer_bvp_gestureType, 0)
                ]
                mShowToolbar = a.getBoolean(
                        R.styleable.BetterVideoPlayer_bvp_showToolbar, true)
                mControlsDisabled = a.getBoolean(
                        R.styleable.BetterVideoPlayer_bvp_disableControls, false)
                mSubViewTextSize = a.getDimensionPixelSize(
                        R.styleable.BetterVideoPlayer_bvp_captionSize,
                        resources.getDimensionPixelSize(R.dimen.bvp_subtitle_size))
                mSubViewTextColor = a.getColor(
                        R.styleable.BetterVideoPlayer_bvp_captionColor,
                        ContextCompat.getColor(context, R.color.bvp_subtitle_color))

            } catch (e: Exception) {
                log("Exception " + e.message)
                e.printStackTrace()
            } finally {
                a.recycle()
            }
        } ?: run {
            mSubViewTextSize = resources.getDimensionPixelSize(R.dimen.bvp_subtitle_size)
            mSubViewTextColor = ContextCompat.getColor(context, R.color.bvp_subtitle_color)
        }

        if (mPlayDrawable == null)
            mPlayDrawable = ContextCompat.getDrawable(context, R.drawable.bvp_action_play)
        if (mPauseDrawable == null)
            mPauseDrawable = ContextCompat.getDrawable(context, R.drawable.bvp_action_pause)
        if (mRestartDrawable == null)
            mRestartDrawable = ContextCompat.getDrawable(context, R.drawable.bvp_action_restart)
    }

    override fun setSource(source: Uri) {
        mSource = source
        if (mPlayer != null) prepare()
    }

    override fun setSource(source: Uri, headers: Map<String, String>) {
        this.headers = headers
        setSource(source)
    }

    override fun setCallback(callback: VideoCallback) {
        mCallback = callback
    }

    override fun setCaptionLoadListener(listener: CaptionsView.CaptionsViewLoadListener?) {
        mSubView.setCaptionsViewLoadListener(listener)
    }

    override fun setProgressCallback(callback: VideoProgressCallback) {
        mProgressCallback = callback
    }

    override fun setButtonDrawable(type: ButtonType, drawable: Drawable) {
        when (type) {
            ButtonType.PlayButton -> {
                mPlayDrawable = drawable
                if (!isPlaying()) {
                    mBtnPlayPause.setImageDrawable(drawable)
                }
            }
            ButtonType.PauseButton -> {
                mPauseDrawable = drawable
                if (isPlaying()) {
                    mBtnPlayPause.setImageDrawable(drawable)
                }
            }
            ButtonType.RestartButton -> {
                mPauseDrawable = drawable
                mPlayer?.let {
                    if (it.currentPosition >= it.duration) {
                        mBtnPlayPause.setImageDrawable(drawable)
                    }
                }
            }
        }
    }

    override fun setHideControlsOnPlay(hide: Boolean) {
        mHideControlsOnPlay = hide
    }

    override fun setAutoPlay(autoPlay: Boolean) {
        mAutoPlay = autoPlay
    }

    override fun enableSwipeGestures() {
        mGestureType = GestureType.SwipeGesture
    }

    override fun enableSwipeGestures(window: Window) {
        mGestureType = GestureType.SwipeGesture
        mWindow = window
    }

    override fun enableDoubleTapGestures(seek: Int) {
        mDoubleTapSeekDuration = seek
        mGestureType = GestureType.DoubleTapGesture
    }

    override fun disableGestures() {
        mGestureType = GestureType.NoGesture
    }

    private fun animateViewFade(view: View, alpha: Int) {
        val viewVisibility = if (alpha > 0) View.VISIBLE else View.INVISIBLE
        view.animate()
                .alpha(alpha.toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = viewVisibility
                    }
                })
    }

    override fun showToolbar() {
        mShowToolbar = true
    }

    override fun hideToolbar() {
        mShowToolbar = false
        hideToolbarWithAnimation()
    }

    override fun setInitialPosition(pos: Int) {
        mInitialPosition = pos
    }

    private fun prepare() {
        if (!mSurfaceAvailable || mIsPrepared)
            return

        mSource?.let { source ->
            try {
                hideControls()
                mCallback?.onPreparing(this)
                mPlayer?.setSurface(mSurface)
                if (source.scheme == "http" || source.scheme == "https") {
                    log("Loading web URI: $source")
                    mPlayer?.setDataSource(context, source, headers)
                } else {
                    log("Loading local URI: $source")
                    mPlayer?.setDataSource(context, source, headers)
                }
                mPlayer?.prepareAsync()
            } catch (e: IOException) {
                throwError(e)
            }
        }

    }

    private fun setControlsEnabled(enabled: Boolean) {
        mSeeker.isEnabled = enabled
        mBtnPlayPause.isEnabled = enabled

        val disabledAlpha = .4f
        mBtnPlayPause.alpha = if (enabled) 1f else disabledAlpha

        mClickFrame.isEnabled = enabled
    }

    override fun showControls() {
        mCallback?.onToggleControls(this, true)

        if (mControlsDisabled || isControlsShown())
            return

        mControlsFrame.animate().cancel()
        mControlsFrame.alpha = 0f
        mControlsFrame.visibility = View.VISIBLE
        mControlsFrame.animate().alpha(1f).translationY(0f).setListener(null)
                .setInterpolator(DecelerateInterpolator()).start()

        val subViewParent = mSubView.parent as View
        subViewParent.animate().cancel()
        subViewParent.translationY = mControlsFrame.height.toFloat()
        subViewParent.animate()
                .translationY(0f)
                .setInterpolator(DecelerateInterpolator())
                .start()

        if (mBottomProgressBarVisibility) {
            mBottomProgressBar.animate().cancel()
            mBottomProgressBar.alpha = 1f
            mBottomProgressBar.animate().alpha(0f).start()
        }

        if (mShowToolbar) {
            mToolbarFrame.animate().cancel()
            mToolbarFrame.alpha = 0f
            mToolbarFrame.visibility = View.VISIBLE
            mToolbarFrame.animate().alpha(1f).setListener(null)
                    .setInterpolator(DecelerateInterpolator()).start()
        }
    }

    override fun hideControls() {
        mCallback?.onToggleControls(this, false)

        if (mControlsDisabled || !isControlsShown())
            return

        mControlsFrame.let { controlsFrame ->
            controlsFrame.animate().cancel()
            controlsFrame.alpha = 1f
            controlsFrame.translationY = 0f
            controlsFrame.visibility = View.VISIBLE
            controlsFrame.animate()
                    .alpha(0f)
                    .translationY(controlsFrame.height.toFloat())
                    .setInterpolator(DecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            mControlsFrame.visibility = View.GONE
                        }
                    }).start()
        }

        val subViewParent = mSubView.parent as View
        subViewParent.animate().cancel()
        subViewParent.animate()
                .translationY(mControlsFrame.height.toFloat())
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        subViewParent.translationY = 0f
                    }
                }).start()

        if (mBottomProgressBarVisibility) {
            mBottomProgressBar.let {
                it.animate().cancel()
                it.alpha = 0f
                it.animate().alpha(1f).start()
            }
        }

        hideToolbarWithAnimation()
    }

    private fun hideToolbarWithAnimation() {
        if (mToolbarFrame.visibility == View.VISIBLE) {
            mToolbarFrame.animate().cancel()
            mToolbarFrame.alpha = 1f
            mToolbarFrame.visibility = View.VISIBLE
            mToolbarFrame.animate().alpha(0f)
                    .setInterpolator(DecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            mToolbarFrame.visibility = View.GONE
                        }
                    }).start()
        }
    }

    @CheckResult
    override fun isControlsShown(): Boolean {
        return !mControlsDisabled && mControlsFrame.alpha > .5f
    }

    override fun toggleControls() {
        if (mControlsDisabled)
            return
        if (isControlsShown()) {
            hideControls()
        } else {
            if (hideControlsDuration >= 0) {
                mHandler?.removeCallbacks(hideControlsRunnable)
                mHandler?.postDelayed(hideControlsRunnable, hideControlsDuration.toLong())
            }
            showControls()
        }
    }

    override fun setBottomProgressBarVisibility(isShowing: Boolean) {
        this.mBottomProgressBarVisibility = isShowing
        if (isShowing) {
            mBottomProgressBar.visibility = View.VISIBLE
        } else {
            mBottomProgressBar.visibility = View.GONE
        }
    }

    override fun enableControls() {
        mControlsDisabled = false
        mClickFrame.isClickable = true
        mClickFrame.setOnTouchListener(clickFrameSwipeListener)
    }

    override fun disableControls() {
        mControlsDisabled = true
        mControlsFrame.visibility = View.GONE
        mToolbarFrame.visibility = View.GONE
        mClickFrame.setOnTouchListener(null)
        mClickFrame.isClickable = false
    }

    override fun isPrepared(): Boolean {
        return mPlayer != null && mIsPrepared
    }

    override fun isPlaying(): Boolean {
        return mPlayer?.isPlaying ?: false
    }

    override fun getCurrentPosition(): Int {
        return mPlayer?.currentPosition ?: -1
    }

    override fun getDuration(): Int {
        return mPlayer?.duration ?: -1
    }

    override fun start() {
        mPlayer?.start()
        mCallback?.onStarted(this)

        if (mHandler == null) mHandler = Handler()

        mHandler?.post(mUpdateCounters)
        mBtnPlayPause.setImageDrawable(mPauseDrawable)
    }

    override fun seekTo(pos: Int) {
        if (Build.VERSION.SDK_INT >= 26) {
            mPlayer?.seekTo(pos.toLong(), MediaPlayer.SEEK_CLOSEST)
        } else {
            mPlayer?.seekTo(pos)
        }
    }

    override fun setVolume(@FloatRange(from = 0.0, to = 1.0) leftVolume: Float,
                           @FloatRange(from = 0.0, to = 1.0) rightVolume: Float) {
        if (mPlayer == null || !mIsPrepared) {
            throw IllegalStateException(
                    "You cannot use setVolume(float, float) until the player is prepared.")
        }
        mPlayer?.setVolume(leftVolume, rightVolume)
    }

    override fun pause() {
        mPlayer?.let { player ->
            if(!isPlaying()) return
            player.pause()
            mCallback?.onPaused(this)
            mHandler?.removeCallbacks(hideControlsRunnable)
            mHandler?.removeCallbacks(mUpdateCounters)

            mBtnPlayPause.setImageDrawable(mPlayDrawable)
        }
    }

    override fun stop() {
        mPlayer?.let { player ->
            try {
                player.stop()
                mHandler?.removeCallbacks(hideControlsRunnable)
                mHandler?.removeCallbacks(mUpdateCounters)
                mBtnPlayPause.setImageDrawable(mPauseDrawable)
            } catch (ignored: Throwable) { }
        }
    }

    override fun reset() {
        mPlayer?.let { player ->
            mIsPrepared = false
            player.reset()
            mIsPrepared = false
        }
    }

    override fun release() {
        mIsPrepared = false

        try { mPlayer?.release() } catch (ignored: Throwable) { }

        mPlayer = null

        mHandler?.removeCallbacks(mUpdateCounters)
        mHandler = null

        log("Released player and Handler")
    }

    override fun setCaptions(source: Uri?, subMime: CaptionsView.SubMime) {
        mSubView.setCaptionsSource(source, subMime)
    }

    override fun setCaptions(@RawRes resId: Int, subMime: CaptionsView.SubMime) {
        mSubView.setCaptionsSource(resId, subMime)
    }

    override fun removeCaptions() {
        setCaptions(null, CaptionsView.SubMime.SUBRIP)
    }

    override fun getToolbar(): Toolbar {
        return mToolbar
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        log("Surface texture available: %dx%d", width, height)
        mInitialTextureWidth = width
        mInitialTextureHeight = height
        mSurfaceAvailable = true
        mSurface = Surface(surfaceTexture)
        if (mIsPrepared) {
            log("Surface texture available and media player is prepared")
            mPlayer?.setSurface(mSurface)
        } else {
            prepare()
        }
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        log("Surface texture changed: %dx%d", width, height)
        mPlayer?.let { adjustAspectRatio(width, height, it.videoWidth, it.videoHeight) }
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        log("Surface texture destroyed")
        mSurfaceAvailable = false
        mSurface = null
        return false
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}

    // Media player listeners

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onPrepared(mediaPlayer: MediaPlayer) {
        log("onPrepared()")
        mProgressBar.visibility = View.INVISIBLE
        showControls()
        mIsPrepared = true

        if (mCallback != null) {
            mCallback?.onPrepared(this)
        }

        mLabelPosition.text = Util.getDurationString(0, false)
        mLabelDuration.text = Util.getDurationString(mediaPlayer.duration.toLong(), false)
        mSeeker.progress = 0
        mSeeker.max = mediaPlayer.duration
        setControlsEnabled(true)

        if (mAutoPlay) {
            if (!mControlsDisabled && mHideControlsOnPlay) {
                mHandler?.postDelayed(hideControlsRunnable, 500)
            }
            start()
            if (mInitialPosition > 0) {
                seekTo(mInitialPosition)
                mInitialPosition = -1
            }
        } else {
            // Hack to show first frame, is there another way?
            mPlayer?.start()
            mPlayer?.pause()
        }
    }

    override fun onBufferingUpdate(mediaPlayer: MediaPlayer, percent: Int) {
        log("Buffering: %d%%", percent)
        mCallback?.onBuffering(percent)

        if (percent == 100) {
            mSeeker.secondaryProgress = 0
            mBottomProgressBar.secondaryProgress = 0
        } else {
            val percentage = percent / 100f
            val secondaryProgress = (mSeeker.max * percentage).toInt()
            mSeeker.secondaryProgress = secondaryProgress
            mBottomProgressBar.secondaryProgress = secondaryProgress
        }
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        log("onCompletion()")
        mBtnPlayPause.setImageDrawable(mRestartDrawable)
        mHandler?.removeCallbacks(mUpdateCounters)

        val currentProgress = mSeeker.max

        mSeeker.progress = currentProgress
        mBottomProgressBar.progress = currentProgress
        if (!mLoop) {
            showControls()
        } else {
            start()
        }

        mCallback?.onCompletion(this)
    }

    override fun onVideoSizeChanged(mediaPlayer: MediaPlayer, width: Int, height: Int) {
        log("Video size changed: %dx%d", width, height)
        adjustAspectRatio(mInitialTextureWidth, mInitialTextureHeight, width, height)

    }

    override fun onError(mediaPlayer: MediaPlayer, what: Int, extra: Int): Boolean {
        if (what == -38) {
            // Error code -38 happens on some Samsung devices
            // Just ignore it
            return false
        }
        var errorMsg = "Preparation/playback error ($what): "
        errorMsg += when (what) {
            MediaPlayer.MEDIA_ERROR_IO -> "I/O error"
            MediaPlayer.MEDIA_ERROR_MALFORMED -> "Malformed"
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> "Not valid for progressive playback"
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "Server died"
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "Timed out"
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "Unsupported"
            else -> "Unknown error"
        }
        throwError(Exception(errorMsg))
        return false
    }

    // View events

    override fun onFinishInflate() {
        super.onFinishInflate()

        keepScreenOn = true

        mHandler = Handler()
        mPlayer = MediaPlayer()
        mPlayer?.setOnPreparedListener(this)
        mPlayer?.setOnBufferingUpdateListener(this)
        mPlayer?.setOnCompletionListener(this)
        mPlayer?.setOnVideoSizeChangedListener(this)
        mPlayer?.setOnErrorListener(this)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPlayer?.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build())
        }
        else {
            mPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }

        am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Instantiate and add TextureView for rendering
        val li = LayoutInflater.from(context)
        val mTextureFrame = li.inflate(R.layout.bvp_include_surface, this, false)
        addView(mTextureFrame)

        mTextureView = mTextureFrame.findViewById(R.id.textureview)
        mTextureView.surfaceTextureListener = this

        viewForward = mTextureFrame.findViewById(R.id.view_forward)
        viewBackward = mTextureFrame.findViewById(R.id.view_backward)

        // Inflate and add progress
        mProgressFrame = li.inflate(R.layout.bvp_include_progress, this, false)
        mProgressBar = mProgressFrame.findViewById(R.id.material_progress_bar)
        mBottomProgressBar = mProgressFrame.findViewById(R.id.progressBarBottom)

        mPositionTextView = mProgressFrame.findViewById(R.id.position_textview)
        mPositionTextView.setShadowLayer(3f, 3f, 3f, Color.BLACK)
        addView(mProgressFrame)

        // Instantiate and add click frame (used to toggle controls)
        mClickFrame = FrameLayout(context)

        (mClickFrame as FrameLayout).foreground = Util.resolveDrawable(context, R.attr.selectableItemBackground)
        addView(mClickFrame, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))

        // Inflate controls
        mControlsFrame = li.inflate(R.layout.bvp_include_controls, this, false)
        val mControlsLp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        mControlsLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        addView(mControlsFrame, mControlsLp)

        // Add topbar
        mToolbarFrame = li.inflate(R.layout.bvp_include_topbar, this, false)
        mToolbar = mToolbarFrame.findViewById(R.id.toolbar)
        mToolbar.title = mTitle
        mToolbarFrame.visibility = if (mShowToolbar) View.VISIBLE else View.GONE
        addView(mToolbarFrame)

        // Inflate subtitles
        val mSubtitlesFrame = li.inflate(R.layout.bvp_include_subtitle, this, false)
        val mSubtitlesLp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        mSubtitlesLp.addRule(RelativeLayout.ABOVE, R.id.bvp_include_relativelayout)
        mSubtitlesLp.alignWithParent = true

        mSubView = mSubtitlesFrame.findViewById(R.id.subs_box)
        mPlayer?.let{ mSubView.setPlayer(it) }

        mSubView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSubViewTextSize.toFloat())
        mSubView.setTextColor(mSubViewTextColor)

        addView(mSubtitlesFrame, mSubtitlesLp)

        // Retrieve controls
        mSeeker = mControlsFrame.findViewById(R.id.seeker)
        mSeeker.setOnSeekBarChangeListener(this)

        mLabelPosition = mControlsFrame.findViewById(R.id.position)
        mLabelPosition.text = Util.getDurationString(0, false)

        mLabelDuration = mControlsFrame.findViewById(R.id.duration)
        mLabelDuration.text = Util.getDurationString(0, true)
        mLabelDuration.setOnClickListener(this)

        mBtnPlayPause = mControlsFrame.findViewById(R.id.btnPlayPause)
        mBtnPlayPause.setOnClickListener(this)
        mBtnPlayPause.setImageDrawable(mPlayDrawable)

        if (mControlsDisabled) {
            disableControls()
        } else {
            enableControls()
        }
        setBottomProgressBarVisibility(mBottomProgressBarVisibility)
        setControlsEnabled(false)
        prepare()
    }

    override fun onClick(view: View) {
        if (view.id == R.id.btnPlayPause) {
            if (mPlayer?.isPlaying == true) {
                pause()
            } else {
                if (mHideControlsOnPlay && !mControlsDisabled) {
                    mHandler?.postDelayed(hideControlsRunnable, 500)
                }
                start()
            }
        } else if (view.id == R.id.duration) {
            mShowTotalDuration = !mShowTotalDuration
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            seekTo(progress)
            mPositionTextView.text = Util.getDurationString(progress.toLong(), false)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        mWasPlaying = isPlaying()
        if (mWasPlaying) mPlayer?.pause() // keeps the time updater running, unlike pause()
        mPositionTextView.visibility = View.VISIBLE
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (mWasPlaying) mPlayer?.start()
        mPositionTextView.visibility = View.GONE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        log("Attached to window")
        if (mPlayer != null) {
            log("mPlayer not null on attach")
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        log("Detached from window")
        release()

        mHandler?.removeCallbacks(mUpdateCounters)
        mHandler = null
    }

    private fun adjustAspectRatio(viewWidth: Int, viewHeight: Int, videoWidth: Int, videoHeight: Int) {
        val aspectRatio = videoHeight.toDouble() / videoWidth
        val newWidth: Int
        val newHeight: Int

        if (viewHeight > (viewWidth * aspectRatio).toInt()) {
            // limited by narrow width; restrict height
            newWidth = viewWidth
            newHeight = (viewWidth * aspectRatio).toInt()
        } else {
            // limited by short height; restrict width
            newWidth = (viewHeight / aspectRatio).toInt()
            newHeight = viewHeight
        }

        val xoff = (viewWidth - newWidth) / 2
        val yoff = (viewHeight - newHeight) / 2

        val txform = Matrix()
        mTextureView.getTransform(txform)
        txform.setScale(newWidth.toFloat() / viewWidth, newHeight.toFloat() / viewHeight)
        txform.postTranslate(xoff.toFloat(), yoff.toFloat())
        mTextureView.setTransform(txform)
    }

    private fun throwError(e: Exception) {
        if (mCallback != null)
            mCallback?.onError(this, e)
        else
            throw RuntimeException(e)
    }

    override fun setLoop(loop: Boolean) {
        this.mLoop = loop
    }

    enum class ButtonType {
        PlayButton, PauseButton, RestartButton
    }

    enum class GestureType {
        NoGesture, SwipeGesture, DoubleTapGesture
    }

    companion object {

        private const val BETTER_VIDEO_PLAYER_BRIGHTNESS = "BETTER_VIDEO_PLAYER_BRIGHTNESS"
        private const val UPDATE_INTERVAL = 100

        // Utilities

        private fun log(message: String, vararg args: Any) {
            Log.d("BetterVideoPlayer", String.format(message, *args))
        }
    }
}
