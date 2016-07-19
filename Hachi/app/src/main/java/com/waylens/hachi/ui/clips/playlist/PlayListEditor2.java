package com.waylens.hachi.ui.clips.playlist;

import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.library.vdb.Clip;
import com.waylens.hachi.library.vdb.ClipSet;
import com.waylens.hachi.library.vdb.ClipSetManager;
import com.waylens.hachi.library.snipe.SnipeError;
import com.waylens.hachi.library.snipe.VdbRequestQueue;
import com.waylens.hachi.library.snipe.VdbResponse;
import com.waylens.hachi.library.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.library.snipe.toolbox.PlaylistEditRequest;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/6/16.
 */
public class PlayListEditor2 {
    private static final String TAG = PlayListEditor2.class.getSimpleName();

    private final VdbRequestQueue mVdbRequestQueue;
    private final int mPlayListId;
    private OnBuildCompleteListener mOnBuildCompleteListener;

    private ClipSet mClipSet;

    private int mClipAdded;

    private EventBus mEventBus = EventBus.getDefault();


    @Subscribe
    public void onEventClipSetChanged(ClipSetChangeEvent event) {
        Logger.t(TAG).d("receive event " + event.getNeedRebuildList());
        if (event.getNeedRebuildList()) {
            doRebuildPlaylist();
        }
    }

    public PlayListEditor2(@NonNull VdbRequestQueue requestQueue, int playListId) {
        this.mVdbRequestQueue = requestQueue;
        this.mPlayListId = playListId;
        this.mClipSet = new ClipSet(playListId);
    }

    public int getPlaylistId() {
        return mPlayListId;
    }


    public void reconstruct() {
        mClipSet = ClipSetManager.getManager().getClipSet(mPlayListId);
    }

    public void build(Clip clip, @NonNull OnBuildCompleteListener listener) {
        mOnBuildCompleteListener = listener;
        mClipSet.clear();
        mClipSet.addClip(clip);
        ClipSetManager.getManager().updateClipSet(mPlayListId, mClipSet);

        doRebuildPlaylist();
    }

    public void build(List<Clip> clipList, @NonNull OnBuildCompleteListener listener) {
        mOnBuildCompleteListener = listener;
        mClipSet.clear();
        for (Clip clip : clipList) {
            mClipSet.addClip(clip);
        }
        ClipSetManager.getManager().updateClipSet(mPlayListId, mClipSet);

        doRebuildPlaylist();
    }

    public void cancel() {
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.cancelAll(TAG);
        }
    }

    public void add(ArrayList<Clip> clips) {
        for (Clip clip : clips) {
            mClipSet.addClip(clip);
        }

        doRebuildPlaylist();
    }


    public void remove(int position) {
        Logger.t(TAG).d("remove");
        mClipSet.remove(position);
        doRebuildPlaylist();
    }




    private void doRebuildPlaylist() {
        doClearPlayList();
    }


    private void doClearPlayList() {
        PlaylistEditRequest request = PlaylistEditRequest.getClearPlayListRequest(mPlayListId,
            new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("clear playlist finished");
                    doBuildPlaylist();
                }

            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).d("clear play list error");
                }
            });
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.add(request.setTag(TAG));
        }
    }


    private void doBuildPlaylist() {
        mClipAdded = 0;

        if (mClipSet.getCount() == 0) {
            mEventBus.post(new ClipSetChangeEvent(mPlayListId, false));
            return;
        }

        for (final Clip clip : mClipSet.getClipList()) {
            PlaylistEditRequest playRequest = new PlaylistEditRequest(clip, clip.editInfo.selectedStartValue,
                clip.editInfo.selectedEndValue, mPlayListId, new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("Add one clip to playlist: " + clip.toString()
                        + " editinfo: " + clip.editInfo.toString());
                    mClipAdded++;
                    if (mClipAdded == mClipSet.getClipList().size()) {
                        doGetPlaylistInfo();
                    }
                }
            },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        mClipAdded++;
                        Logger.t(TAG).d("ClipDeleteRequest", error);
                    }
                });

            if (mVdbRequestQueue != null) {
                mVdbRequestQueue.add(playRequest.setTag(TAG));
            }
        }
    }


    private void doGetPlaylistInfo() {
        Logger.t(TAG).d("do get play list info");
        ClipSetExRequest request = new ClipSetExRequest(mPlayListId, ClipSetExRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
//                    mClipSetManager.updateClipSet(mClipSetIndex, clipSet);
                    adjustClipSet(clipSet);

                    if (mOnBuildCompleteListener != null) {
                        mOnBuildCompleteListener.onBuildComplete(clipSet);
                    }


                }

            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Logger.t(TAG).e("" + error);

            }
        });
        request.setTag(TAG);
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.add(request);
        }

    }

    private void adjustClipSet(ClipSet clipSet) {
        Logger.t(TAG).d("origin count: " + mClipSet.getCount() + " new clip: " + clipSet.getCount());
        for (int i = 0; i < mClipSet.getCount(); i++) {
            Clip originClip = mClipSet.getClip(i);
            Clip newClip = clipSet.getClip(i);
            if (newClip != null) {
                originClip.editInfo.selectedStartValue = newClip.getStartTimeMs();
                originClip.editInfo.selectedEndValue = newClip.getEndTimeMs();
                if (originClip.editInfo.selectedStartValue < originClip.editInfo.minExtensibleValue) {
                    originClip.editInfo.minExtensibleValue = originClip.editInfo.selectedStartValue;
                }

                if (originClip.editInfo.selectedEndValue > originClip.editInfo.maxExtensibleValue) {
                    originClip.editInfo.maxExtensibleValue = originClip.editInfo.selectedEndValue;
                }
            }
        }




        mEventBus.post(new ClipSetChangeEvent(mPlayListId, false));
    }

    public interface OnBuildCompleteListener {
        void onBuildComplete(ClipSet clipSet);
    }
}
