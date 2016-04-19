package com.waylens.hachi.eventbus.events;

import com.waylens.hachi.vdb.Clip;

/**
 * Created by Xiaofei on 2016/4/19.
 */
public class ClipSelectEvent {
    private final Clip mClip;

    public ClipSelectEvent(Clip clip) {
        mClip = clip;
    }

    public Clip getClip() {
        return mClip;
    }
}
