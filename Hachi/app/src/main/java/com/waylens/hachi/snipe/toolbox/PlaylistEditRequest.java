package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.Playlist;

/**
 * Created by Xiaofei on 2016/1/27.
 */
public class PlaylistEditRequest extends VdbRequest<Integer> {
    private static final String TAG = PlaylistEditRequest.class.getSimpleName();
    public static final int METHOD_INSERT_CLIP = 0;
    public static final int METHOD_CLEAR_PLAYLIST = 1;
    private final Clip mClip;
    private final Playlist mPlaylist;
    private final long mStartTimeMs;
    private final long mEndTimeMs;


    public PlaylistEditRequest(int method, Clip clip, long startTimeMs, long endTimeMs,
                               Playlist playlist,
                               VdbResponse.Listener<Integer> listener,
                               VdbResponse.ErrorListener errorListener) {
        super(method, listener, errorListener);
        this.mClip = clip;
        this.mPlaylist = playlist;
        this.mStartTimeMs = startTimeMs;
        this.mEndTimeMs = endTimeMs;
    }


    @Override
    protected VdbCommand createVdbCommand() {
        switch (mMethod) {
            case METHOD_INSERT_CLIP:
                mVdbCommand = VdbCommand.Factory.createCmdInsertClip(mClip.realCid, mStartTimeMs,
                    mEndTimeMs, mPlaylist.getId(), -1);
                break;
            case METHOD_CLEAR_PLAYLIST:
                mVdbCommand = VdbCommand.Factory.createCmdClearPlayList(mPlaylist.getId());

                break;
        }
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<Integer> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("PlaylistEditRequest: failed");
            return null;
        }
        int error = response.readi32();
        return VdbResponse.success(error);
    }
}
