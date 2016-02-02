package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.ui.activities.StoryEditActivity;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.ui.entities.story.Story;
import com.waylens.hachi.ui.entities.story.StoryFactory;
import com.waylens.hachi.ui.entities.story.StoryStrategy;
import com.waylens.hachi.ui.entities.story.StoryStrategyPresets;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Richard on 1/5/16.
 */
public class StoriesFragment extends BaseFragment {
    private static final String TAG = StoriesFragment.class.getSimpleName();

    @Bind(R.id.createStories)
    ImageView mIvCreateStories;

    @Bind(R.id.createStoryProgress)
    ProgressBar mCreateStoryProgress;

    private VdbRequestQueue mVdbRequestQueue;

    @OnClick(R.id.createStories)
    public void onCreateStoryClicked() {
        VdtCameraManager mVdtCameraManager = VdtCameraManager.getManager();
        if (mVdtCameraManager.isConnected()) {
            VdtCamera vdtCamera = VdtCameraManager.getManager().getConnectedCameras().get(0);
            StoryStrategy strategy = StoryStrategyPresets.createBookmarkStrategy();

            StoryFactory storyFactory = new StoryFactory(vdtCamera, strategy, new StoryFactory.OnCreateStoryListener() {
                @Override
                public void onCreateProgress(int progress) {
                    mCreateStoryProgress.setProgress(progress);
                }

                @Override
                public void onCreateFinished(final Story story) {
//                    Logger.t(TAG).d("sharableClips: " + sharableClips.get(0).toString());
//                    getFragmentManager().beginTransaction().add(R.id.root_container,
//                        EnhancementFragment.newInstance(story.getPlaylist())).commit();
                    StoryEditActivity.launch(getActivity(), story);
                    mIvCreateStories.setVisibility(View.VISIBLE);
                    mCreateStoryProgress.setVisibility(View.GONE);

                }
            });

            storyFactory.createStory();

            mIvCreateStories.setVisibility(View.GONE);
            mCreateStoryProgress.setVisibility(View.VISIBLE);

        } else {
            Logger.t(TAG).d("No camera connected");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_stories, savedInstanceState);
        mCreateStoryProgress.setMax(100);
        mVdbRequestQueue = Snipe.newRequestQueue();
        return view;
    }


}
