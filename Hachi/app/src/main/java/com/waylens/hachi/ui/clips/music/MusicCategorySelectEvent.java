package com.waylens.hachi.ui.clips.music;

import com.waylens.hachi.rest.response.MusicCategoryResponse;

/**
 * Created by Xiaofei on 2016/8/18.
 */
public class MusicCategorySelectEvent {
    public final MusicCategoryResponse.MusicCategory category;

    public MusicCategorySelectEvent(MusicCategoryResponse.MusicCategory category) {
        this.category = category;
    }
}
