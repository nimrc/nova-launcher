package dev.nova.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

abstract class Base extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setDecorFitsSystemWindows(false);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            WindowInsetsController c = getWindow().getInsetsController();
            c.hide(WindowInsets.Type.systemBars());
            c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    // Plain views only treat DPAD_CENTER/ENTER as confirm, not gamepad A.
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_BUTTON_A:
                return super.dispatchKeyEvent(new KeyEvent(e.getDownTime(), e.getEventTime(), e.getAction(),
                        KeyEvent.KEYCODE_DPAD_CENTER, e.getRepeatCount(), e.getMetaState(), e.getDeviceId(),
                        e.getScanCode(), e.getFlags(), e.getSource()));
            case KeyEvent.KEYCODE_BUTTON_B:
                if (e.getAction() == KeyEvent.ACTION_UP) onGamepadBack();
                return true;
            default:
                return super.dispatchKeyEvent(e);
        }
    }

    void onGamepadBack() {
        finish();
    }

    int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
