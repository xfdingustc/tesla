package com.waylens.hachi.snipe.toolbox;


import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.vdb.Clip;

/**
 * Created by Richard on 9/17/15.
 */
public class ClipExtentUpdateRequest extends VdbRequest<Integer> {

    private Clip.ID mCid;
    private long mClipStart;
    private long mClipEnd;

    public ClipExtentUpdateRequest(Clip.ID cid, long newClipStart, long newClipEnd,
                                   VdbResponse.Listener<Integer> listener,
                                   VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        mCid = cid;
        mClipStart = newClipStart;
        mClipEnd = newClipEnd;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdSetClipExtent(mCid, mClipStart, mClipEnd);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<Integer> parseVdbResponse(VdbAcknowledge response) {
        return VdbResponse.success(response.getRetCode());
    }
}
