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

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/2/2.
 */
public class StoryEditActivity extends BaseActivity {
    public static final String TAG = StoryEditActivity.class.getSimpleName();
    private static Story mSharedStory;
    private Story mStory;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mImageLoader;

    @Bind(R.id.video_cover)
    ImageView mVideoCover;

    public static void launch(Activity startingActivity, Story story) {
        Intent intent = new Intent(startingActivity, StoryEditActivity.class);
        mSharedStory = story;
        startingActivity.startActivity(intent);
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
        mVdbRequestQueue = Snipe.newRequestQueue();
        mImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_story_edit);
        Clip firstClip = mStory.getPlaylist().getClip(0);
        ClipPos clipPos = new ClipPos(firstClip, firstClip.getStartTimeMs(), ClipPos.TYPE_POSTER,
            false);

        mImageLoader.displayVdbImage(clipPos, mVideoCover);
    }
}
