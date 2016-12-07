package com.waylens.hachi.ui.clips.timelapse;

import java.io.IOException;

/**
 * Created by lshw on 16/12/6.
 */

public class ThumbnailLoadable implements ThumbnailLoader.Loadable{
    public static final String TAG = ThumbnailLoadable.class.getSimpleName();

    private int mTimeInterval;

    public ThumbnailLoadable() {

    }
    @Override
    public void cancelLoad() {

    }

    @Override
    public boolean isLoadCanceled() {
        return false;
    }

    @Override
    public void load() throws IOException, InterruptedException {

    }
}
