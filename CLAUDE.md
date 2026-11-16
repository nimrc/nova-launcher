# Nova launcher

Lightweight big-card game launcher for the Retroid Pocket Nova (1280x960 landscape). Plain Java, no AndroidX, no Gradle. Set as the device's default home screen.

- Build + install + start: `ANDROID_SERIAL=<serial> ./build.sh install`. Private setup (serial, hosts, server URLs) lives in the untracked `CLAUDE.local.md`.
- Verify every change on device with a screenshot: `adb exec-out screencap -p > shot.png`, navigate with `input keyevent KEYCODE_DPAD_RIGHT` / `KEYCODE_BUTTON_A` / `KEYCODE_BUTTON_B` / `KEYCODE_BUTTON_Y`.
- Set as the default home screen: `adb shell cmd package set-home-activity dev.nova.launcher/.MainActivity`.
- A hung RetroArch window can keep input focus even after `am force-stop`. Check with a screenshot, not only `dumpsys window`.

## Adding a game

Add an entry to `GAMES` in `src/dev/nova/launcher/MainActivity.java`. Order in the array = card order.

- Title is Chinese, subtitle is the English name (append ` · <platform>` for emulated games).
- Native Android game: `new Game("中文名", "English Name", "<package>")`. Find the package with `adb shell pm list packages -3`.
- RetroArch game: chain `.cover("<snake_case_name>").retroarch("<rom path>", "<core>")`. `core` is the `.so` name without `_libretro_android.so`.
  - For games already played, take the ROM path and core from `/sdcard/RetroArch/playlists/builtin/content_history.lpl`.
  - Known-good cores: `fbneo` (arcade and Neo Geo; the BIOS `neogeo.zip` is in `/sdcard/RetroArch/system`), `flycast` (Atomiswave/Naomi, e.g. `mslug6.zip`; FBNeo shows "unknown romset" for these; first boot takes ~30 s), `pcsx_rearmed` (PS1), `melonds` (NDS), `mgba` (GB/GBC/GBA). `mesen` hung (ANR) on `The Legend of Kage.nes`. `nestopia` is not installed.
  - GameCube/Wii: standalone Dolphin, via `.dolphin("<rom path>")`, which passes the ROM as the `AutoStartFile` extra.
  - Arcade ROMs go in `/sdcard/ROMs/arcade/`, others in `/sdcard/ROMs/<platform>/`.
  - Test the launch before adding the card: `am start -n com.retroarch.aarch64/com.retroarch.browser.retroactivity.RetroActivityFuture -e ROM ... -e LIBRETRO ... -e CONFIGFILE /storage/emulated/0/Android/data/com.retroarch.aarch64/files/retroarch.cfg`. Then take a screenshot ~12 s later. The `[ENV] Auto-start game` log line only proves RetroArch received the intent, not that the core ran.
- Series or collections: `new Game(...).cover(...).collection(child, child, ...)`. Pressing A opens a second-level page (`CollectionActivity`) with the children as cards. B goes back.

## Getting ROMs from RomM

The user's RomM server URL is `$ROMM_URL` (see `CLAUDE.local.md`). Use the API with `Authorization: Bearer $ROMM_API_TOKEN`, reading the token from a fresh login shell (`zsh -lic 'printf %s "$ROMM_API_TOKEN"'`).

- Search: `GET /api/roms?search_term=<q>&limit=50`. Chinese names in RomM don't always match the common name, so also search the English title and the MAME set name (e.g. `dino`, `wof`, `mslug`).
- Device Wi-Fi only manages ~200 KB/s, while the Mac gets ~4 MB/s. For anything big, download on the Mac and `adb push` (~40 MB/s), then delete the local copy. The RomM server has gone 502 for several minutes after a few large downloads: poll `/api/heartbeat` before each file and pause between files. Inside a `while read` loop, read the list from another fd (`read -u 3 … 3< list`), because `adb shell` swallows stdin.
- Small files can go straight onto the device: `adb shell "curl -sS -f -H 'Authorization: Bearer …' -o '<dest>.part' '$ROMM_URL/api/roms/<id>/content/<url-encoded fs_name>' && mv '<dest>.part' '<dest>'"`.
  - Run multi-file downloads as a background job on the Mac, one `adb shell` per file. `nohup … &` inside `adb shell` gets killed when adb exits.
- Check the file size against `fs_size_bytes`. For zips, RomM's `md5_hash` doesn't match the file's md5, so use `unzip -t` instead.

## Cover art

Every card must use a full-bleed cover. Never fall back to the app icon or a letterboxed image.

- Store the cover at `assets/covers/<key>.jpg`. `<key>` is the package name, or the `.cover(...)` name when set.
- Size: exactly 600x900 (2:3 portrait), JPEG quality 85.
- Use official art, in order of preference:
  1. A Steam library capsule, if the game is on Steam: `https://cdn.cloudflare.steamstatic.com/steam/apps/<appid>/library_600x900.jpg`.
  2. An official boxart or arcade flyer from libretro-thumbnails, center-cropped to 2:3: `https://raw.githubusercontent.com/libretro-thumbnails/<System_repo>/master/Named_Boxarts/<Name>.png`. Find exact file names with `gh api 'repos/libretro-thumbnails/<System_repo>/git/trees/master?recursive=1'` (e.g. `FBNeo_-_Arcade_Games`, `Nintendo_-_Nintendo_DS`). Crop from the top instead of the center when the title would be cut off.
- Use `gpt-image-2` (`POST /v1/images/edits`, `size=1024x1536`, `quality=high`) only for non-character art, such as a poster built around an app's logo. Never use it to redraw game characters. Read `OPENAI_API_KEY` from a fresh login shell, and use `curl --http1.1`.
- Tools (macOS, run with `swift`):
  - `tools/crop.swift in out.jpg [top]` center-crops to 2:3 (or from the top) and writes a 600x900 JPEG.
  - `tools/title.swift in out.jpg "中文名" "ENGLISH NAME" ["合 集 · COLLECTION" "#hex"]` draws a bottom title block onto a 600x900 cover; the optional pair adds an obi band (collections use it; Metal Slug `#C62828`, Zelda `#1E6B45`).
- Collection cards get an original gpt-image-2 scene evoking the series, with no franchise characters or logos, plus a title drawn with `tools/title.swift`. Never let the model render the title text. Individual game cards keep their official art.
- Look at every cover on the device before finishing.
