package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.waylens.hachi.R;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.entities.story.Story;
import com.waylens.hachi.ui.fragments.CameraVideoPlayFragment;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;
import com.xfdingustc.far.FixedAspectRatioFrameLayout;


import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/2/2.
 */
public class StoryEditActivity extends BaseActivity {
    public static final String TAG = StoryEditActivity.class.getSimpleName();
    private static Story mSharedStory;
    private Story mStory;

    private CameraVideoPlayFragment mVideoPlayFragment;

    private VdbRequestQueue mVdbRequestQueue;
    private VdbImageLoader mImageLoader;

    @Bind(R.id.video_cover)
    ImageView mVideoCover;




    @OnClick(R.id.btn_play)
    public void onBtnPlayClicked() {
        mVideoPlayFragment = CameraVideoPlayFragment.newInstance(Snipe.newRequestQueue(), mStory
            .getPlaylist(), null);
        getFragmentManager().beginTransaction().replace(R.id.fragment_content, mVideoPlayFragment).commit();
        mVideoCover.setVisibility(View.INVISIBLE);
    }

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
