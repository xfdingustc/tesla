package com.waylens.hachi.smartremix;

import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.rawdata.RawData;

import java.io.File;

/**
 * Created by Xiaofei on 2016/4/15.
 */
public class UnlimitedDiskCache implements RawDataCache {

    public UnlimitedDiskCache(File cacheDir) {

    }

    @Override
    public File get(Clip.ID clipId) {
        return null;
    }

    @Override
    public boolean save(Clip.ID clipId, RawData rawData) {
        return false;
    }
}
