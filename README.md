## Better Video Player

Better Video Player is a rethought version(fork) of [Easy Video Player](https://github.com/afollestad/easy-video-player).

<img src="https://raw.githubusercontent.com/halilozercan/bettervideoplayer/master/screens/fullscreen.png" width="600px" />

##### Features

* __Based on the stock MediaPlayer API__ It will work on all devices and all CPUs, and it works with both local and remote sources.
* __Simple__ Much less code is required than alternative options to get up and running.
* __Very configurable__ There are lots of options available to make the player behave exactly how you want it to behave.
* __Adaptive__ The player use the colors of your (AppCompat) Activity theme automatically.
* __Swipe Gestures__ Supports the common on-screen scroll behavior which is used by MXPlayer, VLC and other Android media players.
* __Double tap to seek__ Very youtube like double tap to seek with custom time.

---

## Gradle Dependency

[![Release](https://jitpack.io/v/halilozercan/BetterVideoPlayer.svg)](https://jitpack.io/#halilozercan/BetterVideoPlayer)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)

The Gradle dependency is available via jitpack

### Dependency

Add this to your app/build.gradle repositories:

```gradle
maven { url 'https://jitpack.io' }
```

Add this in your module's `build.gradle` file:

```gradle
dependencies {
    // ... other dependencies
    compile 'com.github.halilozercan:BetterVideoPlayer:1.1.0'
}
```


## Getting Started

##### Configuring a Player Activity

You will need an `Activity` in your app that will hold the `BetterVideoPlayer` view and playback content.
There's only a bit of configuration required. However, BetterVideoPlayer offers great 'customizability'.

*First, the Activity needs to use a theme from Google AppCompat. Here's an example from the sample project:*

```xml
<style name="AppTheme" parent="Theme.AppCompat.NoActionBar">

    <item name="colorPrimary">@color/primary</item>
    <item name="colorPrimaryDark">@color/primary_dark</item>
    <item name="colorAccent">@color/accent</item>

</style>
```

*Second, the Activity should disable recreation on orientation changes. This allows playback to continue
when the device orientation changes. The player will adapt the aspect ratio accordingly. You just need to
set `android:configChanges` values to your `Activity` in `AndroidManifest.xml`:*

```xml
<activity
    android:name=".MyPlayerActivity"
    android:label="@string/my_player_activity"
    android:configChanges="orientation|keyboardHidden|screenLayout|screenSize"
    android:theme="@style/AppTheme" />   <!-- Don't need to set the theme here if it's set on your <application /> tag already -->
```

##### Layouts

The layout for your player Activity can be very simple. You only need a `BetterVideoPlayer` view,
all the controls and everything else are created by the player view itself.

```xml
<com.halilibo.bettervideoplayer.BetterVideoPlayer
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/player"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Before moving onto code setup, here is a list of changes that are made to EasyVideoPlayer.

#### Easy Video Player

Most of the features from Easy Video Player is still available in its core. Although many configuration
options were added, simplicity and ready-to-go behavior is not changed.
This document will go through added and removed features.

## Removed Features

##### Actions

In my opinion, 2 actions that are placed under MediaPlayer controls were uneasy to use.
Library should not put developer to choose between drawable and string resources for action buttons.
Also, 2 means multiple. If number of actions are 2, then it can be 10 or more.

Instead, BetterVideoPlayer takes advantage of Toolbar API. Basically, there is a toolbar at the top of
BetterVideoPlayer. Custom View API lets you to set title and populate a menu of actions on this toolbar.

*Removed anything related to actions. LeftAction, RightAction, BottomLabel, SubmitButton, RetryButton.*

##### Fullscreen

While using Easy Video Player, implementing a fullscreen video activity had been cumbersome for me.
Maybe it was my fault but I could not achieve the behavior I wanted with given fullscreen feature.
In the end I ended up changing behavior of the view completely.
In my opinion, every fullscreen video activity can have different kind of tweak for a given user input.
That is why ```setAutoFullscreen()``` has been removed from the API.

__To see an example of how you can use BetterVideoPlayer in fullscreen, refer to sample app__


## Added Features

#### Captions

BetterVideoPlayer supports captions through subtitles in 2 formats; [SRT](https://en.wikipedia.org/wiki/SubRip) and [WEBVTT](https://w3c.github.io/webvtt/).

Captions can be obtained both online and from resource directory.

Support for local storage will be added in the near future.

```
// Online SUBRIP subtitle
bvp.setCaptions("https://www.example.com/subrip.srt", SubtitleView.SubtitleMime.SUBRIP);

// res/raw SUBRIP subtitle
bvp.setCaptions(R.raw.sub, SubtitleView.SubtitleMime.SUBRIP);
```

BetterVideoPlayer also lets you define the text size(in sp) and color of captions inside XML view.

CaptionsView will be seperated from BetterVideoPlayer in next versions. This will allow developers
to customize the captions even further.

```
<com.halilibo.bettervideoplayer.BetterVideoPlayer
        android:id="@+id/bvp"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        bvp:bvp_captionSize="20sp"
        bvp:bvp_captionColor="@android:color/holo_blue_light"/>
```

#### Toolbar

BetterVideoPlayer deploys a common toolbar at the top of player. Toolbar is useful in a video player in two different ways.
- First, it offers a highly customizable title text place.
- Secondly, you can inflate a menu on the toolbar and define as many actions as you need.

To access toolbar, just use `getToolbar()` method. You can also show and hide the toolbar by using `showToolbar()` and `hideToolbar()`

#### Swipe Gestures

Swipe Gestures on a video player are proved to be very useful by MX, VLC and others. Swiping left and right to
seek to any point in video or swipe up and down to control volume and brightness. BetterVideoPlayer comes with
built-in support for these gestures. This feature lets your users to be familiar with your video player

You can enable or disable gestures by `enableSwipeGestures()` and `disableSwipeGestures()` methods.

__Important point:__ You need to use `enableSwipeGestures(Window)` method to also enable brightness control. Brightness setting needs a reference window.

![Gestures](https://github.com/halilozercan/BetterVideoPlayer/raw/master/screens/gestures.gif)


##### Code Setup

Since your `Activity` is using an AppCompat theme, make sure it extends `AppCompatActivity`.

Initializing the player is very simple. You just set a callback listener to receive notifications of
important events, and a source.

```java
public class MyPlayerActivity extends AppCompatActivity implements BetterVideoCallback {

    private static final String TEST_URL = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";

    private BetterVideoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myplayer);

        // Grabs a reference to the player view
        player = (BetterVideoPlayer) findViewById(R.id.player);

        // Sets the callback to this Activity, since it inherits EasyVideoCallback
        player.setCallback(this);

        // Sets the source to the HTTP URL held in the TEST_URL variable.
        // To play files, you can use Uri.fromFile(new File("..."))
        player.setSource(Uri.parse(TEST_URL));

        // From here, the player view will show a progress indicator until the player is prepared.
        // Once it's prepared, the progress indicator goes away and the controls become enabled for the user to begin playback.
    }

    @Override
    public void onPause() {
        super.onPause();
        // Make sure the player stops playing if the user presses the home button.
        player.pause();
    }

    // Methods for the implemented EasyVideoCallback

    @Override
    public void onStarted(BetterVideoPlayer player) {
        //Log.i(TAG, "Started");
    }

    @Override
    public void onPaused(BetterVideoPlayer player) {
        //Log.i(TAG, "Paused");
    }

    @Override
    public void onPreparing(BetterVideoPlayer player) {
        //Log.i(TAG, "Preparing");
    }

    @Override
    public void onPrepared(BetterVideoPlayer player) {
        //Log.i(TAG, "Prepared");
    }

    @Override
    public void onBuffering(int percent) {
        //Log.i(TAG, "Buffering " + percent);
    }

    @Override
    public void onError(BetterVideoPlayer player, Exception e) {
        //Log.i(TAG, "Error " +e.getMessage());
    }

    @Override
    public void onCompletion(BetterVideoPlayer player) {
        //Log.i(TAG, "Completed");
    }

    @Override
    public void onToggleControls(BetterVideoPlayer player, boolean isShowing) {
        //Log.i(TAG, "Controls toggled " + isShowing);
    }
}
```

You can see almost identical code in action in the sample project.

---

## Programmatic Control

Here's a list of methods that can be used to control the `BetterVideoPlayer` programmatically.
Full list of available methods is in [IUserMethods](https://github.com/halilozercan/BetterVideoPlayer/blob/master/bettervideoplayer/src/main/java/com/halilibo/bettervideoplayer/IUserMethods.java) interface.
Methods used to change behavior are discussed in the next section.

```java
BetterVideoPlayer player = // ...

// Sets a video source to be played.
player.setSource(Uri);

// Sets a callback to receive normal player events.
player.setCallback(EasyVideoCallback);

// Sets a callback that can be used to retrieve updates of the current playback position.
player.setProgressCallback(EasyVideoProgressCallback);

// Starts or resumes playback.
player.start();

// Seeks to a position in the video.
player.seekTo(int);

// Pauses playback.
player.pause();

// Stops playback.
player.stop();

// Resets the player, allowing a new source to be set.
player.reset();

// Releases the underlying MediaPlayer and cleans up resources.
player.release();

// Shows the default controls. They can be hidden again if the user taps the player.
player.showControls();

// Hides the default controls. They can be shown again if the user taps the player.
player.hideControls().

// Shows the controls if they're hidden, hides them if they're shown.
player.toggleControls();

// Enables double tap to seek like in Youtube. Input: seek time in MS
player.enableDoubleTapSeek(int);

// Returns true if the default controls are currently shown.
player.isControlsShown();

// Hide the default controls and prevents them from being shown.
player.disableControls();

// Undoes disableControls()
player.enableControls();

// Returns true if the player has prepared for playback entirely
player.isPrepared();

// Returns true if the player is NOT paused.
player.isPlaying();

// Returns the current position of playback.
player.getCurrentPosition();

// Returns the total duration of the video.
player.getDuration();
```

---

## Programmatic Configuration

There are a options that can be used to change the default behavior of the `EasyVideoPlayer`:

```java
BetterVideoPlayer player = // ...

// Defaults to true. The controls fade out when playback starts.
player.setHideControlsOnPlay(boolean);

// Defaults to false. Immediately starts playback when the player becomes prepared.
player.setAutoPlay(boolean);

// Sets a position that will be skipped to right when the player becomes prepared. Only happens once when set.
player.setInitialPosition(int);

// Sets a custom drawable for the left restart action.
player.setRestartDrawable(Drawable);
player.setRestartDrawableRes(int);

// Sets a custom drawable for the play button.
player.setPlayDrawable(Drawable);
player.setPlayDrawableRes(int);

// Sets a custom drawable for the pause button.
player.setPauseDrawable(Drawable);
player.setPauseDrawableRes(int);

// Sets a theme color that is used to color the seekbar and loading icon.
// Defaults to your activity's primary theme color.
player.setThemeColor(int);
player.setThemeColorRes(int);

// Sets the left and right volume levels. The player must be prepared first.
player.setVolume(float, float);

// Sets whether or not the player will start playback over when reaching the end.
player.setLoop(false);

// Registers a caption source
player.setCaptions(Uri, mimeType);

// Sets a style from SpinKit.
player.setLoadingStyle(int);

// Sets the current window. Useful when you want to enable brightness setting in swipe.
player.setWindow(Window);
```

---

## XML Configuration

The programmatic configuration options shown above can also be configured directly from your layout:

```xml
<com.afollestad.easyvideoplayer.EasyVideoPlayer xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/player"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:bvp_autoPlay="false"
    app:bvp_disableControls="false"
    app:bvp_hideControlsOnPlay="true"
    app:bvp_pauseDrawable="@drawable/bvp_action_pause"
    app:bvp_playDrawable="@drawable/bvp_action_play"
    app:bvp_restartDrawable="@drawable/bvp_action_restart"
    app:bvp_source="http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"
    app:bvp_themeColor="@color/color_primary"
    app:bvp_captionSize="22sp"
    app:bvp_captionColor="@color/caption_color"
    app:bvp_loadingStyle="DoubleBounce"
    app:bvp_loop="false" />
```

#### Final note
While I try to complete this README, you can check out Sample project of this repo. It will be updated often and
written code sometimes help a lot more than a poorly written documentation.
