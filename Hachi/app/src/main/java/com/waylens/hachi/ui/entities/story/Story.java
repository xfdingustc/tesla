package com.waylens.hachi.ui.entities.story;

import com.waylens.hachi.vdb.ClipSet;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class Story {
    private String mName;
    private ClipSet mClipSet;

    public Story() {

    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setClipSet(ClipSet clipSet) {
        mClipSet = clipSet;
    }

    public ClipSet getClipSet() {
        return mClipSet;
    }


}
