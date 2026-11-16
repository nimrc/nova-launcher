package dev.nova.launcher;

import java.util.LinkedHashMap;
import java.util.Map;

final class Game {
    static final String RETROARCH = "com.retroarch.aarch64";
    static final String DOLPHIN = "org.dolphinemu.dolphinemu";

    String title = "", subtitle = "", pkg, cover, activity;
    final Map<String, String> extras = new LinkedHashMap<>();
    Game[] children;

    void retroarch(String rom, String core) {
        if (pkg == null) pkg = RETROARCH;
        activity = "com.retroarch.browser.retroactivity.RetroActivityFuture";
        extras.put("ROM", rom);
        extras.put("LIBRETRO", "/data/data/" + pkg + "/cores/" + core + "_libretro_android.so");
        extras.put("CONFIGFILE", "/storage/emulated/0/Android/data/" + pkg + "/files/retroarch.cfg");
    }

    // Standalone Dolphin (GameCube/Wii) boots the file passed as AutoStartFile.
    void dolphin(String rom) {
        if (pkg == null) pkg = DOLPHIN;
        activity = "org.dolphinemu.dolphinemu.ui.main.MainActivity";
        extras.put("AutoStartFile", rom);
    }
}
