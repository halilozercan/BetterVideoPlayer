package com.halilibo.bettervideoplayer;

/**
 * @author Aidan Follestad (halilibo)
 */
public interface BetterVideoCallback {

    void onStarted(BetterVideoPlayer player);

    void onPaused(BetterVideoPlayer player);

    void onPreparing(BetterVideoPlayer player);

    void onPrepared(BetterVideoPlayer player);

    void onBuffering(int percent);

    void onError(BetterVideoPlayer player, Exception e);

    void onCompletion(BetterVideoPlayer player);

    void onLeftButton(BetterVideoPlayer player);

    void onRightButton(BetterVideoPlayer player);

    void onSeekbarPositionChanged(ProgressAction action, int progress, boolean byUser);

    void onClicked(BetterVideoPlayer player);
}