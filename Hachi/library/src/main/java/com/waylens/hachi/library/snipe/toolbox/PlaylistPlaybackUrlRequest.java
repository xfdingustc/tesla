package com.waylens.hachi.library.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.library.vdb.Vdb;
import com.waylens.hachi.library.vdb.urls.PlaylistPlaybackUrl;
import com.waylens.hachi.library.snipe.VdbAcknowledge;
import com.waylens.hachi.library.snipe.VdbCommand;
import com.waylens.hachi.library.snipe.VdbRequest;
import com.waylens.hachi.library.snipe.VdbResponse;


/**
 * Created by Xiaofei on 2016/1/27.
 */
public class PlaylistPlaybackUrlRequest extends VdbRequest<PlaylistPlaybackUrl> {
    private static final String TAG = PlaylistPlaybackUrlRequest.class.getSimpleName();
    private final int mPlaylistID;
    private final int mStartTimeMs;

    public PlaylistPlaybackUrlRequest(int playListID,
                                      int startTimeMs,
                                      VdbResponse.Listener<PlaylistPlaybackUrl> listener,
                                      VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        mPlaylistID = playListID;
        this.mStartTimeMs = startTimeMs;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetPlaylistPlaybackUrl(Vdb.URL_TYPE_HLS,
                mPlaylistID, mStartTimeMs, Vdb.STREAM_SUB_1);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<PlaylistPlaybackUrl> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("PlaylistPlaybackUrlRequest: failed");
            return null;
        }

        PlaylistPlaybackUrl playbackUrl = new PlaylistPlaybackUrl();
        playbackUrl.listType = response.readi32();
        playbackUrl.playlistStartTimeMs = response.readi32();
        playbackUrl.stream = response.readi32();
        playbackUrl.urlType = response.readi32();
        playbackUrl.lengthMs = response.readi32();
        playbackUrl.hasMore = response.readi32() != 0;
        playbackUrl.url = response.readString();


        return VdbResponse.success(playbackUrl);
    }
}