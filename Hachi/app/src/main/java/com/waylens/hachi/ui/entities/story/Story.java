package com.waylens.hachi.ui.entities.story;

import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.Playlist;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class Story {
    private String mName;
    private Playlist mPlaylist = new Playlist();

    public Story() {

    }

    public Playlist getPlaylist() {
        return mPlaylist;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setClipSet(ClipSet clipSet) {
        mPlaylist.setClipSet(clipSet);
    }

    public ClipSet getClipSet() {
        return mPlaylist.getClipSet();
    }


}
