package com.waylens.hachi.snipe.toolbox;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.RawDataBlock;

/**
 * Created by Richard on 9/22/15.
 */
public class RawDataDumpRequest extends VdbRequest<byte[]> {

    private final VdbResponse.Listener<byte[]> mListener;

    private final Clip mClip;
    private final Long mClipTimeMs;
    private final int mLengthMs;
    private final int mDataType;

    public RawDataDumpRequest(Clip clip, Long clipTimeMs, int lengthMs, int dataType,
                              VdbResponse.Listener<byte[]> listener,
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
    protected VdbResponse<byte[]> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            return null;
        }
        return VdbResponse.success(response.mReceiveBuffer);
    }

    @Override
    protected void deliverResponse(byte[] response) {
        mListener.onResponse(response);
    }
}
