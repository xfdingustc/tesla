package com.waylens.hachi.snipe.toolbox;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;

/**
 * Created by Richard on 9/17/15.
 */
public class ClipExtentUpdateRequest extends VdbRequest<Integer> {

    private Clip mClip;
    private long mClipStart;
    private long mClipEnd;

    public ClipExtentUpdateRequest(Clip clip,
                                   long newClipStart,
                                   long newClipEnd,
                                   VdbResponse.Listener<Integer> listener,
                                   VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        mClip = clip;
        mClipStart = newClipStart;
        mClipEnd = newClipEnd;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdSetClipExtent(mClip, mClipStart, mClipEnd);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<Integer> parseVdbResponse(VdbAcknowledge response) {
        return VdbResponse.success(response.getRetCode());
    }
}
