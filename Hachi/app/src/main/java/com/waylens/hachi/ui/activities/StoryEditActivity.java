package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.entities.story.Story;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class StoryEditActivity extends BaseActivity {

    private static Story mSharedStory = null;
    private Story mStory;

    public static void launch(Activity startActivity, Story story) {
        Intent intent = new Intent(startActivity, StoryEditActivity.class);
        mSharedStory = story;
        startActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mStory = mSharedStory;
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_story_edit);
    }
}
