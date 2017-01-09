package com.waylens.hachi.utils;

/**
 * Created by Xiaofei on 2016/12/8.
 */

public class DebugHelper {
    private static final String DEBUG_MODE = "debug.mode";
    private static final String SHOW_MOMENT_SOURCE = "show.moment.source";
    private static final String SHOW_LAP_TIMER = "show.lap.timer";

    public static boolean isInDebugMode() {
        return PreferenceUtils.getBoolean(DEBUG_MODE, false);
    }

    public static void setDebugMode(boolean debugMode) {
        PreferenceUtils.putBoolean(DEBUG_MODE, debugMode);
    }

    public static boolean showMomentSource() {
        return PreferenceUtils.getBoolean(SHOW_MOMENT_SOURCE, false);
    }

    public static void setShowMomentSource(boolean show) {
        PreferenceUtils.putBoolean(SHOW_MOMENT_SOURCE, show);
    }

    public static boolean showLapTimer() {
        return  PreferenceUtils.getBoolean(SHOW_LAP_TIMER, false);
    }

    public static void setShowLapTimer(boolean show) {
        PreferenceUtils.putBoolean(SHOW_LAP_TIMER, show);
    }
}
