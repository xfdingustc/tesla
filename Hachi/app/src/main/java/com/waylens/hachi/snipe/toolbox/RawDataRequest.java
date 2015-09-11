package com.waylens.hachi.snipe.toolbox;

import android.os.Bundle;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.RawData;

/**
 * Created by Xiaofei on 2015/9/11.
 */
public class RawDataRequest extends VdbRequest<RawData> {
    private final VdbResponse<RawData> mListener;
    private final Bundle mParameters;
    public static final int METHOD_GET = 0;
    public static final int METHOD_SET = 1;

    public RawDataRequest(int method, Bundle parameters, VdbResponse<RawData> listener,
                          VdbResponse.ErrorListener errorListener) {
        super(method, errorListener);
        this.mListener = listener;
        this.mParameters = parameters;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        switch (mMethod) {
            case METHOD_GET:
                //mVdbCommand = VdbCommand.Factory.createCmdGetClipSetInfo(type);
                mVdbCommand = null;
        }

        return mVdbCommand;
    }

    @Override
    protected VdbResponse<RawData> parseVdbResponse(VdbAcknowledge response) {
        return null;
    }

    @Override
    protected void deliverResponse(RawData response) {

    }
}
