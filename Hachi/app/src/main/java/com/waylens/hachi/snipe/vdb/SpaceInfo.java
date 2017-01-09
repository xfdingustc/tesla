package com.waylens.hachi.snipe.vdb;


import com.waylens.hachi.snipe.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/5/5.
 */
public class SpaceInfo {
    public long total;
    public long used;
    public long marked;
    public long clip;

    public long getLoopedSpace() {
        return (clip - marked) + (total - used);
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
