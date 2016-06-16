package com.waylens.hachi.eventbus.events;

import com.waylens.hachi.vdb.ClipSetPos;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class ClipSetChangeEvent {
    private final int mClipSetIndex;
    private final boolean mNeedRebuildList;

    public ClipSetChangeEvent() {
        this(-1, false);
    }

    public ClipSetChangeEvent(int clipSetIndex, boolean needRebuildList) {
        this.mClipSetIndex = clipSetIndex;
        this.mNeedRebuildList = needRebuildList;
    }

    public int getIndex() {
        return mClipSetIndex;
    }

    public boolean getNeedRebuildList() {
        return mNeedRebuildList;
    }



}
