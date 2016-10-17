package com.waylens.hachi.utils;

import android.text.format.DateUtils;

/**
 * Created by Xiaofei on 2016/10/17.
 */

public class PrettyTimeUtils {
    public static String getTimeAgo(long time) {
        return DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString();
    }
}
