package com.afollestad.easyvideoplayer;

import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * @author Aidan Follestad (afollestad)
 */
interface IUserMethods {

    void setSource(@NonNull Uri source);

    void setCallback(@NonNull EasyVideoCallback callback);

    void setLeftAction(@EasyVideoPlayer.LeftAction int action);

    void setRightAction(@EasyVideoPlayer.RightAction int action);

    void setHideControlsOnPlay(boolean hide);

    void setAutoPlay(boolean autoPlay);

    void setInitialPosition(int pos);

    void showControls();

    void hideControls();

    boolean isControlsShown();

    void toggleControls();

    boolean isPrepared();

    boolean isPlaying();

    int getCurrentPosition();

    int getDuration();

    void start();

    void seekTo(int pos);

    void pause();

    void stop();

    void reset();

    void release();
}