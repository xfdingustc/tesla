package com.waylens.hachi.utils;

/**
 * Created by Xiaofei on 2016/11/11.
 */

public class SettingHelper {
    private static final String METRIC_UNITS = "metric_units";

    public static boolean isMetricUnit() {
        return PreferenceUtils.getBoolean(SettingHelper.METRIC_UNITS, false);
    }

    public static void setMetricUnit(boolean metric) {
        PreferenceUtils.putBoolean(SettingHelper.METRIC_UNITS, metric);
    }
}
