package com.waylens.hachi.ui.fragments.clipplay2;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.vdb.urls.PlaylistPlaybackUrl;
import com.waylens.hachi.vdb.urls.VdbUrl;

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
