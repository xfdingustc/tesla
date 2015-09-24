package com.waylens.hachi.snipe.toolbox;

import com.transee.common.GPSRawData;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.AccData;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

/**
 * Created by Xiaofei on 2015/9/11.
 */
public class RawDataBlockRequest extends VdbRequest<RawDataBlock> {
    private static final String TAG = RawDataBlockRequest.class.getSimpleName();
    private final VdbResponse.Listener<RawDataBlock> mListener;

    private final Clip mClip;
    private final Long mClipTimeMs;
    private final int mLengthMs;
    private final int mDataType;

    public RawDataBlockRequest(Clip clip, Long clipTimeMs, int lengthMs, int dataType,
                               VdbResponse.Listener<RawDataBlock> listener,
                               VdbResponse.ErrorListener errorListener) {
        super(0, errorListener);
        this.mListener = listener;
        this.mClip = clip;
        this.mClipTimeMs = clipTimeMs;
        this.mLengthMs = lengthMs;
        this.mDataType = dataType;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetRawDataBlock(mClip, false, mClipTimeMs, mLengthMs,
                mDataType);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<RawDataBlock> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            return null;
        }

        int clipType = response.readi32();
        int clipId = response.readi32();
        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, null);
        RawDataBlock.RawDataBlockHeader header = new RawDataBlock.RawDataBlockHeader(cid);
        header.mClipDate = response.readi32();
        header.mDataType = response.readi32();
        header.mRequestedTimeMs = response.readi64();
        header.mNumItems = response.readi32();
        header.mDataSize = response.readi32();


        RawDataBlock block = new RawDataBlock(header);

        int numItems = block.header.mNumItems;
        block.timeOffsetMs = new int[numItems];
        block.dataSize = new int[numItems];

        for (int i = 0; i < numItems; i++) {
            block.timeOffsetMs[i] = response.readi32();
            block.dataSize[i] = response.readi32();
        }


        for (int i = 0; i < numItems; i++) {
            RawDataItem item = new RawDataItem();
            item.dataType = header.mDataType;
            item.clipTimeMs = block.timeOffsetMs[i] + header.mRequestedTimeMs;
            byte[] data = response.readByteArray(block.dataSize[i]);
            if (header.mDataType == RawDataBlock.RAW_DATA_ODB) {
                item.object = OBDData.parse(data);
            } else if (header.mDataType == RawDataBlock.RAW_DATA_ACC) {
                item.object = AccData.parse(data);
            } else if (header.mDataType == RawDataBlock.RAW_DATA_GPS) {
                item.object = GPSRawData.translate(data);
            }

            block.addRawDataItem(item);
        }


        return VdbResponse.success(block);
    }

    @Override
    protected void deliverResponse(RawDataBlock response) {
        mListener.onResponse(response);
    }
}
