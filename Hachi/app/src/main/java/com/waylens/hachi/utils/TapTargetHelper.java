package com.waylens.hachi.utils;

/**
 * Created by Xiaofei on 2016/11/28.
 */

public class TapTargetHelper {
    private static final String PREFERENCE_EXPORT_SHOWED  =  "export_showed";
    public static boolean shouldShowExportTapTarget() {
        return !PreferenceUtils.getBoolean(PREFERENCE_EXPORT_SHOWED, false);
    }

    public static void onShowExportTargetTaped() {
        PreferenceUtils.putBoolean(PREFERENCE_EXPORT_SHOWED, true);
    }
}
