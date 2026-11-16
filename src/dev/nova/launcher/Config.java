package dev.nova.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Games and covers live on shared storage so they can be edited without rebuilding:
//   /sdcard/Nova/games.json, /sdcard/Nova/covers/<key>.jpg|png
final class Config {
    static final File DIR = new File(Environment.getExternalStorageDirectory(), "Nova");
    static final File FILE = new File(DIR, "games.json");
    private static final File COVERS = new File(DIR, "covers");

    private static Game[] games;
    private static String stamp;
    static String error;

    static boolean canRead() {
        return Environment.isExternalStorageManager();
    }

    static Game[] games(Context c) {
        if (games == null || stale()) load(c);
        return games;
    }

    static boolean stale() {
        return games != null && !currentStamp().equals(stamp);
    }

    private static String currentStamp() {
        return canRead() + ":" + FILE.lastModified() + ":" + FILE.length();
    }

    private static void load(Context c) {
        error = null;
        if (canRead() && !FILE.exists()) writeDefault(c);
        if (canRead() && FILE.exists()) {
            try {
                String json = new String(Files.readAllBytes(FILE.toPath()), StandardCharsets.UTF_8);
                games = parse(new JSONObject(json).getJSONArray("games"));
                stamp = currentStamp();
                return;
            } catch (IOException | JSONException e) {
                error = "games.json: " + e.getMessage();
            }
        }
        games = installedGames(c);
        stamp = currentStamp();
    }

    private static Game[] parse(JSONArray a) throws JSONException {
        Game[] out = new Game[a.length()];
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            Game g = new Game();
            g.title = o.getString("title");
            g.subtitle = o.optString("subtitle");
            g.pkg = o.optString("package", null);
            g.activity = o.optString("activity", null);
            JSONObject extras = o.optJSONObject("extras");
            if (extras != null) {
                for (Iterator<String> k = extras.keys(); k.hasNext(); ) {
                    String key = k.next();
                    g.extras.put(key, extras.getString(key));
                }
            }
            JSONObject ra = o.optJSONObject("retroarch");
            if (ra != null) g.retroarch(ra.getString("rom"), ra.getString("core"));
            if (o.has("dolphin")) g.dolphin(o.getString("dolphin"));
            if (o.has("games")) g.children = parse(o.getJSONArray("games"));
            g.cover = o.optString("cover", null);
            if (g.cover == null) g.cover = g.extras.containsKey("ROM") ? baseName(g.extras.get("ROM"))
                    : g.extras.containsKey("AutoStartFile") ? baseName(g.extras.get("AutoStartFile")) : g.pkg;
            if (g.pkg == null && g.children == null) throw new JSONException("\"" + g.title + "\" needs a package, retroarch, dolphin or games");
            out[i] = g;
        }
        return out;
    }

    private static String baseName(String path) {
        String n = new File(path).getName();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    // Apps the system classifies as games, plus RetroArch, sorted by name.
    private static Game[] installedGames(Context c) {
        PackageManager pm = c.getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        List<Game> out = new ArrayList<>();
        for (ResolveInfo r : apps) {
            ApplicationInfo ai = r.activityInfo.applicationInfo;
            boolean game = ai.category == ApplicationInfo.CATEGORY_GAME
                    || (ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0
                    || ai.packageName.equals(Game.RETROARCH);
            if (!game || ai.packageName.equals(c.getPackageName())) continue;
            Game g = new Game();
            g.title = r.loadLabel(pm).toString();
            g.pkg = g.cover = ai.packageName;
            out.add(g);
        }
        Collator collator = Collator.getInstance();
        out.sort((x, y) -> collator.compare(x.title, y.title));
        return out.toArray(new Game[0]);
    }

    private static void writeDefault(Context c) {
        try {
            JSONArray a = new JSONArray();
            for (Game g : installedGames(c)) {
                a.put(new JSONObject().put("title", g.title).put("subtitle", "").put("package", g.pkg));
            }
            COVERS.mkdirs();
            Files.write(FILE.toPath(), new JSONObject().put("games", a).toString(2)
                    .replace("\\/", "/").getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException ignored) {
        }
    }

    static Bitmap cover(String key) {
        if (key == null || !canRead()) return null;
        for (String ext : new String[]{".jpg", ".png", ".webp"}) {
            File f = new File(COVERS, key + ext);
            if (f.exists()) return BitmapFactory.decodeFile(f.getPath());
        }
        return null;
    }
}
