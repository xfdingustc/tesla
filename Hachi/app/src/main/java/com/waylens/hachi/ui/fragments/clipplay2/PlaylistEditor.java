package com.waylens.hachi.ui.fragments.clipplay2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
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

    private OnBuildCompleteListener mOnBuildCompleteListener;
    private OnDeleteCompleteListener mOnDeleteCompleteListener;
    OnMoveCompletedListener mOnMoveCompletedListener;
    OnTrimCompletedListener mOnTrimCompletedListener;

    private int mClipAdded;

    final int mPlayListID;
    //ClipSet mPlayListClipSet;
    private ClipSetManager mClipSetManager = ClipSetManager.getManager();

    public PlaylistEditor(Context context, @NonNull VdtCamera vdtCamera, int playListID) {
        this.mVdbRequestQueue = vdtCamera.getRequestQueue();//Snipe.newRequestQueue(context, vdtCamera);
        //this class should not modify the original clip list
        mPlayListID = playListID;
    }

    public ClipSet getClipSet() {
        return mClipSetManager.getClipSet(mClipSetIndex);
    }

    public int getPlayListID() {
        return mPlayListID;
    }

    public void build(int clipSetIndex, @NonNull OnBuildCompleteListener listener) {
        mOnBuildCompleteListener = listener;
        mClipSetIndex = clipSetIndex;

        doClearPlayList(mClipSetManager.getClipSet(clipSetIndex).getClipList());
    }

    public void appendClips(List<Clip> clips, OnBuildCompleteListener listener) {
        doBuildPlaylist(clips);
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

    public void trimClip(int position,
                         Clip clip,
                         OnTrimCompletedListener listener) {
        mOnTrimCompletedListener = listener;
        doTrimClip(position, clip);
    }

    void doGetPlaylistInfo(final int action) {
        mVdbRequestQueue.add(new ClipSetExRequest(mPlayListID, ClipSetExRequest.FLAG_CLIP_EXTRA,
                new VdbResponse.Listener<ClipSet>() {
                    @Override
                    public void onResponse(ClipSet clipSet) {
                        mClipSetManager.updateClipSet(mClipSetIndex, clipSet);
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
                        Log.e("test", "", error);

                    }
                }));
    }

    void doClearPlayList(final ArrayList<Clip> clipList) {
        PlaylistEditRequest request = PlaylistEditRequest.getClearPlayListRequest(mPlayListID,
                new VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        doBuildPlaylist(clipList);
                    }

                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {

                    }
                });
        mVdbRequestQueue.add(request);
    }

    private void doBuildPlaylist(final List<Clip> clips) {
        mClipAdded = 0;
        for (Clip clip : clips) {
            PlaylistEditRequest playRequest = new PlaylistEditRequest(
                    clip,
                    clip.getStartTimeMs(),
                    clip.getEndTimeMs(),
                    mPlayListID,
                    new VdbResponse.Listener<Integer>() {
                        @Override
                        public void onResponse(Integer response) {
//                            Logger.t(TAG).d("Add one clip to playlist!!!!!! cid: " + " " + "realId: ");
                            mClipAdded++;
                            if (mClipAdded == clips.size() && mOnBuildCompleteListener != null) {
                                doGetPlaylistInfo(ACTION_ADD);
                            }
                        }
                    },
                    new VdbResponse.ErrorListener() {
                        @Override
                        public void onErrorResponse(SnipeError error) {
                            mClipAdded++;
                            Log.e("test", "ClipDeleteRequest", error);
                        }
                    });

            mVdbRequestQueue.add(playRequest);
        }
    }

    private void doDeleteClip(int position) {
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
                        Log.e("test", "ClipDeleteRequest", error);
                    }
                }));

    }

    void doMoveClip(int fromPosition, int toPosition) {
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
                        Log.e("test", "ClipMoveRequest", error);
                    }
                }));
    }

    void doTrimClip(final int position, final Clip clip) {
        ClipSet clipSet = getClipSet();
        if (clipSet == null) {
            return;
        }
        Clip oldClip = clipSet.getClip(position);
        if (oldClip == null) {
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
                        Log.e("test", "ClipDeleteRequest", error);
                    }
                }));

    }

    void insertClip(Clip clip, final int index) {
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
                Log.e("test", "ClipDeleteRequest", error);
            }
        }));
    }

    /**
     * To work around a playlist insertion bug: when inserting a clip to a "position",
     * it's actually inserted at "position + 1"
     */
    void tmpMove(Clip clip, int toPosition) {
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
                        Log.e("test", "ClipMoveRequest", error);
                    }
                }));
    }

    void tmpDoGetPlaylistInfo(final int position) {
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
                        Log.e("test", "", error);

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
