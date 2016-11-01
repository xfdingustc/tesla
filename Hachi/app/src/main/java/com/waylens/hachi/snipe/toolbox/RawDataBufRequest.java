package com.waylens.hachi.snipe.toolbox;

import android.os.Bundle;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;


/**
 * Created by lshw on 2015/9/11.
 */
public class RawDataBufRequest extends VdbRequest<byte[]> {
    private static final String TAG = RawDataBlockRequest.class.getSimpleName();
    private final Clip.ID mCid;
    private final int mDataType;
    private final long mClipTimeMs;
    private final int mDuration;

    public static final String PARAM_CLIP_TIME = "clip.time.ms";
    public static final String PARAM_CLIP_LENGTH = "clip.length.ms";
    public static final String PARAM_DATA_TYPE = "raw.data.type";

    public RawDataBufRequest(Clip.ID cid, Bundle params, VdbResponse.Listener<byte[]> listener,
                               VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        this.mCid = cid;
        this.mDataType = params.getInt(PARAM_DATA_TYPE, RawDataItem.DATA_TYPE_NONE);
        mClipTimeMs = params.getLong(PARAM_CLIP_TIME, 0);
        mDuration = params.getInt(PARAM_CLIP_LENGTH, 0);
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetRawDataBlock(mCid, true, mDataType, mClipTimeMs, mDuration);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<byte[]> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
//            Logger.t(TAG).d("response: " + response.getRetCode());
            return null;
        }
        int bufSize = response.mReceiveBuffer.length - response.getMsgIndex();
        byte[] retBuf = response.readByteArray(bufSize);
        return VdbResponse.success(retBuf);
    }
}
