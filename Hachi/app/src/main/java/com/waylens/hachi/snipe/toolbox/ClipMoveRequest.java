package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;

/**
 * Created by Richard on 2/26/16.
 */
public class ClipMoveRequest extends VdbRequest<Integer> {
    private static final String TAG = "ClipMoveRequest";
    Clip.ID mCid;
    int mNewPosition;

    public ClipMoveRequest(Clip.ID cid, int newPosition,
                           VdbResponse.Listener<Integer> listener,
                           VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        mCid = cid;
        mNewPosition = newPosition;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdClipMove(mCid, mNewPosition);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<Integer> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("ClipMoveRequest: failed");
            return null;
        }
        int error = response.readi32();
        return VdbResponse.success(error);
    }
}
