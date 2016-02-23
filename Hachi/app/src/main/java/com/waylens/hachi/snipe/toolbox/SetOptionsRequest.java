package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;

/**
 * Created by Richard on 2/22/16.
 */
public class SetOptionsRequest extends VdbRequest<Integer> {
    public static final int VDB_OPTION_NONE = 0;
    public static final int VDB_OPTION_HLS_SEGMENT_LENGTH = 1;
    private static final String TAG = "SetOptionsRequest";

    int mOption = 0;
    int mHlsSegmentLength = 1000;

    public SetOptionsRequest(VdbResponse.Listener<Integer> listener,
                             VdbResponse.ErrorListener errorListener,
                             int option,
                             int hlsSegmentLength) {
        super(0, listener, errorListener);
        mOption = option;
        mHlsSegmentLength = hlsSegmentLength;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdSetOptions(mOption, mHlsSegmentLength);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<Integer> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("ackGetClipSetInfo: failed");
            return null;
        }
        return VdbResponse.success(response.readi32());
    }
}
