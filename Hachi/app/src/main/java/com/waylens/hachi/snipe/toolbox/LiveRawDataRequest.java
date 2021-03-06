package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;


/**
 * Created by Xiaofei on 2015/12/21.
 */
public class LiveRawDataRequest extends VdbRequest<Integer> {
    private static final String TAG = LiveRawDataRequest.class.getSimpleName();
    private final int mDataType;

    public LiveRawDataRequest(int dataType,
                              VdbResponse.Listener<Integer> listener,
                              VdbResponse.ErrorListener errorListener
                              ) {
        super(0, listener, errorListener);
        this.mDataType = dataType;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdSetRawDataOption(mDataType);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<Integer> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).d("response: " + response.getRetCode());
            return null;
        }
        return VdbResponse.success(mDataType);
    }
}
