package com.waylens.hachi.vdb;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/9/11.
 */
public class RawDataBlock {
    public static class RawDataItem {
        public static final int RAW_DATA_NULL = 0;
        public static final int RAW_DATA_GPS = 1;
        public static final int RAW_DATA_ACC = 2;
        public static final int RAW_DATA_ODB = 3;

        private final int mType;
        private final long mPtsMs;
        public Object object; // GPSRawData for RAW_DATA_GPS

        public RawDataItem(int type, long ptsMs) {
            this.mType = type;
            this.mPtsMs = ptsMs;
        }

        public int getType() {
            return mType;
        }

        public long getPtsMs() {
            return mPtsMs;
        }
    }

    public static final byte F_RAW_DATA_GPS = (1 << RawDataItem.RAW_DATA_GPS);
    public static final byte F_RAW_DATA_ACC = (1 << RawDataItem.RAW_DATA_ACC);
    public static final byte F_RAW_DATA_ODB = (1 << RawDataItem.RAW_DATA_ODB);

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

    public final RawDataBlockHeader header;
    public int[] timeOffsetMs;
    public int[] dataSize;
    public byte[] data;

    private List<RawDataItem> mRawDataItems = new ArrayList<>();

    public RawDataBlock(RawDataBlockHeader header) {
        this.header = header;
    }

    public RawDataItem getRawDataItem(int index) {
        return mRawDataItems.get(index);
    }

    public void addRawDataItem(RawDataItem item) {
        mRawDataItems.add(item);
    }
}
