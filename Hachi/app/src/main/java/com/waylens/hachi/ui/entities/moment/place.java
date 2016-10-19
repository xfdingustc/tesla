package com.waylens.hachi.ui.entities.moment;

import android.text.TextUtils;

/**
 * Created by Xiaofei on 2016/9/13.
 */
public class Place {
    public String country;
    public String region;
    public String city;

    @Override
    public String toString() {
        if (!TextUtils.isEmpty(country) && !TextUtils.isEmpty(city)) {
            return city + ", " + country;
        } else {
            return "";
        }
    }
}
