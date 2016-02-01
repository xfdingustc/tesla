package com.waylens.hachi.snipe.toolbox;

import android.util.Log;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Playlist;
import com.waylens.hachi.vdb.PlaylistSet;

/**
 * Created by Xiaofei on 2016/2/1.
 */
public class PlaylistSetRequest extends VdbRequest<PlaylistSet> {
    private static final String TAG = PlaylistSetRequest.class.getSimpleName();
    private final int mFlags;

    public PlaylistSetRequest(int flags, VdbResponse.Listener<PlaylistSet> listener, VdbResponse
        .ErrorListener errorListener) {
        super(0, listener, errorListener);
        this.mFlags = flags;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetPlaylistSetInfo(mFlags);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<PlaylistSet> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).e("ackGetPlaylistSet: failed");
            return null;
        }

        PlaylistSet playlistSet = new PlaylistSet();

        playlistSet.mFlags = response.readi32();
        playlistSet.mNumPlaylists = response.readi32();

        int numPlaylists = playlistSet.mNumPlaylists;
        for (int i = 0; i < numPlaylists; i++) {
            Playlist playlist = new Playlist();
            playlist.setId(response.readi32());
            playlist.setProperties(response.readi32());
            //playlist.numClips = 0;
            response.skip(4);
            playlist.setTotalLengthMs(response.readi32());
            playlistSet.mPlaylists.add(playlist);
        }

        return VdbResponse.success(playlistSet);
    }
}
