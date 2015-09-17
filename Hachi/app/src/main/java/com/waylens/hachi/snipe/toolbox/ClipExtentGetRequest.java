package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipExtent;
import com.waylens.hachi.vdb.RemoteClip;

/**
 * Created by Richard on 9/15/15.
 */
public class ClipExtentGetRequest extends VdbRequest<ClipExtent> {

    private static final String TAG = "ClipExtentGetRequest";

    private Clip mClip;
    private VdbResponse.Listener<ClipExtent> mListener;

    public ClipExtentGetRequest(Clip clip, VdbResponse.Listener<ClipExtent> listener, VdbResponse.ErrorListener errorListener) {
        super(0, errorListener);
        mClip = clip;
        mListener = listener;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetClipExtent(mClip);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<ClipExtent> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("ClipExtentGetRequest: failed");
            return null;
        }

        int clipType = response.readi32();
        int clipId = response.readi32();
        int realClipId = response.readi32(); // use this with CLIP_TYPE_REAL to get index picture
        int reserved = response.readi32();

        long minClipStartTimeMs = response.readi64();
        long maxClipEndTimeMs = response.readi64();
        long clipStartTimeMs = response.readi64();
        long clipEndTimeMs = response.readi64();

        ClipExtent clipExtent = new ClipExtent(new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, null),
                new Clip.ID(Clip.CAT_REMOTE, RemoteClip.TYPE_BUFFERED, realClipId, null));
        clipExtent.minClipStartTimeMs = minClipStartTimeMs;
        clipExtent.maxClipEndTimeMs = maxClipEndTimeMs;
        clipExtent.clipStartTimeMs = clipStartTimeMs;
        clipExtent.clipEndTimeMs = clipEndTimeMs;
        return VdbResponse.success(clipExtent);
    }

    @Override
    protected void deliverResponse(ClipExtent response) {
        if (mListener !=  null) {
            mListener.onResponse(response);
        }
    }
}
