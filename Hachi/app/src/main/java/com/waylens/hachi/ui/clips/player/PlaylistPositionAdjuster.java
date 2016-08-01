package com.waylens.hachi.ui.clips.player;


import com.xfdingustc.snipe.vdb.urls.PlaylistPlaybackUrl;
import com.xfdingustc.snipe.vdb.urls.VdbUrl;

/**
 * Created by Xiaofei on 2016/3/1.
 */
public class PlaylistPositionAdjuster extends PositionAdjuster {
    private final PlaylistPlaybackUrl mUrl;

    public PlaylistPositionAdjuster(VdbUrl vdbUrl) {
        mUrl = (PlaylistPlaybackUrl)vdbUrl;
    }

    @Override
    public int getAdjustedPostion(int position) {
        int adjustedPosition = super.getAdjustedPostion(position);
        adjustedPosition += mUrl.playlistStartTimeMs;
        return adjustedPosition;
    }
}
