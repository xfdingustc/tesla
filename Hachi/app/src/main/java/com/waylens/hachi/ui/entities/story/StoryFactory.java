package com.waylens.hachi.ui.entities.story;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.snipe.toolbox.PlaylistEditRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class StoryFactory {
    private static final String TAG = StoryFactory.class.getSimpleName();
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


    }


    public void createStory() {
        doCreateStory();
//        mCreateStoryThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        });
//        mCreateStoryThread.start();
    }

    private void doCreateStory() {
        int clipType = mStrategy.getClipType();
        int flag = ClipSetRequest.FLAG_CLIP_EXTRA;
        ClipSetRequest request = new ClipSetRequest(Clip.TYPE_MARKED, flag, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {
                Logger.t(TAG).d("Get Clip Set");
//                Story story = new Story();
//                story.setClipSet(response);
//
//                doAddClipSetIntoPlaylist(story);

            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        Logger.t(TAG).d("Add Request!!!");
        mRequestQueue.add(request);
    }

    private int mClipAdded;

    private void doAddClipSetIntoPlaylist(final Story story) {
        final ClipSet clipSet = story.getClipSet();
        mClipAdded = 0;

        int clipSetCount = Math.min(clipSet.getCount(), 3);

        for (int i = 0; i < clipSetCount; i++) {
            Clip clip = clipSet.getClip(i);

            PlaylistEditRequest playRequest = new PlaylistEditRequest(PlaylistEditRequest.METHOD_INSERT_CLIP,
                clip, clip.getStartTimeMs(), clip.getStartTimeMs() + clip.getDurationMs(), story
                .getPlaylist(), new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("Add one clip to playlist!!!!!!");
                    mClipAdded++;

                    if (mListener != null) {
                        int progress = mClipAdded * 100 / clipSet.getCount();
                        mListener.onCreateProgress(progress);

                        if (mClipAdded == clipSet.getCount()) {
                            mListener.onCreateFinished(story);
                        }
                    }
                }
            }, new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {

                }
            });

            mRequestQueue.add(playRequest);
        }

    }


}
