package com.waylens.hachi.views.dashboard.adapters;

import com.waylens.hachi.vdb.RawDataBlock;


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

    @Override
    public RawDataBlock.RawDataItem getAccDataItem(long pts) {
        return getRawDataItem(mAccDataBlock, pts);
    }

    @Override
    public RawDataBlock.RawDataItem getObdDataItem(long pts) {
        return getRawDataItem(mObdDataBlock, pts);
    }

    @Override
    public RawDataBlock.RawDataItem getGpsDataItem(long pts) {
        return getRawDataItem(mGpsDataBlock, pts);
    }

    // TODO: We need refine this algorithm:
    private RawDataBlock.RawDataItem getRawDataItem(RawDataBlock datablock, long pts) {
        if (datablock == null) {
            return null;
        }

        for (int i = 0; i < datablock.header.mNumItems; i++) {
            RawDataBlock.RawDataItem item = datablock.getRawDataItem(i);
            if (item.clipTimeMs > pts) {
                return item;
            }
        }
        return null;
    }
}
