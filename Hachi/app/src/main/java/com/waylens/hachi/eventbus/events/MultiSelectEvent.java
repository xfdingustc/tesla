package com.waylens.hachi.eventbus.events;

import com.waylens.hachi.vdb.Clip;

import java.util.List;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class MultiSelectEvent {
    private final boolean mIsMultiSelect;
    private final List<Clip> mSelectedClipList;

    public MultiSelectEvent(boolean isMultiSelect, List<Clip> clipList) {
        this.mIsMultiSelect = isMultiSelect;
        this.mSelectedClipList = clipList;
    }



    public boolean getIsMultiSeleted() {
        return mIsMultiSelect;
    }

    public int getSelectClipCount() {
        return mSelectedClipList == null ? 0 : mSelectedClipList.size();
    }
}
