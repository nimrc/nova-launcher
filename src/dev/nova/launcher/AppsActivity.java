package dev.nova.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

public class AppsActivity extends Base {
    private static class App {
        final ActivityInfo info;
        final String label;
        Drawable icon;

        App(ActivityInfo info, String label) {
            this.info = info;
            this.label = label;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        PackageManager pm = getPackageManager();
        List<App> apps = new ArrayList<>();
        for (ResolveInfo r : pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)) {
            if (!r.activityInfo.packageName.equals(getPackageName())) {
                apps.add(new App(r.activityInfo, r.loadLabel(pm).toString()));
            }
        }
        Collator collator = Collator.getInstance();
        apps.sort((a, b) -> collator.compare(a.label, b.label));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0B0B10);
        root.setPadding(dp(28), dp(16), dp(28), 0);

        TextView header = new TextView(this);
        header.setText("所有应用");
        header.setTextSize(20);
        header.setTextColor(0xFFFFFFFF);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(header, ViewGroup.LayoutParams.MATCH_PARENT, dp(44));

        GradientDrawable selector = new GradientDrawable();
        selector.setCornerRadius(dp(12));
        selector.setColor(0x33FFFFFF);

        GridView grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(dp(100));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(dp(8));
        grid.setSelector(selector);
        grid.setClipToPadding(false);
        grid.setPadding(0, 0, 0, dp(20));
        grid.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return apps.size();
            }

            @Override
            public Object getItem(int i) {
                return apps.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View convert, ViewGroup parent) {
                LinearLayout cell = (LinearLayout) convert;
                if (cell == null) {
                    cell = new LinearLayout(AppsActivity.this);
                    cell.setOrientation(LinearLayout.VERTICAL);
                    cell.setGravity(Gravity.CENTER_HORIZONTAL);
                    cell.setPadding(dp(6), dp(10), dp(6), dp(10));
                    cell.addView(new ImageView(AppsActivity.this), dp(56), dp(56));
                    TextView label = new TextView(AppsActivity.this);
                    label.setTextSize(12);
                    label.setTextColor(0xDDFFFFFF);
                    label.setGravity(Gravity.CENTER);
                    label.setSingleLine(true);
                    label.setEllipsize(TextUtils.TruncateAt.END);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = dp(6);
                    cell.addView(label, lp);
                }
                App app = apps.get(i);
                if (app.icon == null) app.icon = app.info.loadIcon(pm);
                ((ImageView) cell.getChildAt(0)).setImageDrawable(app.icon);
                ((TextView) cell.getChildAt(1)).setText(app.label);
                return cell;
            }
        });
        grid.setOnItemClickListener((parent, view, pos, id) -> {
            ActivityInfo info = apps.get(pos).info;
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(new ComponentName(info.packageName, info.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        });
        root.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
        grid.requestFocus();
        grid.setSelection(0);
    }
}
