package com.waylens.hachi.snipe.toolbox;


import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.vdb.Clip;


public class ClipDeleteRequest extends VdbRequest<Integer> {
    private static final String TAG = "ClipDeleteRequest";
    Clip.ID mCid;

    public ClipDeleteRequest(Clip.ID cid, VdbResponse.Listener<Integer> listener,
                             VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        mCid = cid;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdClipDelete(mCid);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<Integer> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("ClipDeleteRequest: failed");
            return null;
        }
        int error = response.readi32();
        return VdbResponse.success(error);
    }
}
