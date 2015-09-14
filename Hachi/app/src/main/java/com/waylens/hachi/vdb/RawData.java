package com.waylens.hachi.vdb;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/11.
 */
public class RawData {

    public static class RawDataItem {
        public int dataType;
        public long clipTimeMs;
        public Object object; // GPSRawData for RAW_DATA_GPS
    }

    public final Clip.ID cid;
    public int clipDate;
    public List<RawDataItem> items = new ArrayList<>();

    public RawData(Clip.ID cid) {
        this.cid = cid;
    }
}
