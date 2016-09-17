## Better Video Player

Better Video Player is a rethought version(fork) of [Easy Video Player](https://github.com/afollestad/easy-video-player).

<img src="https://raw.githubusercontent.com/halilozercan/bettervideoplayer/master/screens/fullscreen.png" width="600px" />

#### Easy Video Player

Most of the features from Easy Video Player are still available.
You can go to source repository and check the well written documentation and README to get started.
This document will go through the added and removed features.

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


## Added Features

#### Captions

BetterVideoPlayer supports captions through subtitles in 2 formats; [SRT](https://en.wikipedia.org/wiki/SubRip) and [WEBVTT](https://w3c.github.io/webvtt/).

Captions can be obtained both online and from resource directory.

```
// Online SUBRIP subtitle
bvp.setCaptions("https://www.example.com/subrip.srt", SubtitleView.SubtitleMime.SUBRIP);

// res/raw SUBRIP subtitle
bvp.setCaptions(R.raw.sub, SubtitleView.SubtitleMime.SUBRIP);
```

BetterVideoPlayer also lets you define the text size(in sp) and color of captions inside XML view.

```
<com.halilibo.bettervideoplayer.BetterVideoPlayer
        android:id="@+id/bvp"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        bvp:bvp_captionSize="20sp"
        bvp:bvp_captionColor="@android:color/holo_blue_light"/>
```

#### Toolbar

_TODO: Documentation for Toolbar._

#### Volume and Position Control

_TODO: Gif for volume and position control._

Volume and track position control with swipe gestures are usually supported by
mostly used media players like VLC or MX Player. This feature lets your users to be
familiar with your video player. For now, this option cannot be disabled but this will
be possible in the future.

#### Final note
While I try to complete this README, you can check out Sample project of this repo. It will be updated often and
written code sometimes help a lot more than a poorly written documentation.