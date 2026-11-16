# Nova Launcher

A lightweight big-card game launcher for the Retroid Pocket Nova (Android 13, 1280x960). It is plain Java with no AndroidX and no Gradle, and the whole APK is a few MB of cover art plus four classes. It can be set as the default home screen.

![Home](docs/home.png)

## Features

- Full-bleed 2:3 cover cards, three on screen, with a blurred background of the selected cover.
- Native Android games, RetroArch games (ROM and core passed through the launch intent) and standalone Dolphin (GameCube) games.
- Collections open a second-level page. Examples are the Metal Slug and Zelda series.
- Gamepad-first controls: Ⓐ starts a game, Ⓑ goes back, Ⓨ opens the app drawer. The selected card is remembered.
- A clock and battery level in the header. Games that aren't installed are greyed out.

![Collections on the home screen](docs/home-collections.png)

![Inside a collection](docs/collection.png)

![App drawer (Ⓨ)](docs/apps.png)

## Build

Requires the Android SDK (`build-tools/36.0.0`, `platforms/android-37.0`) and JDK 17+.

```sh
./build.sh                              # build/nova-launcher.apk
ANDROID_SERIAL=<serial> ./build.sh install  # install and start
adb shell cmd package set-home-activity dev.nova.launcher/.MainActivity
```

## Adding games

Games are listed in `GAMES` in `src/dev/nova/launcher/MainActivity.java`, and covers live in `assets/covers/<key>.jpg` (600x900). Example entries:

```java
new Game("空洞骑士", "Hollow Knight", "org.nx2a.t0100633007d48000"),
new Game("恐龙快打", "Cadillacs and Dinosaurs · Arcade", RETROARCH)
        .cover("dino").retroarch("/storage/emulated/0/ROMs/arcade/dino.zip", "fbneo"),
new Game("合金弹头 合集", "Metal Slug Collection", RETROARCH)
        .cover("metal_slug_collection").collection(/* child games */),
```

See [CLAUDE.md](CLAUDE.md) for the full guide: emulator cores, getting ROMs from RomM, and the rules for cover art.

`tools/crop.swift` crops artwork to 600x900. `tools/title.swift` draws the title and "合集" obi band on collection covers.
