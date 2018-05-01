package com.halilibo.bettervideoplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.CheckResult;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.github.ybq.android.spinkit.style.ChasingDots;
import com.github.ybq.android.spinkit.style.Circle;
import com.github.ybq.android.spinkit.style.CubeGrid;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.github.ybq.android.spinkit.style.FadingCircle;
import com.github.ybq.android.spinkit.style.Pulse;
import com.github.ybq.android.spinkit.style.RotatingCircle;
import com.github.ybq.android.spinkit.style.RotatingPlane;
import com.github.ybq.android.spinkit.style.ThreeBounce;
import com.github.ybq.android.spinkit.style.WanderingCubes;
import com.github.ybq.android.spinkit.style.Wave;
import com.halilibo.bettervideoplayer.subtitle.CaptionsView;
import com.halilibo.bettervideoplayer.utility.EmptyCallback;
import com.halilibo.bettervideoplayer.utility.Util;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

/**
 * @author Aidan Follestad
 * Modified and improved by Halil Ozercan
 */
public class BetterVideoPlayer extends RelativeLayout implements IUserMethods,
    TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
    MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnErrorListener,
    View.OnClickListener, SeekBar.OnSeekBarChangeListener {

  private static final String BETTER_VIDEO_PLAYER_BRIGHTNESS = "BETTER_VIDEO_PLAYER_BRIGHTNESS";
  private static final int UPDATE_INTERVAL = 100;

  private SpinKitView mProgressBar;
  private TextView mPositionTextView, viewForward, viewBackward;

  private CaptionsView mSubView;
  private AudioManager am;
  private Toolbar mToolbar;
  private String mTitle;
  private int mSubViewTextSize;
  private int mSubViewTextColor;
  private Context context;

  /**
   * Window that hold the player. Necessary for setting brightness.
   */
  private Window mWindow;

  private static final int DOUBLE_BOUNCE = 0;
  private static final int ROTATING_PLANE = 1;
  private static final int WAVE = 2;
  private static final int WANDERING_CUBES = 3;
  private static final int PULSE = 4;
  private static final int CHASING_DOTS = 5;
  private static final int THREE_BOUNCE = 6;
  private static final int CIRCLE = 7;
  private static final int CUBE_GRID = 8;
  private static final int FADING_CIRCLE = 9;
  private static final int ROTATING_CIRCLE = 10;

  @IntDef({DOUBLE_BOUNCE, ROTATING_PLANE, WAVE,
      WANDERING_CUBES, PULSE, CHASING_DOTS,
      THREE_BOUNCE, CIRCLE, CUBE_GRID,
      FADING_CIRCLE, ROTATING_CIRCLE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface LoadingStyle {
  }

  private static final int PLAY_BUTTON = 0;
  private static final int PAUSE_BUTTON = 1;
  private static final int RESTART_BUTTON = 2;

  @IntDef({PLAY_BUTTON, PAUSE_BUTTON, RESTART_BUTTON})
  @Retention(RetentionPolicy.SOURCE)
  public @interface ButtonType {
  }

  private static final int NO_GESTURE = 0;
  private static final int SWIPE_GESTURE = 1;
  private static final int DOUBLETAP_GESTURE = 2;

  @IntDef({NO_GESTURE, SWIPE_GESTURE, DOUBLETAP_GESTURE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface GestureType {
  }

  public BetterVideoPlayer(Context context) {
    super(context);
    init(context, null);
  }

  public BetterVideoPlayer(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public BetterVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private View mControlsFrame;
  private View mProgressFrame;
  private View mClickFrame;
  private View mToolbarFrame;

  private MediaPlayer mPlayer;
  private TextureView mTextureView;
  private Surface mSurface;
  private SeekBar mSeeker;
  private ProgressBar mBottomProgressBar;
  private TextView mLabelPosition;
  private TextView mLabelDuration;
  private ImageButton mBtnPlayPause;

  private boolean mSurfaceAvailable;
  private boolean mIsPrepared;
  private boolean mWasPlaying;
  private int mInitialTextureWidth;
  private int mInitialTextureHeight;
  private Handler mHandler;

  private int viewVisibility;

  private Uri mSource;
  private Map<String, String> headers;

  private BetterVideoCallback mCallback;
  private BetterVideoProgressCallback mProgressCallback;

  private Drawable mPlayDrawable;
  private Drawable mPauseDrawable;
  private Drawable mRestartDrawable;

  private boolean mLoop = false;
  private boolean mHideControlsOnPlay = false;
  private boolean mShowTotalDuration = true;
  private boolean mBottomProgressBarVisibility = false;
  private boolean mShowToolbar = true;
  private int mGestureType = NO_GESTURE;
  private boolean mAutoPlay = false;
  private boolean mControlsDisabled = false;
  private int mLoadingStyle = CHASING_DOTS;
  private int mInitialPosition = -1;
  private int mHideControlsDuration = 2000; // defaults to 2 seconds.
  private int mDoubleTapSeekDuration;


  private void init(Context context, AttributeSet attrs) {
    setBackgroundColor(Color.BLACK);
    this.context = context;
    if (attrs != null) {
      TypedArray a = context.getTheme().obtainStyledAttributes(
          attrs,
          R.styleable.BetterVideoPlayer,
          0, 0);
      try {

        String source = a.getString(R.styleable.BetterVideoPlayer_bvp_source);
        if (source != null && !source.trim().isEmpty())
          mSource = Uri.parse(source);

        String title = a.getString(R.styleable.BetterVideoPlayer_bvp_title);
        if (title != null && !title.trim().isEmpty())
          mTitle = title;

        mPlayDrawable = a.getDrawable(
            R.styleable.BetterVideoPlayer_bvp_playDrawable);
        mPauseDrawable = a.getDrawable(
            R.styleable.BetterVideoPlayer_bvp_pauseDrawable);
        mRestartDrawable = a.getDrawable(
            R.styleable.BetterVideoPlayer_bvp_restartDrawable);
        mLoadingStyle = a.getInt(
            R.styleable.SpinKitView_SpinKit_Style, 0);
        mHideControlsDuration = a.getInteger(
            R.styleable.BetterVideoPlayer_bvp_hideControlsDuration, mHideControlsDuration);

        mHideControlsOnPlay = a.getBoolean(
            R.styleable.BetterVideoPlayer_bvp_hideControlsOnPlay, false);
        mAutoPlay = a.getBoolean(
            R.styleable.BetterVideoPlayer_bvp_autoPlay, false);
        mLoop = a.getBoolean(
            R.styleable.BetterVideoPlayer_bvp_loop, false);
        mShowTotalDuration = a.getBoolean(
            R.styleable.BetterVideoPlayer_bvp_showTotalDuration, false);
        mBottomProgressBarVisibility = a.getBoolean(
            R.styleable.BetterVideoPlayer_bvp_showBottomProgressBar, false);
        mGestureType = a.getInt(
            R.styleable.BetterVideoPlayer_bvp_gestureType, 0);
        mShowToolbar = a.getBoolean(
            R.styleable.BetterVideoPlayer_bvp_showToolbar, true);
        mControlsDisabled = a.getBoolean(
            R.styleable.BetterVideoPlayer_bvp_disableControls, false);
        mSubViewTextSize = a.getDimensionPixelSize(
            R.styleable.BetterVideoPlayer_bvp_captionSize,
            getResources().getDimensionPixelSize(R.dimen.bvp_subtitle_size));
        mSubViewTextColor = a.getColor(
            R.styleable.BetterVideoPlayer_bvp_captionColor,
            ContextCompat.getColor(context, R.color.bvp_subtitle_color));

      } catch (Exception e) {
        LOG("Exception " + e.getMessage());
        e.printStackTrace();
      } finally {
        a.recycle();
      }
    } else {
      mSubViewTextSize = getResources().getDimensionPixelSize(R.dimen.bvp_subtitle_size);
      mSubViewTextColor = ContextCompat.getColor(context, R.color.bvp_subtitle_color);
    }

    if (mPlayDrawable == null)
      mPlayDrawable = ContextCompat.getDrawable(context, R.drawable.bvp_action_play);
    if (mPauseDrawable == null)
      mPauseDrawable = ContextCompat.getDrawable(context, R.drawable.bvp_action_pause);
    if (mRestartDrawable == null)
      mRestartDrawable = ContextCompat.getDrawable(context, R.drawable.bvp_action_restart);

    // Have a default callback. setCallback will change this
    mCallback = new EmptyCallback();
  }

  @Override
  public void setSource(@NonNull Uri source) {
    mSource = source;
    if (mPlayer != null) prepare();
  }

  @Override
  public void setSource(@NonNull Uri source, @NonNull Map<String, String> headers) {
    this.headers = headers;
    setSource(source);
  }

  @Override
  public void setCallback(@NonNull BetterVideoCallback callback) {
    mCallback = callback;
  }

  @Override
  public void setCaptionLoadListener(@Nullable CaptionsView.CaptionsViewLoadListener listener) {
    mSubView.setCaptionsViewLoadListener(listener);
  }

  @Override
  public void setProgressCallback(@NonNull BetterVideoProgressCallback callback) {
    mProgressCallback = callback;
  }

  @Override
  public void setButtonDrawable(@BetterVideoPlayer.ButtonType int type,
                                @NonNull Drawable drawable) {
    switch (type) {
      case PLAY_BUTTON:
        mPlayDrawable = drawable;
        if (!isPlaying()) {
          mBtnPlayPause.setImageDrawable(drawable);
        }
        break;
      case PAUSE_BUTTON:
        mPauseDrawable = drawable;
        if (isPlaying()) {
          mBtnPlayPause.setImageDrawable(drawable);
        }
        break;
      case RESTART_BUTTON:
        mPauseDrawable = drawable;
        if (mPlayer != null && mPlayer.getCurrentPosition() >= mPlayer.getDuration()) {
          mBtnPlayPause.setImageDrawable(drawable);
        }
        break;
    }
  }

  @Override
  public void setHideControlsOnPlay(boolean hide) {
    mHideControlsOnPlay = hide;
  }

  @Override
  public void setAutoPlay(boolean autoPlay) {
    mAutoPlay = autoPlay;
  }

  @Override
  public void enableSwipeGestures() {
    mGestureType = SWIPE_GESTURE;
  }

  @Override
  public void enableSwipeGestures(@NonNull Window window) {
    mGestureType = SWIPE_GESTURE;
    mWindow = window;
  }

  @Override
  public void enableDoubleTapGestures(int seek) {
    mDoubleTapSeekDuration = seek;
    mGestureType = DOUBLETAP_GESTURE;
  }

  @Override
  public void disableGestures() {
    mGestureType = NO_GESTURE;
  }

  private void animateViewFade(final View view, final int alpha) {
    viewVisibility = alpha > 0 ? View.VISIBLE : View.INVISIBLE;
    view.animate()
        .alpha(alpha)
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            view.setVisibility(viewVisibility);
          }
        });
  }

  @Override
  public void showToolbar() {
    mShowToolbar = true;
  }

  @Override
  public void hideToolbar() {
    mShowToolbar = false;
    hideToolbarWithAnimation();
  }

  @Override
  public void setInitialPosition(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
    mInitialPosition = pos;
  }

  private void prepare() {
    if (!mSurfaceAvailable || mSource == null || mPlayer == null || mIsPrepared)
      return;
    try {
      hideControls();
      mCallback.onPreparing(this);
      mPlayer.setSurface(mSurface);
      if (mSource.getScheme().equals("http") || mSource.getScheme().equals("https")) {
        LOG("Loading web URI: " + mSource.toString());
        mPlayer.setDataSource(getContext(), mSource, headers);
      } else {
        LOG("Loading local URI: " + mSource.toString());
        mPlayer.setDataSource(getContext(), mSource, headers);
      }
      mPlayer.prepareAsync();
    } catch (IOException e) {
      throwError(e);
    }
  }

  private void setControlsEnabled(boolean enabled) {
    if (mSeeker == null)
      return;

    mSeeker.setEnabled(enabled);
    mBtnPlayPause.setEnabled(enabled);

    final float disabledAlpha = .4f;
    mBtnPlayPause.setAlpha(enabled ? 1f : disabledAlpha);

    mClickFrame.setEnabled(enabled);
  }

  @Override
  public void showControls() {
    mCallback.onToggleControls(this, true);
    if (mControlsDisabled || isControlsShown() || mSeeker == null)
      return;
    mControlsFrame.animate().cancel();
    mControlsFrame.setAlpha(0f);
    mControlsFrame.setVisibility(View.VISIBLE);
    mControlsFrame.animate().alpha(1f).translationY(0).setListener(null)
        .setInterpolator(new DecelerateInterpolator()).start();

    final View subViewParent = (View) mSubView.getParent();
    subViewParent.animate().cancel();
    subViewParent.setTranslationY(mControlsFrame.getHeight());
    subViewParent.animate()
        .translationY(0)
        .setInterpolator(new DecelerateInterpolator())
        .start();

    if (mBottomProgressBarVisibility) {
      mBottomProgressBar.animate().cancel();
      mBottomProgressBar.setAlpha(1f);
      mBottomProgressBar.animate().alpha(0f).start();
    }

    if (mShowToolbar) {
      mToolbarFrame.animate().cancel();
      mToolbarFrame.setAlpha(0f);
      mToolbarFrame.setVisibility(View.VISIBLE);
      mToolbarFrame.animate().alpha(1f).setListener(null)
          .setInterpolator(new DecelerateInterpolator()).start();
    }
  }

  @Override
  public void hideControls() {
    mCallback.onToggleControls(this, false);
    if (mControlsDisabled || !isControlsShown() || mSeeker == null)
      return;
    mControlsFrame.animate().cancel();
    mControlsFrame.setAlpha(1f);
    mControlsFrame.setTranslationY(0f);
    mControlsFrame.setVisibility(View.VISIBLE);
    mControlsFrame.animate()
        .alpha(0f)
        .translationY(mControlsFrame.getHeight())
        .setInterpolator(new DecelerateInterpolator())
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            if (mControlsFrame != null)
              mControlsFrame.setVisibility(View.GONE);
          }
        }).start();

    final View subViewParent = (View) mSubView.getParent();
    subViewParent.animate().cancel();
    subViewParent.animate()
        .translationY(mControlsFrame.getHeight())
        .setInterpolator(new DecelerateInterpolator())
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            subViewParent.setTranslationY(0);
          }
        }).start();

    if (mBottomProgressBarVisibility) {
      mBottomProgressBar.animate().cancel();
      mBottomProgressBar.setAlpha(0f);
      mBottomProgressBar.animate().alpha(1f).start();
    }

    hideToolbarWithAnimation();
  }

  private void hideToolbarWithAnimation() {
    if (mToolbarFrame.getVisibility() == VISIBLE) {
      mToolbarFrame.animate().cancel();
      mToolbarFrame.setAlpha(1f);
      mToolbarFrame.setVisibility(View.VISIBLE);
      mToolbarFrame.animate().alpha(0f)
          .setInterpolator(new DecelerateInterpolator())
          .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              if (mToolbarFrame != null)
                mToolbarFrame.setVisibility(View.GONE);
            }
          }).start();
    }
  }

  @CheckResult
  @Override
  public boolean isControlsShown() {
    return !mControlsDisabled && mControlsFrame != null && mControlsFrame.getAlpha() > .5f;
  }

  @Override
  public void toggleControls() {
    if (mControlsDisabled)
      return;
    if (isControlsShown()) {
      hideControls();
    } else {
      if (mHideControlsDuration >= 0) {
        mHandler.removeCallbacks(hideControlsRunnable);
        mHandler.postDelayed(hideControlsRunnable, mHideControlsDuration);
      }
      showControls();
    }
  }

  @Override
  public void setBottomProgressBarVisibility(boolean isShowing) {
    this.mBottomProgressBarVisibility = isShowing;
    if (isShowing) {
      mBottomProgressBar.setVisibility(View.VISIBLE);
    } else {
      mBottomProgressBar.setVisibility(View.GONE);
    }
  }

  @Override
  public void setHideControlsDuration(int hideControlsDuration) {
    this.mHideControlsDuration = hideControlsDuration;
  }

  @Override
  public int getHideControlsDuration() {
    return mHideControlsDuration;
  }

  @Override
  public void enableControls() {
    mControlsDisabled = false;
    mClickFrame.setClickable(true);
    mClickFrame.setOnTouchListener(clickFrameSwipeListener);
  }

  @Override
  public void disableControls() {
    mControlsDisabled = true;
    mControlsFrame.setVisibility(View.GONE);
    mToolbarFrame.setVisibility(View.GONE);
    mClickFrame.setOnTouchListener(null);
    mClickFrame.setClickable(false);
  }

  @Override
  public boolean isPrepared() {
    return mPlayer != null && mIsPrepared;
  }

  @Override
  public boolean isPlaying() {
    return mPlayer != null && mPlayer.isPlaying();
  }

  @Override
  public int getCurrentPosition() {
    if (mPlayer == null) return -1;
    return mPlayer.getCurrentPosition();
  }

  @Override
  public int getDuration() {
    if (mPlayer == null) return -1;
    return mPlayer.getDuration();
  }

  @Override
  public void start() {
    if (mPlayer == null) return;
    mPlayer.start();
    mCallback.onStarted(this);
    if (mHandler == null) mHandler = new Handler();
    mHandler.post(mUpdateCounters);
    mBtnPlayPause.setImageDrawable(mPauseDrawable);
  }

  @Override
  public void seekTo(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
    if (mPlayer == null) {
      return;
    }
    if(Build.VERSION.SDK_INT >= 26) {
      mPlayer.seekTo(pos, MediaPlayer.SEEK_CLOSEST);
    }
    else {
      mPlayer.seekTo(pos);
    }
  }

  public void setVolume(@FloatRange(from = 0f, to = 1f) float leftVolume,
                        @FloatRange(from = 0f, to = 1f) float rightVolume) {
    if (mPlayer == null || !mIsPrepared) {
      throw new IllegalStateException(
          "You cannot use setVolume(float, float) until the player is prepared.");
    }
    mPlayer.setVolume(leftVolume, rightVolume);
  }

  @Override
  public void pause() {
    if (mPlayer == null || !isPlaying()) {
      return;
    }
    mPlayer.pause();
    mCallback.onPaused(this);
    if (mHandler == null) {
      return;
    }
    mHandler.removeCallbacks(hideControlsRunnable);
    mHandler.removeCallbacks(mUpdateCounters);
    mBtnPlayPause.setImageDrawable(mPlayDrawable);
  }

  @Override
  public void stop() {
    if (mPlayer == null) return;
    try {
      mPlayer.stop();
    } catch (Throwable ignored) {
    }
    if (mHandler == null) return;
    mHandler.removeCallbacks(hideControlsRunnable);
    mHandler.removeCallbacks(mUpdateCounters);
    mBtnPlayPause.setImageDrawable(mPauseDrawable);
  }

  @Override
  public void reset() {
    if (mPlayer == null) return;
    mIsPrepared = false;
    mPlayer.reset();
    mIsPrepared = false;
  }

  @Override
  public void release() {
    mIsPrepared = false;

    if (mPlayer != null) {
      try {
        mPlayer.release();
      } catch (Throwable ignored) {
      }
      mPlayer = null;
    }

    if (mHandler != null) {
      mHandler.removeCallbacks(mUpdateCounters);
      mHandler = null;
    }

    LOG("Released player and Handler");
  }

  @Override
  public void setCaptions(Uri source, CaptionsView.CMime cMime) {
    mSubView.setCaptionsSource(source, cMime);
  }

  @Override
  public void setCaptions(@RawRes int resId, CaptionsView.CMime cMime) {
    mSubView.setCaptionsSource(resId, cMime);
  }

  @Override
  public void removeCaptions() {
    setCaptions(null, null);
  }

  @Override
  public Toolbar getToolbar() {
    return mToolbar;
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
    LOG("Surface texture available: %dx%d", width, height);
    mInitialTextureWidth = width;
    mInitialTextureHeight = height;
    mSurfaceAvailable = true;
    mSurface = new Surface(surfaceTexture);
    if (mIsPrepared) {
      LOG("Surface texture available and media player is prepared");
      mPlayer.setSurface(mSurface);
    } else {
      prepare();
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    LOG("Surface texture changed: %dx%d", width, height);
    adjustAspectRatio(width, height, mPlayer.getVideoWidth(), mPlayer.getVideoHeight());
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
    LOG("Surface texture destroyed");
    mSurfaceAvailable = false;
    mSurface = null;
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
  }

  // Media player listeners

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @Override
  public void onPrepared(MediaPlayer mediaPlayer) {
    LOG("onPrepared()");
    mProgressBar.setVisibility(View.INVISIBLE);
    showControls();
    mIsPrepared = true;

    if (mCallback != null) {
      mCallback.onPrepared(this);
    }

    mLabelPosition.setText(Util.getDurationString(0, false));
    mLabelDuration.setText(Util.getDurationString(mediaPlayer.getDuration(), false));
    mSeeker.setProgress(0);
    mSeeker.setMax(mediaPlayer.getDuration());
    setControlsEnabled(true);

    if (mAutoPlay) {
      if (!mControlsDisabled && mHideControlsOnPlay) {
        mHandler.postDelayed(hideControlsRunnable, 500);
      }
      start();
      if (mInitialPosition > 0) {
        seekTo(mInitialPosition);
        mInitialPosition = -1;
      }
    } else {
      // Hack to show first frame, is there another way?
      mPlayer.start();
      mPlayer.pause();
    }
  }

  @Override
  public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
    LOG("Buffering: %d%%", percent);
    if (mCallback != null) {
      mCallback.onBuffering(percent);
    }
    if (mSeeker != null) {
      if (percent == 100) {
        mSeeker.setSecondaryProgress(0);
        mBottomProgressBar.setSecondaryProgress(0);
      } else {
        float percentage = percent / 100f;
        int secondaryProgress = (int) (mSeeker.getMax() * percentage);
        mSeeker.setSecondaryProgress(secondaryProgress);
        mBottomProgressBar.setSecondaryProgress(secondaryProgress);
      }
    }
  }

  @Override
  public void onCompletion(MediaPlayer mediaPlayer) {
    LOG("onCompletion()");
    mBtnPlayPause.setImageDrawable(mRestartDrawable);
    if (mHandler != null)
      mHandler.removeCallbacks(mUpdateCounters);
    int currentProgress = mSeeker.getMax();
    mSeeker.setProgress(currentProgress);
    mBottomProgressBar.setProgress(currentProgress);
    if (!mLoop) {
      showControls();
    } else {
      start();
    }
    if (mCallback != null)
      mCallback.onCompletion(this);
  }

  @Override
  public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
    LOG("Video size changed: %dx%d", width, height);
    adjustAspectRatio(mInitialTextureWidth, mInitialTextureHeight, width, height);

  }

  @Override
  public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
    if (what == -38) {
      // Error code -38 happens on some Samsung devices
      // Just ignore it
      return false;
    }
    String errorMsg = "Preparation/playback error (" + what + "): ";
    switch (what) {
      default:
        errorMsg += "Unknown error";
        break;
      case MediaPlayer.MEDIA_ERROR_IO:
        errorMsg += "I/O error";
        break;
      case MediaPlayer.MEDIA_ERROR_MALFORMED:
        errorMsg += "Malformed";
        break;
      case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
        errorMsg += "Not valid for progressive playback";
        break;
      case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
        errorMsg += "Server died";
        break;
      case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
        errorMsg += "Timed out";
        break;
      case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
        errorMsg += "Unsupported";
        break;
    }
    throwError(new Exception(errorMsg));
    return false;
  }

  // View events

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    setKeepScreenOn(true);

    mHandler = new Handler();
    mPlayer = new MediaPlayer();
    mPlayer.setOnPreparedListener(this);
    mPlayer.setOnBufferingUpdateListener(this);
    mPlayer.setOnCompletionListener(this);
    mPlayer.setOnVideoSizeChangedListener(this);
    mPlayer.setOnErrorListener(this);
    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

    am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

    // Instantiate and add TextureView for rendering
    final LayoutInflater li = LayoutInflater.from(getContext());
    View mTextureFrame = li.inflate(R.layout.bvp_include_surface, this, false);
    addView(mTextureFrame);

    mTextureView = mTextureFrame.findViewById(R.id.textureview);
    mTextureView.setSurfaceTextureListener(this);

    viewForward = mTextureFrame.findViewById(R.id.view_forward);
    viewBackward = mTextureFrame.findViewById(R.id.view_backward);

    // Inflate and add progress
    mProgressFrame = li.inflate(R.layout.bvp_include_progress, this, false);
    mProgressBar = mProgressFrame.findViewById(R.id.spin_kit);
    mBottomProgressBar = mProgressFrame.findViewById(R.id.progressBarBottom);

    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = getContext().getTheme();
    theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
    int color = typedValue.data;
    mProgressBar.setColor(color);
    setLoadingStyle(mLoadingStyle);

    mPositionTextView = mProgressFrame.findViewById(R.id.position_textview);
    mPositionTextView.setShadowLayer(3, 3, 3, Color.BLACK);
    addView(mProgressFrame);

    // Instantiate and add click frame (used to toggle controls)
    mClickFrame = new FrameLayout(getContext());
    //noinspection RedundantCast
    ((FrameLayout) mClickFrame).setForeground(
        Util.resolveDrawable(getContext(), R.attr.selectableItemBackground));
    addView(mClickFrame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT));

    // Inflate controls
    mControlsFrame = li.inflate(R.layout.bvp_include_controls, this, false);
    LayoutParams mControlsLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    mControlsLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
    addView(mControlsFrame, mControlsLp);

    // Add topbar
    mToolbarFrame = li.inflate(R.layout.bvp_include_topbar, this, false);
    mToolbar = mToolbarFrame.findViewById(R.id.toolbar);
    mToolbar.setTitle(mTitle);
    mToolbarFrame.setVisibility(mShowToolbar ? VISIBLE : GONE);
    addView(mToolbarFrame);

    // Inflate subtitles
    View mSubtitlesFrame = li.inflate(R.layout.bvp_include_subtitle, this, false);
    LayoutParams mSubtitlesLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    mSubtitlesLp.addRule(RelativeLayout.ABOVE, R.id.bvp_include_relativelayout);
    mSubtitlesLp.alignWithParent = true;

    mSubView = mSubtitlesFrame.findViewById(R.id.subs_box);
    mSubView.setPlayer(mPlayer);

    mSubView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSubViewTextSize);
    mSubView.setTextColor(mSubViewTextColor);

    addView(mSubtitlesFrame, mSubtitlesLp);

    // Retrieve controls
    mSeeker = mControlsFrame.findViewById(R.id.seeker);
    mSeeker.setOnSeekBarChangeListener(this);

    mLabelPosition = mControlsFrame.findViewById(R.id.position);
    mLabelPosition.setText(Util.getDurationString(0, false));

    mLabelDuration = mControlsFrame.findViewById(R.id.duration);
    mLabelDuration.setText(Util.getDurationString(0, true));
    mLabelDuration.setOnClickListener(this);

    mBtnPlayPause = mControlsFrame.findViewById(R.id.btnPlayPause);
    mBtnPlayPause.setOnClickListener(this);
    mBtnPlayPause.setImageDrawable(mPlayDrawable);

    if (mControlsDisabled) {
      disableControls();
    } else {
      enableControls();
    }
    setBottomProgressBarVisibility(mBottomProgressBarVisibility);
    setControlsEnabled(false);
    prepare();
  }

  Runnable hideControlsRunnable = new Runnable() {
    @Override
    public void run() {
      hideControls();
    }
  };

  @Override
  public void onClick(View view) {
    if (view.getId() == R.id.btnPlayPause) {
      if (mPlayer.isPlaying()) {
        pause();
      } else {
        if (mHideControlsOnPlay && !mControlsDisabled) {
          mHandler.postDelayed(hideControlsRunnable, 500);
        }
        start();
      }
    } else if (view.getId() == R.id.duration) {
      mShowTotalDuration = !mShowTotalDuration;
    }
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (fromUser) {
      seekTo(progress);
      mPositionTextView.setText(Util.getDurationString(progress, false));
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    mWasPlaying = isPlaying();
    if (mWasPlaying) mPlayer.pause(); // keeps the time updater running, unlike pause()
    mPositionTextView.setVisibility(VISIBLE);
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    if (mWasPlaying) mPlayer.start();
    mPositionTextView.setVisibility(GONE);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    LOG("Attached to window");
    if (mPlayer != null) {
      LOG("mPlayer not null on attach");
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    LOG("Detached from window");
    release();

    mSeeker = null;
    mLabelPosition = null;
    mLabelDuration = null;
    mBtnPlayPause = null;

    mControlsFrame = null;
    mClickFrame = null;
    mProgressFrame = null;

    if (mHandler != null) {
      mHandler.removeCallbacks(mUpdateCounters);
      mHandler = null;
    }
  }

  // Utilities

  private static void LOG(String message, Object... args) {
    if (args != null && args.length > 0) {
      message = String.format(message, args);
    }
    Log.d("BetterVideoPlayer", message);
  }

  private void adjustAspectRatio(int viewWidth, int viewHeight, int videoWidth, int videoHeight) {
    final double aspectRatio = (double) videoHeight / videoWidth;
    int newWidth, newHeight;

    if (viewHeight > (int) (viewWidth * aspectRatio)) {
      // limited by narrow width; restrict height
      newWidth = viewWidth;
      newHeight = (int) (viewWidth * aspectRatio);
    } else {
      // limited by short height; restrict width
      newWidth = (int) (viewHeight / aspectRatio);
      newHeight = viewHeight;
    }

    final int xoff = (viewWidth - newWidth) / 2;
    final int yoff = (viewHeight - newHeight) / 2;

    final Matrix txform = new Matrix();
    mTextureView.getTransform(txform);
    txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
    txform.postTranslate(xoff, yoff);
    mTextureView.setTransform(txform);
  }

  private void throwError(Exception e) {
    if (mCallback != null)
      mCallback.onError(this, e);
    else throw new RuntimeException(e);
  }

  public void setLoop(boolean loop) {
    this.mLoop = loop;
  }

  @Override
  public void setLoadingStyle(@LoadingStyle int style) {
    Drawable drawable;
    switch (style) {
      case DOUBLE_BOUNCE:
        drawable = new DoubleBounce();
        break;
      case ROTATING_PLANE:
        drawable = new RotatingPlane();
        break;
      case WAVE:
        drawable = new Wave();
        break;
      case WANDERING_CUBES:
        drawable = new WanderingCubes();
        break;
      case PULSE:
        drawable = new Pulse();
        break;
      case CHASING_DOTS:
        drawable = new ChasingDots();
        break;
      case THREE_BOUNCE:
        drawable = new ThreeBounce();
        break;
      case CIRCLE:
        drawable = new Circle();
        break;
      case CUBE_GRID:
        drawable = new CubeGrid();
        break;
      case FADING_CIRCLE:
        drawable = new FadingCircle();
        break;
      case ROTATING_CIRCLE:
        drawable = new RotatingCircle();
        break;
      default:
        drawable = new ThreeBounce();
        break;
    }
    mProgressBar.setIndeterminateDrawable(drawable);
  }

  OnSwipeTouchListener clickFrameSwipeListener =
      new OnSwipeTouchListener(true) {

    float diffTime = -1, finalTime = -1;
    int startVolume;
    int maxVolume;
    int startBrightness;
    int maxBrightness;

    @Override
    public void onMove(Direction dir, float diff) {
      // If swipe is not enabled, move should not be evaluated.
      if (mGestureType != SWIPE_GESTURE)
        return;

      if (dir == Direction.LEFT || dir == Direction.RIGHT) {
        if (mPlayer.getDuration() <= 60) {
          diffTime = (float) mPlayer.getDuration() * diff / ((float) mInitialTextureWidth);
        } else {
          diffTime = (float) 60000 * diff / ((float) mInitialTextureWidth);
        }
        if (dir == Direction.LEFT) {
          diffTime *= -1;
        }
        finalTime = mPlayer.getCurrentPosition() + diffTime;
        if (finalTime < 0) {
          finalTime = 0;
        } else if (finalTime > mPlayer.getDuration()) {
          finalTime = mPlayer.getDuration();
        }
        diffTime = finalTime - mPlayer.getCurrentPosition();

        String progressText =
            Util.getDurationString((long) finalTime, false) +
                " [" + (dir == Direction.LEFT ? "-" : "+") +
                Util.getDurationString((long) Math.abs(diffTime), false) +
                "]";
        mPositionTextView.setText(progressText);
      } else {
        finalTime = -1;
        if (initialX >= mInitialTextureWidth / 2 || mWindow == null) {
          float diffVolume;
          int finalVolume;

          diffVolume = (float) maxVolume * diff / ((float) mInitialTextureHeight / 2);
          if (dir == Direction.DOWN) {
            diffVolume = -diffVolume;
          }
          finalVolume = startVolume + (int) diffVolume;
          if (finalVolume < 0)
            finalVolume = 0;
          else if (finalVolume > maxVolume)
            finalVolume = maxVolume;

          String progressText = String.format(
              getResources().getString(R.string.volume), finalVolume
          );
          mPositionTextView.setText(progressText);
          am.setStreamVolume(AudioManager.STREAM_MUSIC, finalVolume, 0);
        } else if (initialX < mInitialTextureWidth / 2) {
          float diffBrightness;
          int finalBrightness;

          diffBrightness = (float) maxBrightness * diff / ((float) mInitialTextureHeight / 2);
          if (dir == Direction.DOWN) {
            diffBrightness = -diffBrightness;
          }
          finalBrightness = startBrightness + (int) diffBrightness;
          if (finalBrightness < 0)
            finalBrightness = 0;
          else if (finalBrightness > maxBrightness)
            finalBrightness = maxBrightness;

          String progressText = String.format(
              getResources().getString(R.string.brightness), finalBrightness
          );
          mPositionTextView.setText(progressText);

          WindowManager.LayoutParams layout = mWindow.getAttributes();
          layout.screenBrightness = (float) finalBrightness / 100;
          mWindow.setAttributes(layout);

          PreferenceManager.getDefaultSharedPreferences(getContext())
              .edit()
              .putInt(BETTER_VIDEO_PLAYER_BRIGHTNESS, finalBrightness)
              .apply();
        }
      }
    }

    @Override
    public void onClick() {
      toggleControls();
    }

    @Override
    public void onDoubleTap(MotionEvent e) {
      if (mGestureType == DOUBLETAP_GESTURE) {
        int seekSec = mDoubleTapSeekDuration / 1000;
        viewForward.setText(seekSec + " seconds");
        viewBackward.setText(seekSec + " seconds");
        if (e.getX() > mInitialTextureWidth / 2) {
          animateViewFade(viewForward, 1);
          seekTo(getCurrentPosition() + mDoubleTapSeekDuration);
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              animateViewFade(viewForward, 0);
            }
          }, 500);
        } else {
          animateViewFade(viewBackward, 1);
          seekTo(getCurrentPosition() - mDoubleTapSeekDuration);
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              animateViewFade(viewBackward, 0);
            }
          }, 500);
        }
      }
    }

    @Override
    public void onAfterMove() {
      if (finalTime >= 0 && mGestureType == SWIPE_GESTURE) {
        seekTo((int) finalTime);
        if (mWasPlaying) mPlayer.start();
      }
      mPositionTextView.setVisibility(View.GONE);
    }

    @Override
    public void onBeforeMove(Direction dir) {
      if (mGestureType != SWIPE_GESTURE)
        return;
      if (dir == Direction.LEFT || dir == Direction.RIGHT) {
        mWasPlaying = isPlaying();
        mPlayer.pause();
        mPositionTextView.setVisibility(View.VISIBLE);
      } else {
        maxBrightness = 100;
        if (mWindow != null) {
          startBrightness = (int) (mWindow.getAttributes().screenBrightness * 100);
        }
        maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        startVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        mPositionTextView.setVisibility(View.VISIBLE);
      }
    }
  };

  // Runnable used to run code on an interval to update counters and seeker
  private final Runnable mUpdateCounters = new Runnable() {
    @Override
    public void run() {
      if (mHandler == null || !mIsPrepared || mSeeker == null || mPlayer == null)
        return;
      long pos = mPlayer.getCurrentPosition();
      final long dur = mPlayer.getDuration();
      if (pos > dur) pos = dur;
      mLabelPosition.setText(Util.getDurationString(pos, false));
      if (mShowTotalDuration) {
        mLabelDuration.setText(Util.getDurationString(dur, false));
      } else {
        mLabelDuration.setText(Util.getDurationString(dur - pos, true));
      }
      int position = (int) pos;
      int duration = (int) dur;

      mSeeker.setProgress(position);
      mSeeker.setMax(duration);

      mBottomProgressBar.setProgress(position);
      mBottomProgressBar.setMax(duration);

      if (mProgressCallback != null)
        mProgressCallback.onVideoProgressUpdate(position, duration);
      if (mHandler != null)
        mHandler.postDelayed(this, UPDATE_INTERVAL);
    }
  };
}
