package com.waylens.hachi.eventbus.events;

import com.waylens.hachi.vdb.ClipSetPos;

/**
 * Created by Xiaofei on 2016/4/5.
 */
public class ClipSetPosChangeEvent {

    private final String mBroadcaster;
    private final ClipSetPos mClipSetPos;

    public ClipSetPosChangeEvent(ClipSetPos clipSetPos, String broadcaster) {
        this.mClipSetPos = clipSetPos;
        this.mBroadcaster = broadcaster;
    }

    public ClipSetPos getClipSetPos() {
        return mClipSetPos;
    }

    public String getBroadcaster() {
        return mBroadcaster;
    }
}
