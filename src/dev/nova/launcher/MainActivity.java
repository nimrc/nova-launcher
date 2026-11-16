package dev.nova.launcher;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.graphics.Bitmap;
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

public class MainActivity extends Base {
    static final String EXTRA_COLLECTION = "collection";
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
    private boolean batteryRegistered;

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
        Game[] games = Config.games(this);
        if (collection >= games.length || (collection >= 0 && games[collection].children == null)) {
            finish();
            return;
        }
        items = collection >= 0 ? games[collection].children : games;
        cards = new View[items.length];
        covers = new Bitmap[items.length];
        prefs = getSharedPreferences("nova", MODE_PRIVATE);
        focused = Math.max(0, Math.min(prefs.getInt(focusKey(), 0), items.length - 1));
        if (collection < 0) {
            if (Config.error != null) Toast.makeText(this, Config.error, Toast.LENGTH_LONG).show();
            // Ask once for all-files access, needed to read /sdcard/Nova.
            if (!Config.canRead() && !prefs.getBoolean("askedStorage", false)) {
                prefs.edit().putBoolean("askedStorage", true).apply();
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.fromParts("package", getPackageName(), null)));
            }
        }

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
        TextView brand = text(collection >= 0 ? games[collection].title : "NOVA", 13, 0x99FFFFFF, true);
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
        if (items.length == 0) {
            TextView empty = text("没有游戏 — 编辑 " + Config.FILE.getPath(), 15, 0x99FFFFFF, false);
            empty.setGravity(Gravity.CENTER);
            column.addView(empty, new LinearLayout.LayoutParams(MATCH, 0, 1));
        } else {
            column.addView(scroller, new LinearLayout.LayoutParams(MATCH, 0, 1));
        }

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
        if (items.length > 0) showFocus(focused);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (items == null) return;
        if (Config.stale()) {
            // games.json or storage access changed; a collection index may no longer be valid.
            if (collection >= 0) finish();
            else recreate();
            return;
        }
        batteryRegistered = true;
        Intent sticky = registerReceiver(batteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_EXPORTED);
        if (sticky != null) batteryReceiver.onReceive(this, sticky);
        // Also lands focus on this card when leaving touch mode on the first D-pad press.
        if (cards.length == 0) return;
        for (View c : cards) c.setFocusedByDefault(c == cards[focused]);
        cards[focused].requestFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (batteryRegistered) unregisterReceiver(batteryReceiver);
        batteryRegistered = false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        // Act on the highlighted card even when the window hasn't settled view focus yet.
        if (code == KeyEvent.KEYCODE_BUTTON_A || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            if (e.getAction() == KeyEvent.ACTION_UP && cards != null && cards.length > 0) launch(focused, cards[focused]);
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
        boolean installed = g.pkg == null || getPackageManager().getLaunchIntentForPackage(g.pkg) != null;
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

        Bitmap cover = Config.cover(g.cover);
        boolean isIcon = cover == null;
        if (isIcon && g.pkg != null) cover = appIcon(g.pkg);
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
        g.extras.forEach(i::putExtra);
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
