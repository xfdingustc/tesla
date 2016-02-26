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
import com.waylens.hachi.snipe.toolbox.PlaylistSetRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.PlaylistSet;

/**
 * Created by Xiaofei on 2016/1/26.
 */
public class StoryFactory {
    private static final String TAG = StoryFactory.class.getSimpleName();
    private final VdbRequestQueue mRequestQueue;
    private final VdtCamera mVdtCamera;
    private final StoryStrategy mStrategy;

    private Story mStory = new Story();

    private int mClipAdded;

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
        doGetPlaylistInfo();

    }

    private void doGetPlaylistInfo() {
        Logger.t(TAG).d("Get Play list info");
        PlaylistSetRequest request = new PlaylistSetRequest(0, new VdbResponse.Listener<PlaylistSet>() {
            @Override
            public void onResponse(PlaylistSet response) {
                Logger.t(TAG).d("Get Response!!!!!!");
                mStory.setPlaylist(response.getPlaylist(0));
                doClearPlayList();
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mRequestQueue.add(request);
    }

    private void doClearPlayList() {
        PlaylistEditRequest request = new PlaylistEditRequest(PlaylistEditRequest
            .METHOD_CLEAR_PLAYLIST, null, 0, 0, mStory.getPlaylist().getId(), new VdbResponse.Listener<Integer>() {
            @Override
            public void onResponse(Integer response) {
                doCreateStory();
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mRequestQueue.add(request);
    }

    private void doCreateStory() {
        int clipType = mStrategy.getClipType();
        int flag = ClipSetRequest.FLAG_CLIP_EXTRA;
        ClipSetRequest request = new ClipSetRequest(clipType, flag, new VdbResponse.Listener<ClipSet>() {
            @Override
            public void onResponse(ClipSet response) {

                mStory.setClipSet(response);

                doAddClipSetIntoPlaylist(mStory);

            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });

        mRequestQueue.add(request);
    }


    private void doAddClipSetIntoPlaylist(final Story story) {
        final ClipSet clipSet = story.getClipSet();
        mClipAdded = 0;

        final int clipSetCount = Math.min(clipSet.getCount(), mStrategy.getMaxiumClipCount());

        for (int i = 0; i < clipSetCount; i++) {
            final Clip clip = clipSet.getClip(i);

            int duration = Math.min(clip.getDurationMs(), mStrategy.getMaxiumClipLengthMs());

            PlaylistEditRequest playRequest = new PlaylistEditRequest(PlaylistEditRequest.METHOD_INSERT_CLIP,
                clip, clip.getStartTimeMs(), clip.getStartTimeMs() + duration, story.getPlaylist().getId(), new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("Add one clip to playlist!!!!!! cid: " + clip.cid + " " +
                        "realId: " + clip.realCid);
                    mClipAdded++;

                    if (mListener != null) {
                        int progress = mClipAdded * 100 / clipSetCount;
                        mListener.onCreateProgress(progress);

                        if (mClipAdded == clipSetCount) {
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
