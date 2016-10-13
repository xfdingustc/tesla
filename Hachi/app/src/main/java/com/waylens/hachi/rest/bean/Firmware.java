package com.waylens.hachi.rest.bean;

import com.waylens.hachi.snipe.utils.ToStringUtils;

import java.io.Serializable;

/**
 * Created by Xiaofei on 2016/10/13.
 */

public class Firmware implements Serializable {
    public String name;
    public String version;
    public String BSPVersion;
    public String url;
    public long size;
    public String md5;
    public String releaseData;
    public Description description;

    public static class Description implements Serializable {
        public String en;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
