package com.halilibo.bettervideoplayer;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.Window;

import com.halilibo.bettervideoplayer.subtitle.CaptionsView;

import java.util.Map;

/**
 * @author Aidan Follestad (halilibo)
 */
interface IUserMethods {

    void setSource(@NonNull Uri source);

    void setSource(@NonNull Uri source, @NonNull Map<String, String> headers);

    void setCallback(@NonNull BetterVideoCallback callback);

    void setProgressCallback(@NonNull BetterVideoProgressCallback callback);

    void setTitle(@NonNull String text);

    void setTitle(@StringRes int resId);

    void setMenu(@MenuRes int resId);

    void setMenuCallback(@NonNull Toolbar.OnMenuItemClickListener callback);

    void setPlayDrawable(@NonNull Drawable drawable);

    void setPlayDrawable(@DrawableRes int res);

    void setPauseDrawable(@NonNull Drawable drawable);

    void setPauseDrawable(@DrawableRes int res);

    /**
     Required to activate brightness setting.
     @param window; Current window that BetterVideoPlayer is part of.
     */
    void setWindow(@NonNull Window window);

    void setHideControlsOnPlay(boolean hide);

    void setAutoPlay(boolean autoPlay);

    void setInitialPosition(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos);

    void showControls();

    void hideControls();

    @CheckResult
    boolean isControlsShown();

    void toggleControls();

    void enableControls(boolean andShow);

    void disableControls();

    @CheckResult
    boolean isPrepared();

    @CheckResult
    boolean isPlaying();

    @CheckResult
    int getCurrentPosition();

    @CheckResult
    int getDuration();

    void start();

    void seekTo(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos);

    void setVolume(@FloatRange(from = 0f, to = 1f) float leftVolume, @FloatRange(from = 0f, to = 1f) float rightVolume);

    void pause();

    void stop();

    void reset();

    void release();

    void setCaptions(Uri source, CaptionsView.CMime subMime);

    void setCaptions(@RawRes int resId, CaptionsView.CMime subMime);

    void removeCaptions();

    void setLoop(boolean loop);

    void setLoadingStyle(@BetterVideoPlayer.LoadingStyle int style);
}