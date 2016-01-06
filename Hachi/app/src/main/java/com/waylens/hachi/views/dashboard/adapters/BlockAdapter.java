package com.waylens.hachi.views.dashboard.adapters;

import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;


/**
 * Created by Xiaofei on 2015/12/18.
 */
public class BlockAdapter extends RawDataAdapter {
    private RawDataBlock mAccDataBlock;
    private RawDataBlock mObdDataBlock;
    private RawDataBlock mGpsDataBlock;

    public void setAccDataBlock(RawDataBlock accDataBlock) {
        this.mAccDataBlock = accDataBlock;
    }

    public void setObdDataBlock(RawDataBlock obdDataBlock) {
        this.mObdDataBlock = obdDataBlock;
    }

    public void setGpsDataBlock(RawDataBlock gpsDataBlock) {
        this.mGpsDataBlock = gpsDataBlock;
    }



    // TODO: We need refine this algorithm:
    private RawDataItem getRawDataItem(RawDataBlock datablock, long pts) {
        if (datablock == null) {
            return null;
        }

        for (int i = 0; i < datablock.header.mNumItems; i++) {
            RawDataItem item = datablock.getRawDataItem(i);
            if (item.getPtsMs() > pts) {
                return item;
            }
        }
        return null;
    }
}
