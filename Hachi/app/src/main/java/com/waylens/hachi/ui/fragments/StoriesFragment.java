package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.activities.StoryEditActivity;
import com.waylens.hachi.ui.entities.story.Story;
import com.waylens.hachi.ui.entities.story.StoryFactory;
import com.waylens.hachi.ui.entities.story.StoryStrategy;
import com.waylens.hachi.ui.entities.story.StoryStrategyPresets;

import butterknife.OnClick;

/**
 * Created by Richard on 1/5/16.
 */
public class StoriesFragment extends BaseFragment {
    private static final String TAG = StoriesFragment.class.getSimpleName();

    @OnClick(R.id.createStories)
    public void onCreateStoryClicked() {
        VdtCameraManager mVdtCameraManager = VdtCameraManager.getManager();
        if (mVdtCameraManager.isConnected()) {
            VdtCamera vdtCamera = VdtCameraManager.getManager().getConnectedCameras().get(0);
            StoryStrategy strategy = StoryStrategyPresets.createBookmarkStrategy();

            StoryFactory storyFactory = new StoryFactory(vdtCamera, strategy, new StoryFactory.OnCreateStoryListener() {
                @Override
                public void onCreateProgress(int progress) {

                }

                @Override
                public void onCreateFinished(Story story) {
                    StoryEditActivity.launch(getActivity(), story);
                }
            });

            storyFactory.createStory();


        } else {
            Logger.t(TAG).d("No camera connected");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_stories, savedInstanceState);
        return view;
    }
}
