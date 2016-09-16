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

**Removed anything related to Actions. LeftAction, RightAction, BottomLabel, SubmitButton, RestartButton.**

##### Fullscreen

While using Easy Video Player, implementing a fullscreen video activity had been cumbersome for me.
Maybe it was my fault but I could not achieve the behavior I wanted with given fullscreen feature.
In the end I ended up changing behavior of the view completely.
In my opinion, every fullscreen video activity can have different kind of tweak for a given user input.
That is why ```setAutoFullscreen()``` has been removed from the API.


## Added Features

#### Subtitles

_TODO: Documentation for subtitles._

#### Toolbar

_TODO: Documentation for Toolbar._

#### Volume and Position Control

_TODO: Explanation of volume and position control._

While I try to complete this README, you can check out Sample project of this repo. It will be updated often and
written code sometimes help a lot more than a poorly written documentation.