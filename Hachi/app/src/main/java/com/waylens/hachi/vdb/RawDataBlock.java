package com.waylens.hachi.vdb;

/**
 * Created by Xiaofei on 2015/9/11.
 */
public class RawDataBlock {

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

    public RawDataBlock(RawDataBlockHeader header) {
        this.header = header;
    }
}
