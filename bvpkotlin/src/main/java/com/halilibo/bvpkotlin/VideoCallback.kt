package com.halilibo.bvpkotlin

/**
 * @author Aidan Follestad
 * Modified by Halil Ozercan
 */
interface VideoCallback {

    /**
     * Called right after Android MediaPlayer is started
     * @param player
     */
    fun onStarted(player: BetterVideoPlayer)

    /**
     * Called right after Android MediaPlayer is paused
     * @param player
     */
    fun onPaused(player: BetterVideoPlayer)

    /**
     * Called just before setting the source of Android MediaPlayer
     * @param player
     */
    fun onPreparing(player: BetterVideoPlayer)

    /**
     * Called when Android MediaPlayer is prepared
     * @param player
     */
    fun onPrepared(player: BetterVideoPlayer)

    /**
     * Called whenever Android MediaPlayer fires a BufferUpdate.
     * @param percent
     */
    fun onBuffering(percent: Int)

    /**
     * Exception occurred in the player.
     * @param player
     * @param e
     */
    fun onError(player: BetterVideoPlayer, e: Exception)

    /**
     * Called after video is completed and every action is taken by the player.
     * @param player
     */
    fun onCompletion(player: BetterVideoPlayer)

    /**
     * New: Control toggling might be of importance especially when using fullscreen.
     * You might also adjust your layout or overlays according to controls' visibility.
     * This method is called whenever visibility of controls is changed.
     * @param player
     * @param isShowing : True if controls are visible.
     */
    fun onToggleControls(player: BetterVideoPlayer, isShowing: Boolean)
}