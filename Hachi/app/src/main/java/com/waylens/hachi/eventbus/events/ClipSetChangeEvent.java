package com.waylens.hachi.eventbus.events;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class ClipSetChangeEvent {
    private final int mClipSetIndex;

    public ClipSetChangeEvent(int clipSetIndex) {
        this.mClipSetIndex = clipSetIndex;
    }

    public int getIndex() {
        return mClipSetIndex;
    }
}
