package com.rest.response;

import com.waylens.hachi.utils.ToStringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/6/12.
 */
public class MomentPlayInfo {

    public long momentID;

    public String captureTime;

    public int duration;

    public String videoUrl;

    public List<RawDataUrl> rawDataUrl;


    public static class RawDataUrl {
        public String captureTime;

        public int duration;

        public String url;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
