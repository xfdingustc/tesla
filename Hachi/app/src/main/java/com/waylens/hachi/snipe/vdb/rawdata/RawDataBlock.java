package com.waylens.hachi.snipe.vdb.rawdata;


import com.waylens.hachi.snipe.vdb.Clip;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/11.
 */
public class RawDataBlock {

    public static final byte F_RAW_DATA_GPS = (1 << RawDataItem.DATA_TYPE_GPS);
    public static final byte F_RAW_DATA_ACC = (1 << RawDataItem.DATA_TYPE_IIO);
    public static final byte F_RAW_DATA_ODB = (1 << RawDataItem.DATA_TYPE_OBD);

    public final RawDataBlockHeader header;
    public int[] timeOffsetMs;
    public int[] dataSize;
    public byte[] data;

    private int mItemIndex = 0;

    public static class RawDataBlockHeader {
        public final Clip.ID cid;
        public int mClipDate;
        public int mDataType;
        public long mRequestedTimeMs;
        public int mNumItems;
        public int mDataSize;

        public RawDataBlockHeader(Clip.ID cid) {
            this.cid = cid;
        }
    }

    public static class DownloadRawDataBlock {
        public final RawDataBlockHeader header;
        public byte[] ack_data;

        public DownloadRawDataBlock(RawDataBlockHeader header) {
            this.header = header;
        }
    }


    private List<RawDataItem> mRawDataItems = new ArrayList<>();

    public RawDataBlock(RawDataBlockHeader header) {
        this.header = header;
    }

    public List<RawDataItem> getItemList() {
        return mRawDataItems;
    }

    public RawDataItem getRawDataItem(int index) {
        return mRawDataItems.get(index);
    }

    public void addRawDataItem(RawDataItem item) {
        mRawDataItems.add(item);
    }


    public RawDataItem getRawDataItemByTime(long timeMs) {
        int low = 0;
        int high = mRawDataItems.size() - 1;
        int mid, res = -1;
        if (mRawDataItems.get(low).getPtsMs() > timeMs || mRawDataItems.get(high).getPtsMs() < timeMs) {
            return null;
        }
        while (low < high) {
            mid = (low + high) / 2;
            if (mRawDataItems.get(mid).getPtsMs() == timeMs) {
                res = mid;
                break;
            } else if (mRawDataItems.get(mid).getPtsMs() < timeMs) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        RawDataItem updateItem = null;
        if (res != -1) {
            updateItem = new RawDataItem(mRawDataItems.get(res));
        } else {
            updateItem = new RawDataItem(mRawDataItems.get(low));
        }

        if (Math.abs(updateItem.getPtsMs() - timeMs) <= 5000) {
            return updateItem;
        } else {
            return null;
        }
    }
}
