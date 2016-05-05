package com.waylens.hachi.vdb;

import com.waylens.hachi.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/5/5.
 */
public class SpaceInfo {
    public long total;
    public long used;
    public long marked;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
