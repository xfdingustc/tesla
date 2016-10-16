package com.waylens.hachi.utils;

import android.app.Activity;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import com.waylens.hachi.app.Hachi;

/**
 * Created by Xiaofei on 2016/10/16.
 */

public class StatusBarHelper {
    public static void setStatusBarColor(Activity activity, int color) {
        if (VersionHelper.isGreaterThanLollipop()) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
    }
}
