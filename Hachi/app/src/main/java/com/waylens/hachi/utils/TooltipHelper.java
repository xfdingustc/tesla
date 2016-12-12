package com.waylens.hachi.utils;

/**
 * Created by Xiaofei on 2016/11/28.
 */

public class TooltipHelper {
    private static final String PREFERENCE_EXPORT_SHOWED = "export_showed";
    private static final String PREFERENCE_REFRESH_LEADERBOARD_SHOWD = "refresh_leaderboard_showed";

    public static boolean shouldShowExportTapTarget() {
        return !PreferenceUtils.getBoolean(PREFERENCE_EXPORT_SHOWED, false);
    }

    public static boolean shouldShowLeaderboardRefresh() {
        return !PreferenceUtils.getBoolean(PREFERENCE_REFRESH_LEADERBOARD_SHOWD, false);
    }

    public static void onShowExportTargetTaped() {
        PreferenceUtils.putBoolean(PREFERENCE_EXPORT_SHOWED, true);
    }

    public static void onShowLeaderboardRefreshTaped() {
        PreferenceUtils.putBoolean(PREFERENCE_REFRESH_LEADERBOARD_SHOWD, true);
    }
}
