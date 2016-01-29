package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Playlist;
import com.waylens.hachi.vdb.PlaylistPlaybackUrl;
import com.waylens.hachi.vdb.Vdb;

/**
 * Created by Xiaofei on 2016/1/27.
 */
public class PlaylistPlaybackUrlRequest extends VdbRequest<PlaylistPlaybackUrl> {
    private static final String TAG = PlaylistPlaybackUrlRequest.class.getSimpleName();
    private final Playlist mPlaylist;
    private final int mStartTimeMs;

    public PlaylistPlaybackUrlRequest(Playlist playlist, int startTimeMs, VdbResponse
                                      .Listener<PlaylistPlaybackUrl> listener, VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        this.mPlaylist = playlist;
        this.mStartTimeMs = startTimeMs;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetPlaylistPlaybackUrl(Vdb.URL_TYPE_HLS,
            mPlaylist.getId(), mStartTimeMs, Vdb.STREAM_SUB_1);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<PlaylistPlaybackUrl> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("PlaylistEditRequest: failed");
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