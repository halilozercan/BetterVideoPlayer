package com.afollestad.easyvideoplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Aidan Follestad (afollestad)
 */
public class EasyVideoPlayer extends FrameLayout implements IUserMethods, TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnErrorListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    @IntDef({LEFT_ACTION_NONE, LEFT_ACTION_RESTART, LEFT_ACTION_RETRY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LeftAction {
    }

    @IntDef({RIGHT_ACTION_NONE, RIGHT_ACTION_SUBMIT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RightAction {
    }

    public static final int LEFT_ACTION_NONE = 0;
    public static final int LEFT_ACTION_RESTART = 1;
    public static final int LEFT_ACTION_RETRY = 2;
    public static final int RIGHT_ACTION_NONE = 3;
    public static final int RIGHT_ACTION_SUBMIT = 4;
    private static final int UPDATE_INTERVAL = 200;

    public EasyVideoPlayer(Context context) {
        super(context);
        init();
    }

    public EasyVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EasyVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private TextureView mTextureView;
    private Surface mSurface;

    private View mControlsFrame;
    private View mProgressFrame;
    private View mClickFrame;

    private SeekBar mSeeker;
    private TextView mLabelPosition;
    private TextView mLabelDuration;
    private ImageButton mBtnRestart;
    private Button mBtnRetry;
    private ImageButton mBtnPlayPause;
    private Button mBtnSubmit;

    private MediaPlayer mPlayer;
    private boolean mSurfaceAvailable;
    private boolean mIsPrepared;
    private boolean mWasPlaying;
    private int mInitialTextureWidth;
    private int mInitialTextureHeight;

    private Handler mHandler;

    private Uri mSource;
    private EasyVideoCallback mCallback;
    @LeftAction
    private int mLeftAction = LEFT_ACTION_RESTART;
    @RightAction
    private int mRightAction = RIGHT_ACTION_NONE;
    private boolean mHideControlsOnPlay = true;
    private boolean mAutoPlay;
    private int mInitialPosition = -1;

    // Runnable used to run code on an interval to update counters and seeker
    private final Runnable mUpdateCounters = new Runnable() {
        @Override
        public void run() {
            if (mHandler == null || !mIsPrepared || mSeeker == null || mPlayer == null)
                return;
            final int pos = mPlayer.getCurrentPosition();
            final int dur = mPlayer.getDuration();
            mLabelPosition.setText(Util.getDurationString(pos, false));
            mLabelDuration.setText(Util.getDurationString(dur - pos, true));
            mSeeker.setProgress(pos);

            if (mHandler != null)
                mHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };


    private void init() {
        setBackgroundColor(Color.BLACK);
    }

    @Override
    public void setSource(@NonNull Uri source) {
        mSource = source;
        if (mPlayer != null) prepare();
    }

    @Override
    public void setCallback(@NonNull EasyVideoCallback callback) {
        mCallback = callback;
    }

    @Override
    public void setLeftAction(@LeftAction int action) {
        if (action < LEFT_ACTION_NONE || action > LEFT_ACTION_RETRY)
            throw new IllegalArgumentException("Invalid left action specified.");
        mLeftAction = action;
        invalidateActions();
    }

    @Override
    public void setRightAction(@RightAction int action) {
        if (action < RIGHT_ACTION_NONE || action > RIGHT_ACTION_SUBMIT)
            throw new IllegalArgumentException("Invalid right action specified.");
        mRightAction = action;
        invalidateActions();
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
    public void setInitialPosition(int pos) {
        mInitialPosition = pos;
    }

    private void prepare() {
        if (!mSurfaceAvailable || mSource == null || mPlayer == null || mIsPrepared)
            return;
        try {
            if (mCallback != null)
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
        if (mSeeker == null) return;
        mSeeker.setEnabled(enabled);
        mBtnPlayPause.setEnabled(enabled);
        mBtnSubmit.setEnabled(enabled);
        mBtnRestart.setEnabled(enabled);
        mBtnRetry.setEnabled(false);

        final float disabledAlpha = .4f;
        mBtnPlayPause.setAlpha(enabled ? 1f : disabledAlpha);
        mBtnSubmit.setAlpha(enabled ? 1f : disabledAlpha);
        mBtnRestart.setAlpha(enabled ? 1f : disabledAlpha);

        mClickFrame.setEnabled(enabled);
    }

    @Override
    public void showControls() {
        if (isControlsShown() || mSeeker == null) return;
        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(0f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mControlsFrame.animate().alpha(1f).setListener(null)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    @Override
    public void hideControls() {
        if (!isControlsShown() || mSeeker == null) return;
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
    }

    @Override
    public boolean isControlsShown() {
        return mControlsFrame != null && mControlsFrame.getAlpha() > .5f;
    }

    @Override
    public void toggleControls() {
        if (isControlsShown()) {
            hideControls();
        } else {
            showControls();
        }
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
        if (mHandler == null) mHandler = new Handler();
        mHandler.post(mUpdateCounters);
        mBtnPlayPause.setImageResource(R.drawable.evp_action_pause);
    }

    @Override
    public void seekTo(int pos) {
        if (mPlayer == null) return;
        mPlayer.seekTo(pos);
    }

    @Override
    public void pause() {
        if (mPlayer == null || !isPlaying()) return;
        mPlayer.pause();
        if (mHandler == null) return;
        mHandler.removeCallbacks(mUpdateCounters);
        mBtnPlayPause.setImageResource(R.drawable.evp_action_play);
    }

    @Override
    public void stop() {
        if (mPlayer == null) return;
        try {
            mPlayer.stop();
        } catch (Throwable ignored) {
        }
        if (mHandler == null) return;
        mHandler.removeCallbacks(mUpdateCounters);
        mBtnPlayPause.setImageResource(R.drawable.evp_action_pause);
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
        if (mPlayer == null) return;
        mIsPrepared = false;

        try {
            mPlayer.release();
        } catch (Throwable ignored) {
        }
        mPlayer = null;

        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateCounters);
            mHandler = null;
        }

        LOG("Released player and Handler");
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

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        LOG("onPrepared()");
        mProgressFrame.setVisibility(View.INVISIBLE);
        mIsPrepared = true;
        if (mCallback != null)
            mCallback.onPrepared(this);
        mLabelPosition.setText(Util.getDurationString(0, false));
        mLabelDuration.setText(Util.getDurationString(mediaPlayer.getDuration(), false));
        mSeeker.setProgress(0);
        mSeeker.setMax(mediaPlayer.getDuration());
        setControlsEnabled(true);

        if (mAutoPlay) {
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
        if (mCallback != null)
            mCallback.onCompletion(this);
        mBtnPlayPause.setImageResource(R.drawable.evp_action_play);
        if (mHandler != null)
            mHandler.removeCallbacks(mUpdateCounters);
        showControls();
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

        mHandler = new Handler();
        mPlayer = new MediaPlayer();
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnVideoSizeChangedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        // Instantiate and add TextureView for rendering
        final FrameLayout.LayoutParams textureLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mTextureView = new TextureView(getContext());
        mTextureView.setBackgroundColor(Color.BLACK);
        addView(mTextureView, textureLp);
        mTextureView.setSurfaceTextureListener(this);

        final LayoutInflater li = LayoutInflater.from(getContext());

        // Inflate and add progress
        mProgressFrame = li.inflate(R.layout.evp_include_progress, this, false);
        addView(mProgressFrame);

        // Instantiate and add click frame (used to toggle controls)
        mClickFrame = new FrameLayout(getContext());
        //noinspection RedundantCast
        ((FrameLayout) mClickFrame).setForeground(Util.resolveDrawable(getContext(), R.attr.selectableItemBackground));
        addView(mClickFrame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mClickFrame.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleControls();
            }
        });

        // Inflate controls
        mControlsFrame = li.inflate(R.layout.evp_include_controls, this, false);
        final FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsLp.gravity = Gravity.BOTTOM;
        addView(mControlsFrame, controlsLp);

        // Retrieve controls
        mSeeker = (SeekBar) mControlsFrame.findViewById(R.id.seeker);
        mLabelPosition = (TextView) mControlsFrame.findViewById(R.id.position);
        mLabelDuration = (TextView) mControlsFrame.findViewById(R.id.duration);
        mBtnRestart = (ImageButton) mControlsFrame.findViewById(R.id.btnRestart);
        mBtnRestart.setOnClickListener(this);
        mBtnRetry = (Button) mControlsFrame.findViewById(R.id.btnRetry);
        mBtnRetry.setOnClickListener(this);
        mBtnPlayPause = (ImageButton) mControlsFrame.findViewById(R.id.btnPlayPause);
        mBtnPlayPause.setOnClickListener(this);
        mBtnSubmit = (Button) mControlsFrame.findViewById(R.id.btnSubmit);
        mBtnSubmit.setOnClickListener(this);
        mSeeker.setOnSeekBarChangeListener(this);
        setControlsEnabled(false);

        final int primaryColor = Util.resolveColor(getContext(), R.attr.colorPrimary);
        mControlsFrame.setBackgroundColor(Util.adjustAlpha(primaryColor, 0.8f));
        final int labelColor = Util.isColorDark(primaryColor) ? Color.WHITE : Color.BLACK;
        mLabelPosition.setTextColor(labelColor);
        mLabelPosition.setText(Util.getDurationString(0, false));
        mLabelDuration.setTextColor(labelColor);
        mLabelDuration.setText(Util.getDurationString(0, true));

        invalidateActions();
        prepare();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnPlayPause) {
            if (mPlayer.isPlaying()) {
                pause();
            } else {
                if (mHideControlsOnPlay)
                    hideControls();
                start();
            }
        } else if (view.getId() == R.id.btnRestart) {
            seekTo(0);
            if (!isPlaying()) start();
        } else if (view.getId() == R.id.btnRetry) {
            if (mCallback != null)
                mCallback.onRetry(this, mSource);
        } else if (view.getId() == R.id.btnSubmit) {
            if (mCallback != null)
                mCallback.onSubmit(this, mSource);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
        if (fromUser) seekTo(value);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mWasPlaying = isPlaying();
        if (mWasPlaying) mPlayer.pause(); // keeps the time updater running, unlike pause()
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mWasPlaying) mPlayer.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        LOG("Detached from window");
        if (mPlayer != null) {
            stop();
            release();
        }

        mTextureView = null;
        mSurface = null;

        mSeeker = null;
        mLabelPosition = null;
        mLabelDuration = null;
        mBtnPlayPause = null;
        mBtnRestart = null;
        mBtnSubmit = null;

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
        Log.d("EasyVideoPlayer", message);
    }

    private void invalidateActions() {
        switch (mLeftAction) {
            case LEFT_ACTION_NONE:
                mBtnRetry.setVisibility(View.GONE);
                mBtnRestart.setVisibility(View.GONE);
                break;
            case LEFT_ACTION_RESTART:
                mBtnRetry.setVisibility(View.GONE);
                mBtnRestart.setVisibility(View.VISIBLE);
                break;
            case LEFT_ACTION_RETRY:
                mBtnRetry.setVisibility(View.VISIBLE);
                mBtnRestart.setVisibility(View.GONE);
                break;
        }
        switch (mRightAction) {
            case RIGHT_ACTION_NONE:
                mBtnSubmit.setVisibility(View.GONE);
                break;
            case RIGHT_ACTION_SUBMIT:
                mBtnSubmit.setVisibility(View.VISIBLE);
                break;
        }
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
}