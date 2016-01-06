package com.waylens.hachi.vdb;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/11.
 */
public class RawData {

    public final Clip.ID cid;
    public int clipDate;
    public List<RawDataItem> items = new ArrayList<>();

    public RawData(Clip.ID cid) {
        this.cid = cid;
    }
}
