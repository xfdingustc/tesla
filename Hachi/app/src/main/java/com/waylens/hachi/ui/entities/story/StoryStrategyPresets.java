package com.waylens.hachi.ui.entities.story;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class StoryStrategyPresets {
    public static StoryStrategy createBookmarkStrategy() {
        return new StoryBookmarkStrategy();
    }
}
