package com.waylens.hachi.smartremix;

import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.rawdata.RawData;

import java.io.File;

/**
 * Created by Xiaofei on 2016/4/15.
 */
public interface RawDataCache {
    File get(Clip.ID clipId);

    boolean save(Clip.ID clipId, RawData rawData);
}
