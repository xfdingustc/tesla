package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.entities.story.Story;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class StoryEditActivity extends BaseActivity {

    private static Story mSharedStory = null;
    private Story mStory;

    private VdbRequestQueue mRequestQueue;
    private VdbImageLoader mImageLoader;

    public static void launch(Activity startActivity, Story story) {
        Intent intent = new Intent(startActivity, StoryEditActivity.class);
        mSharedStory = story;
        startActivity.startActivity(intent);
    }

    @Bind(R.id.ivClipPreview)
    ImageView mIvClipPreview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mStory = mSharedStory;
        mRequestQueue = Snipe.newRequestQueue();
        mImageLoader = VdbImageLoader.getImageLoader(mRequestQueue);

        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_story_edit);

        ClipSet clipSet = mStory.getClipSet();
        Clip clip = clipSet.getClip(0);

        ClipPos clipPos = new ClipPos(clip, clip.getStartTimeMs(), ClipPos.TYPE_POSTER, false);

        mImageLoader.displayVdbImage(clipPos, mIvClipPreview);
    }
}
