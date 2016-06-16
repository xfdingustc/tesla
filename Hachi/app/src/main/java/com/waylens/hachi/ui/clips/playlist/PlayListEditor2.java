package com.waylens.hachi.ui.clips.playlist;

import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.PlaylistEditRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

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


    public PlayListEditor2(@NonNull VdbRequestQueue requestQueue, int playListId) {
        this.mVdbRequestQueue = requestQueue;
        this.mPlayListId = playListId;
        this.mClipSet = new ClipSet(playListId);
    }

    public int getPlaylistId() {
        return mPlayListId;
    }

    public void build(Clip clip, @NonNull OnBuildCompleteListener listener) {
        mOnBuildCompleteListener = listener;
        mClipSet.addClip(clip);
        ClipSetManager.getManager().updateClipSet(mPlayListId, mClipSet);

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
                    doBuildPlaylist();
                }

            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).d("clear play list error");
                }
            });
        mVdbRequestQueue.add(request);
    }


    private void doBuildPlaylist() {
        mClipAdded = 0;
        for (final Clip clip : mClipSet.getClipList()) {
            Logger.t(TAG).d("Clip: " + clip.toString());
            PlaylistEditRequest playRequest = new PlaylistEditRequest(clip, clip.editInfo.selectedStartValue,
                clip.editInfo.selectedEndValue, mPlayListId, new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("Add one clip to playlist: " + clip.toString());
                    mClipAdded++;
                    if (mClipAdded == mClipSet.getClipList().size() && mOnBuildCompleteListener != null) {
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

            mVdbRequestQueue.add(playRequest);
        }
    }


    private void doGetPlaylistInfo() {
        Logger.t(TAG).d("do get play list info");
        mVdbRequestQueue.add(new ClipSetExRequest(mPlayListId, ClipSetExRequest.FLAG_CLIP_EXTRA,
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
        }

        ));
    }

    private void adjustClipSet(ClipSet clipSet) {
        ClipSet originClipSet = mClipSet;


        for (Clip origClip : originClipSet.getClipList()) {
            for (Clip newClip : clipSet.getClipList()) {
                if (origClip.realCid.equals(newClip.realCid)) {
                    origClip.editInfo.selectedStartValue = newClip.getStartTimeMs();
                    origClip.editInfo.selectedEndValue = newClip.getEndTimeMs();
                    if (origClip.editInfo.selectedStartValue < origClip.editInfo.minExtensibleValue) {
                        origClip.editInfo.minExtensibleValue = origClip.editInfo.selectedStartValue;
                    }

                    if (origClip.editInfo.selectedEndValue > origClip.editInfo.maxExtensibleValue) {
                        origClip.editInfo.maxExtensibleValue = origClip.editInfo.selectedEndValue;
                    }
                    break;
                }
            }
        }

//        mEventBus.post(new ClipSetChangeEvent(mClipSetIndex, false));
    }

    public interface OnBuildCompleteListener {
        void onBuildComplete(ClipSet clipSet);
    }
}
