package com.halilibo.bettervideoplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
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
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.halilibo.bettervideoplayer.subtitle.SubtitleView;

import java.io.IOException;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * @author Aidan Follestad (halilibo)
 */
public class BetterVideoPlayer extends RelativeLayout implements IUserMethods,
        TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnErrorListener,
        View.OnClickListener, SeekBar.OnSeekBarChangeListener{

    private View mSubtitlesFrame;
    private MaterialProgressBar mProgressBar;
    private TextView mPositionTextView;
    private LayoutParams mControlsLp;
    private boolean mLoop = false;
    private SubtitleView mSubView;

    private AudioManager am;
    private static final int UPDATE_INTERVAL = 100;
    private LayoutParams mSubtitlesLp;
    private Toolbar mToolbar;
    private int mMenuId;
    private Toolbar.OnMenuItemClickListener menuItemClickListener;
    private String mTitle;

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

    private TextureView mTextureView;
    private Surface mSurface;

    private View mControlsFrame;
    private View mProgressFrame;
    private View mClickFrame;
    private View mTextureFrame;
    private View mTopBarFrame;

    private SeekBar mSeeker;
    private TextView mLabelPosition;
    private TextView mLabelDuration;
    private ImageButton mBtnPlayPause;

    private MediaPlayer mPlayer;
    private boolean mSurfaceAvailable;
    private boolean mIsPrepared;
    private boolean mWasPlaying;
    private int mInitialTextureWidth;
    private int mInitialTextureHeight;

    private Handler mHandler;

    private Uri mSource;
    private BetterVideoCallback mCallback;
    private BetterVideoProgressCallback mProgressCallback;
    private CharSequence mSubmitText;
    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;
    private boolean mHideControlsOnPlay = false;
    private boolean mAutoPlay;
    private int mInitialPosition = -1;
    private boolean mControlsDisabled;
    private int mThemeColor = 0;

    OnSwipeTouchListener clickFrameSwipeListener = new OnSwipeTouchListener(){

        float diffTime = -1, finalTime = -1;
        int startVolume;
        int maxVolume;
        @Override
        public void onMove(Direction dir, float diff) {
            Log.d("ClickFrame", dir + " " + diff);
            if(dir == Direction.LEFT || dir == Direction.RIGHT) {
                diffTime = (float) mPlayer.getDuration() * diff / ((float) mInitialTextureWidth * 10);
                if (dir == Direction.LEFT) {
                    diffTime = -diffTime;
                }
                finalTime = mPlayer.getCurrentPosition() + diffTime;
                if (finalTime < 0)
                    finalTime = 0;
                else if (finalTime > mPlayer.getDuration())
                    finalTime = mPlayer.getDuration();

                String progressText =
                        Util.getDurationString((long) finalTime, false) +
                                " [" + (dir == Direction.LEFT ? "-":"+") +
                                Util.getDurationString((long) diffTime, false) +
                                "]";
                mPositionTextView.setText(progressText);
            }
            else{
                finalTime = -1;
                float diffVolume;
                int finalVolume;

                diffVolume = (float) maxVolume * diff / ((float) mInitialTextureHeight / 2);
                Log.d("VolumeControl", (diff / (float) mInitialTextureHeight) + " " + diff + " " + mInitialTextureHeight);
                if (dir == Direction.DOWN) {
                    diffVolume = -diffVolume;
                }
                finalVolume = startVolume + (int)diffVolume;
                if (finalVolume < 0)
                    finalVolume = 0;
                else if (finalVolume > maxVolume)
                    finalVolume = maxVolume;

                String progressText = String.format(getContext().getString(R.string.volume), finalVolume);
                mPositionTextView.setText(progressText);
                am.setStreamVolume(AudioManager.STREAM_MUSIC, finalVolume, 0);
            }
        }

        @Override
        public void onClick() {
            Log.d("ClickFrame", "Clicked");
            //toggleControls();
            if(mCallback!=null){
                mCallback.onClicked(BetterVideoPlayer.this);
            }
            toggleControls();
        }

        @Override
        public void onAfterMove() {
            if(finalTime >= 0) {
                seekTo((int) finalTime);
                if (mWasPlaying) mPlayer.start();
            }
            mPositionTextView.setVisibility(GONE);
        }

        @Override
        public void onBeforeMove(Direction dir) {
            if(dir == Direction.LEFT || dir == Direction.RIGHT) {
                mWasPlaying = isPlaying();
                if (mWasPlaying)
                    mPlayer.pause(); // keeps the time updater running, unlike pause()
                mPositionTextView.setVisibility(VISIBLE);
            }
            else{
                maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                startVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                mPositionTextView.setVisibility(VISIBLE);
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
            mLabelDuration.setText(Util.getDurationString(dur - pos, true));
            mSeeker.setProgress((int) pos);
            mSeeker.setMax((int) dur);

            if (mProgressCallback != null)
                mProgressCallback.onVideoProgressUpdate((int)pos, (int)dur);
            if (mHandler != null)
                mHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };


    private void init(Context context, AttributeSet attrs) {
        setBackgroundColor(Color.BLACK);

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

                mPlayDrawable = a.getDrawable(R.styleable.BetterVideoPlayer_bvp_playDrawable);
                mPauseDrawable = a.getDrawable(R.styleable.BetterVideoPlayer_bvp_pauseDrawable);

                mHideControlsOnPlay = a.getBoolean(R.styleable.BetterVideoPlayer_bvp_hideControlsOnPlay, false);
                mAutoPlay = a.getBoolean(R.styleable.BetterVideoPlayer_bvp_autoPlay, false);
                mControlsDisabled = a.getBoolean(R.styleable.BetterVideoPlayer_bvp_disableControls, false);

                mThemeColor = a.getColor(R.styleable.BetterVideoPlayer_bvp_themeColor,
                        Util.resolveColor(context, R.attr.colorPrimary));

                mMenuId = a.getResourceId(R.styleable.BetterVideoPlayer_bvp_menu, R.menu.base);

            } finally {
                a.recycle();
            }
        } else {
            mHideControlsOnPlay = false;
            mAutoPlay = false;
            mControlsDisabled = false;
            mThemeColor = Util.resolveColor(context, R.attr.colorPrimary);
            mMenuId = R.menu.base;
        }

        if (mPlayDrawable == null)
            mPlayDrawable = ContextCompat.getDrawable(context, R.drawable.bvp_action_play);
        if (mPauseDrawable == null)
            mPauseDrawable = ContextCompat.getDrawable(context, R.drawable.bvp_action_pause);

        // Have a default callback. setCallback will change this
        mCallback = new BetterVideoCallback() {
            @Override
            public void onStarted(BetterVideoPlayer player) {

            }

            @Override
            public void onPaused(BetterVideoPlayer player) {

            }

            @Override
            public void onPreparing(BetterVideoPlayer player) {

            }

            @Override
            public void onPrepared(BetterVideoPlayer player) {

            }

            @Override
            public void onBuffering(int percent) {

            }

            @Override
            public void onError(BetterVideoPlayer player, Exception e) {

            }

            @Override
            public void onCompletion(BetterVideoPlayer player) {

            }

            @Override
            public void onLeftButton(BetterVideoPlayer player) {

            }

            @Override
            public void onRightButton(BetterVideoPlayer player) {

            }

            @Override
            public void onSeekbarPositionChanged(ProgressAction action, int progress, boolean byUser) {

            }

            @Override
            public void onClicked(BetterVideoPlayer player) {

            }
        };

    }

    @Override
    public void setSource(@NonNull Uri source) {
        mSource = source;
        if (mPlayer != null) prepare();
    }

    @Override
    public void setCallback(@NonNull BetterVideoCallback callback) {
        mCallback = callback;
    }

    @Override
    public void setProgressCallback(@NonNull BetterVideoProgressCallback callback) {
        mProgressCallback = callback;
    }

    @Override
    public void setTitle(@NonNull String text) {
        mTitle = text;
        mToolbar.setTitle(mTitle);
    }

    @Override
    public void setTitle(@StringRes int resId) {
        mTitle = getResources().getString(resId);
        mToolbar.setTitle(mTitle);
    }

    @Override
    public void setMenu(@MenuRes int resId) {
        mMenuId = resId;
        mToolbar.inflateMenu(mMenuId);
    }

    @Override
    public void setMenuCallback(@NonNull Toolbar.OnMenuItemClickListener callback) {
        menuItemClickListener = callback;
        mToolbar.setOnMenuItemClickListener(menuItemClickListener);
    }

    @Override
    public void setPlayDrawable(@NonNull Drawable drawable) {
        mPlayDrawable = drawable;
        if (!isPlaying()) mBtnPlayPause.setImageDrawable(drawable);
    }

    @Override
    public void setPlayDrawable(@DrawableRes int res) {
        setPlayDrawable(ContextCompat.getDrawable(getContext(), res));
    }

    @Override
    public void setPauseDrawable(@NonNull Drawable drawable) {
        mPauseDrawable = drawable;
        if (isPlaying()) mBtnPlayPause.setImageDrawable(drawable);
    }

    @Override
    public void setPauseDrawable(@DrawableRes int res) {
        setPauseDrawable(ContextCompat.getDrawable(getContext(), res));
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
    public void setInitialPosition(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
        mInitialPosition = pos;
    }

    private void prepare() {
        if (!mSurfaceAvailable || mSource == null || mPlayer == null || mIsPrepared)
            return;
        try {
            mCallback.onPreparing(this);
            mPlayer.setSurface(mSurface);
            if (mSource.getScheme().equals("http") || mSource.getScheme().equals("https")) {
                LOG("Loading web URI: " + mSource.toString());
                mPlayer.setDataSource(mSource.toString());
            } else {
                LOG("Loading local URI: " + mSource.toString());
                mPlayer.setDataSource(getContext(), mSource);
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
        if (mControlsDisabled || isControlsShown() || mSeeker == null)
            return;
        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(0f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mControlsFrame.animate().alpha(1f).setListener(null)
                .setInterpolator(new DecelerateInterpolator()).start();

        mTopBarFrame.animate().cancel();
        mTopBarFrame.setAlpha(0f);
        mTopBarFrame.setVisibility(View.VISIBLE);
        mTopBarFrame.animate().alpha(1f).setListener(null)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    @Override
    public void hideControls() {
        if (mControlsDisabled || !isControlsShown() || mSeeker == null)
            return;
        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(1f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mControlsFrame.animate().alpha(0f)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mControlsFrame != null)
                            mControlsFrame.setVisibility(View.GONE);
                    }
                }).start();

        mTopBarFrame.animate().cancel();
        mTopBarFrame.setAlpha(1f);
        mTopBarFrame.setVisibility(View.VISIBLE);
        mTopBarFrame.animate().alpha(0f)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mTopBarFrame != null)
                            mTopBarFrame.setVisibility(View.GONE);
                    }
                }).start();
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
            showControls();
        }
    }

    @Override
    public void enableControls(boolean andShow) {
        mControlsDisabled = false;
        if (andShow)
            showControls();
        mClickFrame.setClickable(true);
        mClickFrame.setOnTouchListener(clickFrameSwipeListener);
    }

    @Override
    public void disableControls() {
        mControlsDisabled = true;
        mControlsFrame.setVisibility(View.GONE);
        mClickFrame.setOnTouchListener(null);
        mClickFrame.setClickable(false);
    }

    @CheckResult
    @Override
    public boolean isPrepared() {
        return mPlayer != null && mIsPrepared;
    }

    @CheckResult
    @Override
    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    @CheckResult
    @Override
    public int getCurrentPosition() {
        if (mPlayer == null) return -1;
        return mPlayer.getCurrentPosition();
    }

    @CheckResult
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
        if (mPlayer == null) return;
        mPlayer.seekTo(pos);
    }

    public void setVolume(@FloatRange(from = 0f, to = 1f) float leftVolume, @FloatRange(from = 0f, to = 1f) float rightVolume) {
        if (mPlayer == null || !mIsPrepared)
            throw new IllegalStateException("You cannot use setVolume(float, float) until the player is prepared.");
        mPlayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void pause() {
        if (mPlayer == null || !isPlaying()) return;
        mPlayer.pause();
        mCallback.onPaused(this);
        if (mHandler == null) return;
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
    public void setSubtitle(Uri source) {
        mSubView.setSubSource(source, SubtitleView.SubtitleMime.SUBRIP);
    }

    @Override
    public void setSubtitle(@RawRes int resId) {
        mSubView.setSubSource(resId, SubtitleView.SubtitleMime.SUBRIP);
    }

    @Override
    public void removeSubtitle(){
        setSubtitle(null);
    }

    // Surface listeners

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        LOG("Surface texture available: %dx%d", width, height);
        mInitialTextureWidth = width;
        mInitialTextureHeight = height;
        mSurfaceAvailable = true;
        mSurface = new Surface(surfaceTexture);
        if (mIsPrepared) {
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
        mIsPrepared = true;
        if (mCallback != null)
            mCallback.onPrepared(this);
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
        if (mCallback != null)
            mCallback.onBuffering(percent);
        if (mSeeker != null) {
            if (percent == 100) mSeeker.setSecondaryProgress(0);
            else mSeeker.setSecondaryProgress(mSeeker.getMax() * (percent / 100));
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        LOG("onCompletion()");
        mBtnPlayPause.setImageDrawable(mPlayDrawable);
        if (mHandler != null)
            mHandler.removeCallbacks(mUpdateCounters);
        mSeeker.setProgress(mSeeker.getMax());
        if(!mLoop) {
            showControls();
        }
        else{
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
        mTextureFrame = li.inflate(R.layout.bvp_include_surface, this, false);
        addView(mTextureFrame);

        mTextureView = (TextureView) mTextureFrame.findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener(this);

        // Inflate and add progress
        mProgressFrame = li.inflate(R.layout.bvp_include_progress, this, false);
        mProgressBar = (MaterialProgressBar) mProgressFrame.findViewById(R.id.material_progressbar);
        mPositionTextView = (TextView)mProgressFrame.findViewById(R.id.position_textview);
        mPositionTextView.setShadowLayer(3,3,3,Color.BLACK);
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
        mControlsLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
        mControlsLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        addView(mControlsFrame, mControlsLp);

        if (mControlsDisabled) {
            mClickFrame.setOnTouchListener(null);
            mControlsFrame.setVisibility(View.GONE);
        } else {
            mClickFrame.setOnTouchListener(clickFrameSwipeListener);
        }

        // Add topbar
        mTopBarFrame = li.inflate(R.layout.bvp_include_topbar, this, false);
        mToolbar = (Toolbar) mTopBarFrame.findViewById(R.id.toolbar);
        mToolbar.setTitle(mTitle);
        mToolbar.inflateMenu(mMenuId);
        if(menuItemClickListener != null) {
            mToolbar.setOnMenuItemClickListener(menuItemClickListener);
        }
        addView(mTopBarFrame);

        // Inflate subtitles
        mSubtitlesFrame = li.inflate(R.layout.bvp_include_subtitle, this, false);
        mSubtitlesLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        mSubtitlesLp.addRule(RelativeLayout.ABOVE, R.id.bvp_include_relativelayout);
        mSubtitlesLp.alignWithParent = true;

        mSubView = (SubtitleView) mSubtitlesFrame.findViewById(R.id.subs_box);
        mSubView.setPlayer(mPlayer);

        addView(mSubtitlesFrame, mSubtitlesLp);

        // Retrieve controls
        mSeeker = (SeekBar) mControlsFrame.findViewById(R.id.seeker);
        mSeeker.setOnSeekBarChangeListener(this);

        mLabelPosition = (TextView) mControlsFrame.findViewById(R.id.position);
        mLabelPosition.setText(Util.getDurationString(0, false));

        mLabelDuration = (TextView) mControlsFrame.findViewById(R.id.duration);
        mLabelDuration.setText(Util.getDurationString(0, true));

        mBtnPlayPause = (ImageButton) mControlsFrame.findViewById(R.id.btnPlayPause);
        mBtnPlayPause.setOnClickListener(this);
        mBtnPlayPause.setImageDrawable(mPlayDrawable);

        setControlsEnabled(false);
        invalidateActions();
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
            }
            else {
                if (mHideControlsOnPlay && !mControlsDisabled) {
                    mHandler.postDelayed(hideControlsRunnable, 500);
                }
                start();
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (b) {
            seekTo(i);
            mPositionTextView.setText(Util.getDurationString(i, false));
        }
        if(mCallback != null){
            mCallback.onSeekbarPositionChanged(ProgressAction.Change, i, b);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mWasPlaying = isPlaying();
        if (mWasPlaying) mPlayer.pause(); // keeps the time updater running, unlike pause()
        mPositionTextView.setVisibility(VISIBLE);

        if(mCallback != null){
            mCallback.onSeekbarPositionChanged(ProgressAction.Start, 0, false);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mWasPlaying) mPlayer.start();
        if(mCallback != null){
            mCallback.onSeekbarPositionChanged(ProgressAction.Stop, 0, false);
        }
        mPositionTextView.setVisibility(GONE);
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
        if (args != null)
            message = String.format(message, args);
        Log.d("BetterVideoPlayer", message);
    }

    private void invalidateActions() {

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

    public void setLoop(boolean loop){
        this.mLoop = loop;
    }
}
