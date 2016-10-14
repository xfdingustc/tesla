package com.waylens.hachi.rest.bean;

import com.waylens.hachi.snipe.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/10/14.
 */

public class MomentSimple {
    public long momentID;
    public String title;
    public String provider;
    public String videoThumbnail;
    public String videoID;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
