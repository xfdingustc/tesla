package com.waylens.hachi.snipe;

import com.transee.vdb.ClipSet;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class ClipSetRequest extends VdbRequest<ClipSet> {
    private final VdbResponse.Listener<ClipSet> mListener;

    public interface Method {
        int GET = 0;
    }



    public ClipSetRequest(int method, VdbResponse.Listener<ClipSet> listener, VdbResponse
        .ErrorListener errorListener) {
        super(method, errorListener);
        mListener = listener;
    }

    @Override
    protected VdbResponse<ClipSet> parseVdbResponse(RawResponse response) {
        return null;
    }


}
