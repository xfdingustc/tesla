package com.waylens.hachi.snipe.toolbox;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.ClipFragment;

/**
 * Created by Richard on 9/22/15.
 */
public class RawDataDumpRequest extends VdbRequest<byte[]> {
    private final ClipFragment mClipFragment;
    private final int mDataType;

    public RawDataDumpRequest(ClipFragment clipFragment, int dataType,
                              VdbResponse.Listener<byte[]> listener,
                              VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        this.mClipFragment = clipFragment;
        this.mDataType = dataType;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetRawDataBlock(mClipFragment, false, mDataType);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<byte[]> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            return null;
        }
        return VdbResponse.success(response.mReceiveBuffer);
    }
}
