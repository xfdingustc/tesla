package com.waylens.hachi.rest.bean;

import android.text.TextUtils;

import com.waylens.hachi.utils.StringUtils;

/**
 * Created by Xiaofei on 2016/10/18.
 */

public class MomentTimingInfo {
    public long t1;
    public long t2;
    public long t3_1;
    public long t4_1;
    public long t5_1;
    public long t6_1;
    public long t3_2;
    public long t4_2;
    public long t5_2;
    public long t6_2;

    public long getRaceTime030(String momentType) {
        if (TextUtils.isEmpty(momentType)) {
            return -1;
        }

        if (momentType.startsWith("RACING_AU")) {
            return t3_2;
        } else {
            return t3_1;
        }
    }

    public long getRaceTime060(String momentType) {
        if (TextUtils.isEmpty(momentType)) {
            return -1;
        }

        if (momentType.startsWith("RACING_AU")) {
            return t5_2;
        } else {
            return t5_1;
        }
    }

    public String getRaceType(String momentType) {
        if (TextUtils.isEmpty(momentType)) {
            return null;
        }

        if (momentType.equals("RACING_AU6T") || momentType.equals("RACING_CD6T")) {
            return "60";
        } else {
            return "30";
        }
    }

    public String getRaceTime(String momentType) {
        if (TextUtils.isEmpty(momentType)) {
            return null;
        }

        if (momentType.equals("RACING_AU6T") || momentType.equals("RACING_CD6T")) {
            return StringUtils.getRaceTime(getRaceTime060(momentType));
        } else {
            return StringUtils.getRaceTime(getRaceTime030(momentType));
        }
    }




}
