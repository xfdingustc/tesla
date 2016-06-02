package com.waylens.hachi.eventbus.events;

import com.waylens.hachi.vdb.ClipSetPos;

/**
 * Created by Xiaofei on 2016/4/5.
 */
public class ClipSetPosChangeEvent {

    private final String mBroadcaster;
    private final ClipSetPos mClipSetPos;
    private final int mIntent;

    public static final int INTENT_NONE = 0;
    public static final int INTENT_SHOW_THUMBNAIL = 1;

    public ClipSetPosChangeEvent(ClipSetPos clipSetPos, String broadcaster) {
        this(clipSetPos, broadcaster, INTENT_NONE);
    }

    public ClipSetPosChangeEvent(ClipSetPos clipSetPos, String broadcaster, int intent) {
        this.mClipSetPos = clipSetPos;
        this.mBroadcaster = broadcaster;
        this.mIntent = intent;
    }

    public ClipSetPos getClipSetPos() {
        return mClipSetPos;
    }

    public String getBroadcaster() {
        return mBroadcaster;
    }

    public int getIntent() {
        return mIntent;
    }
}
