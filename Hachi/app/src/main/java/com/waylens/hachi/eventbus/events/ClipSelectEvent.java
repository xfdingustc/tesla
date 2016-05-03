package com.waylens.hachi.eventbus.events;

import com.waylens.hachi.vdb.Clip;

import java.util.List;

/**
 * Created by Xiaofei on 2016/4/19.
 */
public class ClipSelectEvent {
    private final List<Clip> mClipList;

    public ClipSelectEvent(List<Clip> clipList) {
        mClipList = clipList;
    }

    public List<Clip> getClipList() {
        return mClipList;
    }
}
