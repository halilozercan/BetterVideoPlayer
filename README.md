## Easy Video Player

Easy Video Player is a simple but powerful view that you can plugin to your apps to quickly get
video playback working.

<img src="https://raw.githubusercontent.com/afollestad/easy-video-player/master/art/showcase1.png" width="400px" />

##### Features

* *Based on the stock MediaPlayer API.* It will work on all devices and all CPUs, and it works with both local and remote sources.
* *Simple.* Much less code is required than alternative options to get up and running.
* *Very configurable.* There are lots of options available to make the player behave exactly how you want it to behave.
* *Adaptive.* The player use the colors of your (AppCompat) Activity theme automatically.

You can download a [sample APK here](https://raw.githubusercontent.com/afollestad/easy-video-player/master/sample.apk).

---

## Gradle Dependency

[ ![jCenter](https://api.bintray.com/packages/drummer-aidan/maven/easy-video-player/images/download.svg) ](https://bintray.com/drummer-aidan/maven/easy-video-player/_latestVersion)
[![Build Status](https://travis-ci.org/afollestad/easy-video-player.svg)](https://travis-ci.org/afollestad/easy-video-player)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)

The Gradle dependency is available via [jCenter](https://bintray.com/drummer-aidan/maven/easy-video-player/view).
jCenter is the default Maven repository used by Android Studio.

### Dependency

Add this in your module's `build.gradle` file:

```gradle
dependencies {
    // ... other dependencies
    compile 'com.afollestad:easyvideoplayer:0.2.12'
}
```

## Getting Started

##### Configuring a Player Activity

You will need an `Activity` in your app that will hold the `EasyVideoPlayer` view and playback content.
There's only a bit of configuration required.

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

The layout for your player Activity can be very simple. You only need a `EasyVideoPlayer` view,
all the controls and everything else are created by the player view itself.

```xml
<com.afollestad.easyvideoplayer.EasyVideoPlayer xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/player"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

##### Code Setup

Since your `Activity` is using an AppCompat theme, make sure it extends `AppCompatActivity`.

Initializing the player is very simple. You just set a callback listener to receive notifications of
important events, and a source.

```java
public class MyPlayerActivity extends AppCompatActivity implements EasyVideoCallback {

    private static final String TEST_URL = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";

    private EasyVideoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myplayer);

        // Grabs a reference to the player view
        player = (EasyVideoPlayer) findViewById(R.id.player);

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
    public void onPreparing(EasyVideoPlayer player) {
        // TODO handle if needed
    }

    @Override
    public void onPrepared(EasyVideoPlayer player) {
        // TODO handle
    }

    @Override
    public void onBuffering(int percent) {
        // TODO handle if needed
    }

    @Override
    public void onError(EasyVideoPlayer player, Exception e) {
        // TODO handle
    }

    @Override
    public void onCompletion(EasyVideoPlayer player) {
        // TODO handle if needed
    }

    @Override
    public void onRetry(EasyVideoPlayer player, Uri source) {
        // TODO handle if used
    }

    @Override
    public void onSubmit(EasyVideoPlayer player, Uri source) {
        // TODO handle if used
    }

    @Override
    public void onStarted(EasyVideoPlayer player) {
        // TODO handle if needed
    }

    @Override
    public void onPaused(EasyVideoPlayer player) {
        // TODO handle if needed
    }
}
```

You can see almost identical code in action in the sample project.

---

## Programmatic Control

Here's a list of methods that can be used to control the `EasyVideoPlayer` view programatically.
Methods used to change behavior are discussed in the next section.

```java
EasyVideoPlayer player = // ...

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
EasyVideoPlayer player = // ...

// EasyVideoPlayer.LEFT_ACTION_NONE:     hides all left actions.
// EasyVideoPlayer.LEFT_ACTION_RESTART:  the default, shows the skip back to beginning button.
// EasyVideoPlayer.LEFT_ACTION_RETRY:    shows a textual 'Retry' button, invokes the onRetry() callback method.
player.setLeftAction(int);

// EasyVideoPlayer.RIGHT_ACTION_NONE:    the default, hides all right actions.
// EasyVideoPlayer.RIGHT_ACTION_SUBMIT:  shows a textual 'Submit' button, invokes the onSubmit() callback method.
// EasyVideoPlayer.RIGHT_ACTION_LABEL:   shows a textual label that can be customized with setCustomLabelText(CharSequence) and setCustomLabelTextRes(int);
player.setRightAction(int);

// Defaults to true. The controls fade out when playback starts.
player.setHideControlsOnPlay(boolean);

// Defaults to false. Immediately starts playback when the player becomes prepared.
player.setAutoPlay(boolean);

// Sets a position that will be skipped to right when the player becomes prepared. Only happens once when set.
player.setInitialPosition(int);

// Sets a custom string for the left retry action.
player.setRetryText(CharSequence);
player.setRetryTextRes(int);

// Sets a custom string for the right submit action.
player.setSubmitText(CharSequence);
player.setSubmitTextRes(int);

// Sets a custom drawable for the left restart action.
player.setRestartDrawable(Drawable);
player.setRestartDrawableRes(int);

// Sets a custom drawable for the play button.
player.setPlayDrawable(Drawable);
player.setPlayDrawableRes(int);

// Sets a custom drawable for the pause button.
player.setPauseDrawable(Drawable);
player.setPauseDrawableRes(int);

// Sets a theme color used to color the controls and labels. Defaults to your activity's primary theme color.
player.setThemeColor(int);
player.setThemeColorRes(int);

// Sets the left and right volume levels. The player must be prepared first.
player.setVolume(float, float);

// Sets whether or not the player will toggle fullscreen for its Activity when tapped.
player.setAutoFullscreen(false);
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
    app:evp_autoPlay="false"
    app:evp_customLabelText="Custom label text if rightAction is customLabel"
    app:evp_disableControls="false"
    app:evp_hideControlsOnPlay="true"
    app:evp_leftAction="restart"
    app:evp_pauseDrawable="@drawable/evp_action_pause"
    app:evp_playDrawable="@drawable/evp_action_play"
    app:evp_restartDrawable="@drawable/evp_action_restart"
    app:evp_retryText="@string/evp_retry"
    app:evp_rightAction="none"
    app:evp_source="http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"
    app:evp_submitText="@string/evp_submit"
    app:evp_themeColor="@color/color_primary"
    app:evp_autoFullscreen="false" />
```