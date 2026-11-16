package dev.nova.launcher;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Base {
    static final String EXTRA_COLLECTION = "collection";
    private static final String RETROARCH = "com.retroarch.aarch64";

    private static final class Game {
        final String title, subtitle, pkg;
        String cover, activity;
        String[] extras = {};
        Game[] children;

        Game(String title, String subtitle, String pkg) {
            this.title = title;
            this.subtitle = subtitle;
            this.pkg = pkg;
            this.cover = pkg;
        }

        // Cover art: assets/covers/<name>.jpg or .png; falls back to the app icon.
        Game cover(String name) {
            cover = name;
            return this;
        }

        // Opens a second-level page listing the children instead of launching anything.
        Game collection(Game... games) {
            children = games;
            return this;
        }

        // Standalone Dolphin (GameCube/Wii); the launcher passes the ROM via AutoStartFile.
        Game dolphin(String rom) {
            activity = "org.dolphinemu.dolphinemu.ui.main.MainActivity";
            extras = new String[]{"AutoStartFile", rom};
            return this;
        }

        Game retroarch(String rom, String core) {
            activity = "com.retroarch.browser.retroactivity.RetroActivityFuture";
            extras = new String[]{
                    "ROM", rom,
                    "LIBRETRO", "/data/data/" + RETROARCH + "/cores/" + core + "_libretro_android.so",
                    "CONFIGFILE", "/storage/emulated/0/Android/data/" + RETROARCH + "/files/retroarch.cfg",
            };
            return this;
        }
    }

    private static final Game[] GAMES = {
            new Game("空洞骑士", "Hollow Knight", "org.nx2a.t0100633007d48000"),
            new Game("空洞骑士：丝之歌", "Hollow Knight: Silksong", "org.nx2a.hollowknightsilksong"),
            new Game("小丑牌", "Balatro", "com.playstack.balatro.android"),
            new Game("吸血鬼幸存者", "Vampire Survivors", "com.poncle.vampiresurvivors"),
            new Game("潜水员戴夫", "Dave the Diver", "com.xd.dave.tap.cn"),
            new Game("蔚蓝", "Celeste", "org.celesteandroid.celeste"),
            new Game("泰拉瑞亚", "Terraria", "com.and.games505.TerrariaPaid"),
            new Game("星露谷物语", "Stardew Valley", "com.chucklefish.stardewvalley"),
            new Game("我的世界", "Minecraft", "com.minecraft.ow"),
            new Game("数码宝贝大乱斗", "Digimon Rumble Arena · PlayStation", RETROARCH)
                    .cover("digimon_rumble_arena")
                    .retroarch("/storage/emulated/0/Cartnest/ROMs/psx/Digimon Rumble Arena.PBP", "pcsx_rearmed"),
            new Game("口袋妖怪 金", "Pokémon Gold · Game Boy Color", RETROARCH)
                    .cover("pokemon_gold").retroarch("/storage/emulated/0/ROMs/gbc/Pokemon_Gold.gbc", "mgba"),
            new Game("恐龙快打", "Cadillacs and Dinosaurs · Arcade", RETROARCH)
                    .cover("dino").retroarch("/storage/emulated/0/ROMs/arcade/dino.zip", "fbneo"),
            new Game("吞食天地2", "Warriors of Fate · Arcade", RETROARCH)
                    .cover("wof").retroarch("/storage/emulated/0/ROMs/arcade/wof.zip", "fbneo"),
            new Game("影之传说", "The Legend of Kage · Arcade", RETROARCH)
                    .cover("lkage").retroarch("/storage/emulated/0/ROMs/arcade/lkage.zip", "fbneo"),
            new Game("合金弹头 合集", "Metal Slug Collection · 8 部", RETROARCH)
                    .cover("metal_slug_collection").collection(
                            neogeo("合金弹头", "Metal Slug", "mslug"),
                            neogeo("合金弹头2", "Metal Slug 2", "mslug2"),
                            neogeo("合金弹头X", "Metal Slug X", "mslugx"),
                            neogeo("合金弹头3", "Metal Slug 3", "mslug3"),
                            neogeo("合金弹头4", "Metal Slug 4", "mslug4"),
                            neogeo("合金弹头5", "Metal Slug 5", "mslug5"),
                            new Game("合金弹头6", "Metal Slug 6 · Atomiswave", RETROARCH)
                                    .cover("mslug6").retroarch("/storage/emulated/0/ROMs/arcade/mslug6.zip", "flycast"),
                            new Game("合金弹头7", "Metal Slug 7 · Nintendo DS", RETROARCH)
                                    .cover("mslug7").retroarch("/storage/emulated/0/ROMs/nds/Metal_Slug_7.nds", "melonds")),
            new Game("塞尔达传说 合集", "The Legend of Zelda · 6 部", "org.dolphinemu.dolphinemu")
                    .cover("zelda_collection").collection(
                            new Game("塞尔达传说 风之杖", "The Wind Waker · GameCube", "org.dolphinemu.dolphinemu")
                                    .cover("zelda_ww").dolphin("/storage/emulated/0/ROMs/gc/Zelda_Wind_Waker.gcz"),
                            new Game("塞尔达传说 众神的三角力量", "A Link to the Past & Four Swords · GBA", RETROARCH)
                                    .cover("zelda_alttp").retroarch("/storage/emulated/0/ROMs/gba/Zelda_ALttP_Four_Swords.gba", "mgba"),
                            new Game("塞尔达传说 缩小帽", "The Minish Cap · GBA", RETROARCH)
                                    .cover("zelda_minish").retroarch("/storage/emulated/0/ROMs/gba/Zelda_Minish_Cap.gba", "mgba"),
                            new Game("塞尔达传说 织梦岛DX", "Link's Awakening DX · GBC", RETROARCH)
                                    .cover("zelda_la").retroarch("/storage/emulated/0/ROMs/gbc/Zelda_Links_Awakening_DX.gbc", "mgba"),
                            new Game("塞尔达传说 时空之章", "Oracle of Ages · GBC", RETROARCH)
                                    .cover("zelda_ages").retroarch("/storage/emulated/0/ROMs/gbc/Zelda_Oracle_of_Ages.gbc", "mgba"),
                            new Game("塞尔达传说 大地之章", "Oracle of Seasons · GBC", RETROARCH)
                                    .cover("zelda_seasons").retroarch("/storage/emulated/0/ROMs/gbc/Zelda_Oracle_of_Seasons.gbc", "mgba")),
            new Game("RetroArch", "模拟器", RETROARCH),
    };

    private static Game neogeo(String title, String subtitle, String set) {
        return new Game(title, subtitle + " · Neo Geo", RETROARCH)
                .cover(set).retroarch("/storage/emulated/0/ROMs/arcade/" + set + ".zip", "fbneo");
    }
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private Game[] items;
    private int collection;
    private View[] cards;
    private Bitmap[] covers;
    private HorizontalScrollView scroller;
    private ImageView bgA, bgB;
    private TextView title, subtitle, battery;
    private SharedPreferences prefs;
    private int focused;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100
                    / Math.max(1, i.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
            boolean charging = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            battery.setText((charging ? "⚡ " : "") + level + "%");
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        collection = getIntent().getIntExtra(EXTRA_COLLECTION, -1);
        items = collection >= 0 ? GAMES[collection].children : GAMES;
        cards = new View[items.length];
        covers = new Bitmap[items.length];
        prefs = getSharedPreferences("nova", MODE_PRIVATE);
        focused = Math.min(prefs.getInt(focusKey(), 0), items.length - 1);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0B0B10);
        bgA = blurred(dp(36));
        bgB = blurred(dp(36));
        bgA.setAlpha(0f);
        bgB.setAlpha(0f);
        root.addView(bgA, MATCH, MATCH);
        root.addView(bgB, MATCH, MATCH);
        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xAA000000, 0x66000000, 0xEE000000}));
        root.addView(scrim, MATCH, MATCH);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(28), dp(16), dp(28), dp(20));
        column.setClipChildren(false);
        root.addView(column, MATCH, MATCH);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text(collection >= 0 ? GAMES[collection].title : "NOVA", 13, 0x99FFFFFF, true);
        brand.setLetterSpacing(0.35f);
        top.addView(brand, new LinearLayout.LayoutParams(0, WRAP, 1));
        battery = text("", 14, 0xCCFFFFFF, false);
        top.addView(battery);
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(16);
        clock.setTextColor(0xFFFFFFFF);
        clock.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        clockLp.leftMargin = dp(16);
        top.addView(clock, clockLp);
        column.addView(top, MATCH, dp(28));

        scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setFillViewport(true);
        scroller.setClipChildren(false);
        scroller.setClipToPadding(false);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setClipChildren(false);
        int[] size = cardSize();
        for (int i = 0; i < items.length; i++) {
            cards[i] = card(i, size[0], size[1]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size[0], size[1]);
            lp.leftMargin = lp.rightMargin = dp(10);
            row.addView(cards[i], lp);
        }
        scroller.addView(row, new FrameLayout.LayoutParams(WRAP, MATCH));
        column.addView(scroller, new LinearLayout.LayoutParams(MATCH, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.BOTTOM);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        title = text("", 26, 0xFFFFFFFF, true);
        subtitle = text("", 13, 0x99FFFFFF, false);
        titles.addView(title);
        titles.addView(subtitle);
        bottom.addView(titles, new LinearLayout.LayoutParams(0, WRAP, 1));
        bottom.addView(text(collection >= 0 ? "Ⓐ 开始    Ⓑ 返回" : "Ⓐ 开始    Ⓨ 应用", 13, 0x99FFFFFF, false));
        column.addView(bottom, MATCH, dp(64));

        setContentView(root);
        showFocus(focused);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent sticky = registerReceiver(batteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_EXPORTED);
        if (sticky != null) batteryReceiver.onReceive(this, sticky);
        // Also lands focus on this card when leaving touch mode on the first D-pad press.
        for (View c : cards) c.setFocusedByDefault(c == cards[focused]);
        cards[focused].requestFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryReceiver);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        // Act on the highlighted card even when the window hasn't settled view focus yet.
        if (code == KeyEvent.KEYCODE_BUTTON_A || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            if (e.getAction() == KeyEvent.ACTION_UP) launch(focused, cards[focused]);
            return true;
        }
        if (code == KeyEvent.KEYCODE_BUTTON_Y) {
            if (e.getAction() == KeyEvent.ACTION_UP) startActivity(new Intent(this, AppsActivity.class));
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    // On the home screen B has nowhere to go back to.
    @Override
    void onGamepadBack() {
        if (collection >= 0) finish();
    }

    private String focusKey() {
        return "focused" + collection;
    }

    private int[] cardSize() {
        DisplayMetrics m = getResources().getDisplayMetrics();
        int w = (m.widthPixels - dp(56) - dp(20) * 3) / 3;
        int maxH = m.heightPixels - dp(36) - dp(28) - dp(64) - dp(56);
        int h = Math.min(w * 3 / 2, maxH);
        return new int[]{h * 2 / 3, h};
    }

    private View card(int i, int w, int h) {
        Game g = items[i];
        boolean installed = getPackageManager().getLaunchIntentForPackage(g.pkg) != null;
        int radius = dp(14);

        FrameLayout card = new FrameLayout(this);
        card.setFocusable(true);
        card.setClickable(true);
        card.setDefaultFocusHighlightEnabled(false);
        card.setBackgroundColor(0xFF1C1C24);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        card.setClipToOutline(true);
        card.setElevation(dp(6));
        card.setAlpha(0.6f);

        Bitmap cover = loadCover(g.cover);
        boolean isIcon = cover == null;
        if (isIcon) cover = appIcon(g.pkg);
        covers[i] = cover;

        ImageView img = new ImageView(this);
        if (cover != null) {
            img.setImageBitmap(cover);
            float ratio = (float) cover.getWidth() / cover.getHeight();
            if (isIcon || Math.abs(ratio - (float) w / h) > 0.1f) {
                // Fill the letterbox with a blurred, dimmed copy of the same image.
                ImageView backdrop = blurred(dp(24));
                backdrop.setImageBitmap(cover);
                backdrop.setColorFilter(0x66000000);
                card.addView(backdrop, MATCH, MATCH);
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                if (isIcon) img.setPadding(w / 5, w / 5, w / 5, w / 5);
            } else {
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        }
        card.addView(img, MATCH, MATCH);

        if (!installed) {
            ColorMatrix gray = new ColorMatrix();
            gray.setSaturation(0);
            img.setColorFilter(new ColorMatrixColorFilter(gray));
            img.setImageAlpha(110);
            TextView badge = text("未安装", 13, 0xFFFFFFFF, true);
            card.addView(badge, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));
        }

        GradientDrawable border = new GradientDrawable();
        border.setColor(0);
        border.setCornerRadius(radius);
        border.setStroke(dp(3), 0xFFFFFFFF);
        card.setTag(border);
        card.setOnFocusChangeListener((v, has) -> {
            if (has) showFocus(i);
        });
        card.setOnClickListener(v -> launch(i, v));
        return card;
    }

    private void showFocus(int i) {
        focused = i;
        prefs.edit().putInt(focusKey(), i).apply();
        // Highlight by selection, not focus, so it also shows in touch mode.
        for (View c : cards) {
            boolean on = c == cards[i];
            c.setForeground(on ? (Drawable) c.getTag() : null);
            c.animate().scaleX(on ? 1.08f : 1f).scaleY(on ? 1.08f : 1f)
                    .alpha(on ? 1f : 0.6f).translationZ(on ? dp(12) : 0).setDuration(160).start();
        }
        View c = cards[i];
        scroller.post(() -> scroller.smoothScrollTo(
                ((View) c.getParent()).getLeft() + c.getLeft() + c.getWidth() / 2 - scroller.getWidth() / 2, 0));
        title.setText(items[i].title);
        subtitle.setText(items[i].subtitle);
        ImageView in = bgA.getAlpha() > 0.5f ? bgB : bgA;
        ImageView out = in == bgA ? bgB : bgA;
        in.setImageBitmap(covers[i]);
        in.animate().alpha(1f).setDuration(350).start();
        out.animate().alpha(0f).setDuration(350).start();
    }

    private Intent intent(Game g) {
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(g.pkg);
        if (launch == null || g.activity == null) return launch;
        Intent i = new Intent(Intent.ACTION_MAIN).setClassName(g.pkg, g.activity)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        for (int k = 0; k < g.extras.length; k += 2) i.putExtra(g.extras[k], g.extras[k + 1]);
        if (i.hasExtra("ROM")) {
            // RetroArch restores this IME when it exits.
            i.putExtra("IME", Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD));
        }
        return i;
    }

    private void launch(int i, View v) {
        Intent intent = items[i].children != null
                ? new Intent(this, CollectionActivity.class).putExtra(EXTRA_COLLECTION, i)
                : intent(items[i]);
        if (intent == null) {
            Toast.makeText(this, items[i].title + " 未安装", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent, ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight()).toBundle());
    }

    private ImageView blurred(float radius) {
        ImageView v = new ImageView(this);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        v.setScaleX(1.2f);
        v.setScaleY(1.2f);
        v.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        return v;
    }

    private Bitmap loadCover(String name) {
        for (String ext : new String[]{".jpg", ".png"}) {
            try (InputStream in = getAssets().open("covers/" + name + ext)) {
                return BitmapFactory.decodeStream(in);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private Bitmap appIcon(String pkg) {
        Drawable d;
        try {
            d = getPackageManager().getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        int size = dp(128);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, size, size);
        d.draw(new Canvas(b));
        return b;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setSingleLine(true);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }
}
