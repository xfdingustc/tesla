package com.waylens.hachi.utils;

/**
 * Created by Xiaofei on 2016/10/14.
 */

public class ThemeHelper {
    public static boolean isDarkTheme() {
        String theme = PreferenceUtils.getString(PreferenceUtils.APP_THEME, "dark");
        return theme.equals("dark");
    }
}
