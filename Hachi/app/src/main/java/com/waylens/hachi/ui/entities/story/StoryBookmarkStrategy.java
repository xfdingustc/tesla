package com.waylens.hachi.ui.entities.story;

import com.waylens.hachi.vdb.Clip;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class StoryBookmarkStrategy implements StoryStrategy {
    @Override
    public int getClipType() {
        return Clip.TYPE_MARKED;
    }
}
