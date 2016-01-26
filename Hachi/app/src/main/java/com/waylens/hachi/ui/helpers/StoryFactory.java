package com.waylens.hachi.ui.helpers;

import android.os.Handler;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.ui.entities.Story;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class StoryFactory {

    private final VdtCamera mVdtCamera;
    private final StoryStrategy mStrategy;

    private Thread mCreateStoryThread;
    private OnCreateStoryListener mListener;

    public interface OnCreateStoryListener {

        void onCreateProgress(int progress);

        void onCreateFinished(Story story);
    }

    public StoryFactory(VdtCamera vdtCamera, StoryStrategy strategy, OnCreateStoryListener listener) {
        this.mVdtCamera = vdtCamera;
        this.mStrategy = strategy;
        this.mListener = listener;
    }


    public void createStory() {
        mCreateStoryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                doCreateStory();
            }
        });
        mCreateStoryThread.start();
    }

    private void doCreateStory() {

    }


}
