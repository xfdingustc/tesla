package com.waylens.hachi.rest.response;

import android.text.TextUtils;

/**
 * Created by laina on 16/9/13.
 */
public class GeoInfoResponse {
    public String country;
    public String region;
    public String city;
    public String address;

    public String getLocationString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(country)) {
            stringBuilder.append(country).append(", ");
        }
        if (!TextUtils.isEmpty(city)) {
            stringBuilder.append(city);
        }
        return stringBuilder.toString();
    }
}
