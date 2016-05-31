package com.waylens.hachi.ui.clips.player;

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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


public class PlaylistEditor {
    private static final String TAG = PlaylistEditor.class.getSimpleName();

    static final int ACTION_ADD = 0;
    static final int ACTION_DELETE = 1;
    static final int ACTION_MOVE = 2;
    static final int ACTION_TRIM = 3;

    private final VdbRequestQueue mVdbRequestQueue;
    private int mClipSetIndex;
    private final int mEditSetIndex = ClipSetManager.CLIP_SET_TYPE_ENHANCE_EDITING;

    private OnBuildCompleteListener mOnBuildCompleteListener;
    private OnDeleteCompleteListener mOnDeleteCompleteListener;
    private OnMoveCompletedListener mOnMoveCompletedListener;
    private OnTrimCompletedListener mOnTrimCompletedListener;

    private int mClipAdded;

    private final int mPlayListID;

    private EventBus mEventBus = EventBus.getDefault();

    private ClipSetManager mClipSetManager = ClipSetManager.getManager();

    public PlaylistEditor(@NonNull VdbRequestQueue requestQueue, int playListID) {
        this.mVdbRequestQueue = requestQueue;
        this.mPlayListID = playListID;
    }

    public ClipSet getClipSet() {
        return mClipSetManager.getClipSet(mClipSetIndex);
    }

    public int getPlaylistId() {
        return mPlayListID;
    }

    @Subscribe
    public void onEventClipSetChanged(ClipSetChangeEvent event) {
        Logger.t(TAG).d("receive event");
        if (event.getNeedRebuildList()) {
            doRebuildPlaylist(-1);
        }
    }


    public void build(int clipSetIndex, @NonNull OnBuildCompleteListener listener) {
        mOnBuildCompleteListener = listener;
        mClipSetIndex = clipSetIndex;

        doRebuildPlaylist(ACTION_ADD);
    }

    private void doRebuildPlaylist(int actionAdd) {
        doClearPlayList(actionAdd);
    }


    public void delete(int position, OnDeleteCompleteListener listener) {
        mOnDeleteCompleteListener = listener;
        doRebuildPlaylist(ACTION_DELETE);
    }

    public void move(int fromPosition, int toPosition, OnMoveCompletedListener listener) {
        mOnMoveCompletedListener = listener;
        doRebuildPlaylist(ACTION_MOVE);
    }



    private void doGetPlaylistInfo(final int action) {
        Logger.t(TAG).d("do get play list info");
        mVdbRequestQueue.add(new ClipSetExRequest(mPlayListID, ClipSetExRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
//                    mClipSetManager.updateClipSet(mClipSetIndex, clipSet);
                    adjustClipSet(clipSet);
                    switch (action) {
                        case ACTION_ADD:
                            if (mOnBuildCompleteListener != null) {
                                mOnBuildCompleteListener.onBuildComplete(clipSet);
                            }
                            break;
                        case ACTION_DELETE:
                            if (mOnDeleteCompleteListener != null) {
                                mOnDeleteCompleteListener.onDeleteComplete();
                            }
                            break;
                        case ACTION_MOVE:
                            if (mOnMoveCompletedListener != null) {
                                mOnMoveCompletedListener.onMoveCompleted(clipSet);
                            }
                            break;
                        case ACTION_TRIM:
                            if (mOnTrimCompletedListener != null) {
                                mOnTrimCompletedListener.onTrimCompleted(clipSet);
                            }
                            break;
                    }

                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("" + error);

                }
            }));
    }

    private void adjustClipSet(ClipSet clipSet) {
        ClipSet originClipSet = getClipSet();


        for (Clip origClip : originClipSet.getClipList()) {
            for (Clip newClip : clipSet.getClipList()) {
                if (origClip.realCid.equals(newClip.realCid)) {
                    origClip.editInfo.selectedStartValue = newClip.getStartTimeMs();
                    origClip.editInfo.selectedEndValue = newClip.getEndTimeMs();
                    break;
                }
            }
        }

        mEventBus.post(new ClipSetChangeEvent(mClipSetIndex, false));
    }

    private void doClearPlayList(final int action) {
        PlaylistEditRequest request = PlaylistEditRequest.getClearPlayListRequest(mPlayListID,
            new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    doBuildPlaylist(action);
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

    private void doBuildPlaylist(final int action) {
        mClipAdded = 0;
        for (final Clip clip : getClipSet().getClipList()) {
            Logger.t(TAG).d("Clip: " + clip.toString());
            PlaylistEditRequest playRequest = new PlaylistEditRequest(clip, clip.editInfo.selectedStartValue,
                clip.editInfo.selectedEndValue, mPlayListID, new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("Add one clip to playlist: " + clip.toString());
                    mClipAdded++;
                    if (mClipAdded == getClipSet().getClipList().size() && mOnBuildCompleteListener != null) {
                        doGetPlaylistInfo(action);
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


    public interface OnBuildCompleteListener {
        void onBuildComplete(ClipSet clipSet);
    }

    public interface OnDeleteCompleteListener {
        void onDeleteComplete();
    }

    public interface OnMoveCompletedListener {
        void onMoveCompleted(ClipSet clipSet);
    }

    public interface OnTrimCompletedListener {
        void onTrimCompleted(ClipSet clipSet);
    }

}
