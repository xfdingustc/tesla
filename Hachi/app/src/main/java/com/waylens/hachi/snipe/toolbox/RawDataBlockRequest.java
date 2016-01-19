package com.waylens.hachi.snipe.toolbox;

import android.os.Bundle;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

/**
 * Created by Xiaofei on 2015/9/11.
 */
public class RawDataBlockRequest extends VdbRequest<RawDataBlock> {
    private static final String TAG = RawDataBlockRequest.class.getSimpleName();
    private final Clip.ID mCid;
    private final int mDataType;
    private final long mClipTimeMs;
    private final int mDuration;

    public static final String PARAM_CLIP_TIME = "clip.time.ms";
    public static final String PARAM_CLIP_LENGTH = "clip.length.ms";
    public static final String PARAM_DATA_TYPE = "raw.data.type";

    public RawDataBlockRequest(Clip.ID cid, Bundle params,
                               VdbResponse.Listener<RawDataBlock> listener,
                               VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        this.mCid = cid;
        this.mDataType = params.getInt(PARAM_DATA_TYPE, RawDataItem.DATA_TYPE_UNKNOWN);
        mClipTimeMs = params.getLong(PARAM_CLIP_TIME, 0);
        mDuration = params.getInt(PARAM_CLIP_LENGTH, 0);
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetRawDataBlock(mCid, true, mDataType, mClipTimeMs, mDuration);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<RawDataBlock> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).d("response: " + response.getRetCode());
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
            RawDataItem item = new RawDataItem(header.mDataType, block.timeOffsetMs[i] + header.mRequestedTimeMs);

            byte[] data = response.readByteArray(block.dataSize[i]);
            if (header.mDataType == RawDataItem.DATA_TYPE_OBD) {
                item.data = RawDataItem.OBDData.fromBinary(data);
            } else if (header.mDataType == RawDataItem.DATA_TYPE_ACC) {
                item.data = RawDataItem.AccData.fromBinary(data);
            } else if (header.mDataType == RawDataItem.DATA_TYPE_GPS) {
                item.data = RawDataItem.GpsData.fromBinary(data);
            }

            block.addRawDataItem(item);
        }


        return VdbResponse.success(block);
    }
}
