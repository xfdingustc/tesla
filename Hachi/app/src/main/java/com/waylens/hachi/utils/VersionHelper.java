package com.waylens.hachi.utils;

import android.os.Build;

/**
 * Created by Xiaofei on 2016/10/13.
 */

public class VersionHelper {
    public static boolean isGreaterThanLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isGreaterThanKitkat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }
}
