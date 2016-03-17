package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;

public class AddBookmarkRequest extends VdbRequest<Integer> {
    private static final String TAG = AddBookmarkRequest.class.getSimpleName();

    private final Clip mClip;
    private final long mStartTimeMs;
    private final long mEndTimeMs;

    public AddBookmarkRequest(Clip clip, long startTimeMs, long endTimeMs, VdbResponse
        .Listener<Integer> listener, VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        this.mClip = clip;
        this.mStartTimeMs = startTimeMs;
        this.mEndTimeMs = endTimeMs;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdAddBookmark(mClip.cid, mStartTimeMs, mEndTimeMs);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<Integer> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).d("response: " + response.getRetCode());
            return null;
        }

        int error = response.readi32();
        return VdbResponse.success(error);
    }
}