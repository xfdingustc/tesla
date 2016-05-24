package com.waylens.hachi.ui.clips.player;

import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipDeleteRequest;
import com.waylens.hachi.snipe.toolbox.ClipMoveRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.PlaylistEditRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/2/29.
 */
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

    private ClipSetManager mClipSetManager = ClipSetManager.getManager();

    public PlaylistEditor(@NonNull VdbRequestQueue requestQueue, int playListID) {
        this.mVdbRequestQueue = requestQueue;
        this.mPlayListID = playListID;
    }

    public ClipSet getClipSet() {
        return mClipSetManager.getClipSet(mClipSetIndex);
    }

    public ClipSet getEditClipSet() {
        return mClipSetManager.getClipSet(mEditSetIndex);
    }

    public int getPlaylistId() {
        return mPlayListID;
    }


    public void build(int clipSetIndex, @NonNull OnBuildCompleteListener listener) {
        mOnBuildCompleteListener = listener;
        mClipSetIndex = clipSetIndex;

        doRebuildPlaylist(ACTION_ADD);
    }

    private void doRebuildPlaylist(int actionAdd) {
        doClearPlayList(actionAdd);
    }

    public void appendClips(List<Clip> clips, OnBuildCompleteListener listener) {
        ClipSet clipSet = getClipSet();
        clipSet.getClipList().addAll(clips);
        ClipSetManager.getManager().updateClipSet(mClipSetIndex, clipSet);
        doRebuildPlaylist(ACTION_ADD);

        mOnBuildCompleteListener = listener;
    }

    public void delete(int position, OnDeleteCompleteListener listener) {
        mOnDeleteCompleteListener = listener;
        doDeleteClip(position);
    }

    public void move(int fromPosition, int toPosition, OnMoveCompletedListener listener) {
        mOnMoveCompletedListener = listener;
        doMoveClip(fromPosition, toPosition);
    }

    public void trimClip(int position, Clip clip, OnTrimCompletedListener listener) {
        mOnTrimCompletedListener = listener;
//        doTrimClip(position, clip);
        doRebuildPlaylist(ACTION_TRIM);
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
    }

    private void doClearPlayList(final int action) {
        Logger.t(TAG).d("doClearPlaylist");
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
            PlaylistEditRequest playRequest = new PlaylistEditRequest(clip, clip.editInfo.selectedStartValue,
                clip.editInfo.selectedEndValue, mPlayListID, new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("Add one clip to playlist: " + clip.toString() );
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

    private void doDeleteClip(int position) {
        Logger.t(TAG).d("do delete clip");
        if (getClipSet() == null) {
            return;
        }
        Clip clip = getClipSet().getClip(position);
        if (clip == null) {
            return;
        }
        mVdbRequestQueue.add(new ClipDeleteRequest(clip.cid,
            new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    doGetPlaylistInfo(ACTION_DELETE);
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("ClipDeleteRequest: " + error);
                }
            }));

    }

    private void doMoveClip(int fromPosition, int toPosition) {
        Logger.t(TAG).d("do move clip");
        if (getClipSet() == null) {
            return;
        }
        Clip clip = getClipSet().getClip(fromPosition);
        if (clip == null) {
            return;
        }
        mVdbRequestQueue.add(new ClipMoveRequest(clip.cid, toPosition,
            new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    doGetPlaylistInfo(ACTION_MOVE);
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("ClipMoveRequest" + error);
                }
            }));
    }

    private void doTrimClip(final int position, final Clip clip) {
        Logger.t(TAG).d("do trim clip");
        ClipSet clipSet = getClipSet();
        if (clipSet == null) {
            Logger.t(TAG).d("clip set is null clipSetIndex: " + mClipSetIndex);
            return;
        }
        Clip oldClip = clipSet.getClip(position);
        if (oldClip == null) {
            Logger.t(TAG).d("old clip is null");
            return;
        }
        mVdbRequestQueue.add(new ClipDeleteRequest(oldClip.cid,
            new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    insertClip(clip, position);
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("ClipDeleteRequest" + error);
                }
            }));

    }

    private void insertClip(Clip clip, final int index) {
        Logger.t(TAG).d("do insert clip");
        long startValue = clip.editInfo.selectedStartValue;
        long endValue = clip.editInfo.selectedEndValue;

        mVdbRequestQueue.add(new PlaylistEditRequest(PlaylistEditRequest.METHOD_INSERT_CLIP,
            clip, startValue, endValue, index, mPlayListID, new VdbResponse.Listener<Integer>() {
            @Override
            public void onResponse(Integer response) {
                tmpDoGetPlaylistInfo(index + 1);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Logger.t(TAG).e("ClipDeleteRequest" + error);
            }
        }));
    }

    /**
     * To work around a playlist insertion bug: when inserting a clip to a "position",
     * it's actually inserted at "position + 1"
     */
    private void tmpMove(Clip clip, int toPosition) {
        mVdbRequestQueue.add(new ClipMoveRequest(clip.cid, toPosition,
            new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    doGetPlaylistInfo(ACTION_TRIM);
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    Logger.t(TAG).e("ClipMoveRequest" + error);
                }
            }));
    }

    private void tmpDoGetPlaylistInfo(final int position) {
        mVdbRequestQueue.add(new ClipSetExRequest(0x100, ClipSetExRequest.FLAG_CLIP_EXTRA,
            new VdbResponse.Listener<ClipSet>() {
                @Override
                public void onResponse(ClipSet clipSet) {
                    if (position >= clipSet.getCount()) {
                        doGetPlaylistInfo(ACTION_TRIM);
                    } else {
                        Clip clip = clipSet.getClip(position);
                        tmpMove(clip, position - 1);
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
