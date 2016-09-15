package com.halilibo.bettervideoplayer;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;

/**
 * @author Aidan Follestad (halilibo)
 */
interface IUserMethods {

    void setSource(@NonNull Uri source);

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

    void setSubtitle(Uri source);

    void setSubtitle(@RawRes int resId);

    void removeSubtitle();

    void setLoop(boolean loop);
}