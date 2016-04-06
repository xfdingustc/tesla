package com.waylens.hachi.eventbus.events;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class MultiSelectEvent {
    private final boolean mIsMultiSelect;

    public MultiSelectEvent(boolean isMultiSelect) {
        this.mIsMultiSelect = isMultiSelect;
    }

    public boolean getIsMultiSeleted() {
        return mIsMultiSelect;
    }
}
