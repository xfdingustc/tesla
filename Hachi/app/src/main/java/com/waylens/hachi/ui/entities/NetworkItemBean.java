package com.waylens.hachi.ui.entities;

import com.waylens.hachi.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/3/21.
 */
public class NetworkItemBean {
    public String ssid;
    public String bssid;
    public String flags;
    public int frequency;
    public int singalLevel;
    public boolean added;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
