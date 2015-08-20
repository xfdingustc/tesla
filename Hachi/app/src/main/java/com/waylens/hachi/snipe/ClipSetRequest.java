package com.waylens.hachi.snipe;

import android.graphics.SweepGradient;
import android.widget.Switch;

import com.transee.vdb.ClipSet;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class ClipSetRequest extends VdbRequest<ClipSet> {
    private final VdbResponse.Listener<ClipSet> mListener;

    public static final int METHOD_GET = 0;
    public static final int METHOD_SET = 1;

    public ClipSetRequest(int method, VdbResponse.Listener<ClipSet> listener, VdbResponse
        .ErrorListener errorListener) {
        super(method, errorListener);
        mListener = listener;
    }

    @Override
    protected VdbCommand getVdbCommand() {
        switch (mMethod) {
            case METHOD_GET:
                return VdbCommand.Factory.createCmdGetClipSetInfo(0);
            case METHOD_SET:
                return null;
            default:
                return null;
        }


    }

    @Override
    protected VdbResponse<ClipSet> parseVdbResponse(RawResponse response) {
        return null;
    }


}
