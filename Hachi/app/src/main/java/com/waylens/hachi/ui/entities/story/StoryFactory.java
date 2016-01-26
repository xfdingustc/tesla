package com.waylens.hachi.ui.entities.story;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.vdb.ClipSet;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class StoryFactory {
    private final VdbRequestQueue mRequestQueue;
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
        this.mRequestQueue = Snipe.newRequestQueue();
        mRequestQueue.start();

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
        int clipType = mStrategy.getClipType();
        int flag = ClipSetRequest.FLAG_CLIP_EXTRA;
        ClipSetRequest request = new ClipSetRequest(clipType, flag, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {

            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mRequestQueue.add(request);
    }


}
