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
import com.waylens.hachi.snipe.toolbox.ClipExtentUpdateRequest;
import com.waylens.hachi.snipe.toolbox.ClipMoveRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetRequest;
import com.waylens.hachi.snipe.toolbox.PlaylistEditRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSet;

import java.util.Collections;
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
    private final List<Clip> mOriginalClipList;

    private OnBuildCompleteListener mOnBuildCompleteListener;
    private OnDeleteCompleteListener mOnDeleteCompleteListener;
    OnMoveCompletedListener mOnMoveCompletedListener;
    OnTrimCompletedListener mOnTrimCompletedListener;

    private int mClipAdded;

    final int mPlayListID;
    ClipSet mPlayListClipSet;

    public PlaylistEditor(Context context, @NonNull VdtCamera vdtCamera, @NonNull List<Clip>
            originalClipList, int playListID) {
        this.mVdbRequestQueue = Snipe.newRequestQueue(context, vdtCamera);
        //this class should not modify the original clip list
        this.mOriginalClipList = Collections.unmodifiableList(originalClipList);
        mPlayListID = playListID;
    }

    public List<Clip> getOriginalClipList() {
        return mOriginalClipList;
    }

    public List<Clip> getPlayListClips() {
        if (mPlayListClipSet != null) {
            return mPlayListClipSet.getClipList();
        } else {
            return null;
        }
    }

    public int getPlayListID() {
        return mPlayListID;
    }

    public void build(@NonNull OnBuildCompleteListener listener) {
        mOnBuildCompleteListener = listener;
        doClearPlayList();
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
                         long startValue,
                         long endValue,
                         OnTrimCompletedListener listener) {
        mOnTrimCompletedListener = listener;
        doTrimClip(position, startValue, endValue);
    }

    void doGetPlaylistInfo(final int action) {
        mVdbRequestQueue.add(new ClipSetRequest(0x100, ClipSetRequest.FLAG_CLIP_EXTRA,
                new VdbResponse.Listener<ClipSet>() {
                    @Override
                    public void onResponse(ClipSet clipSet) {
                        mPlayListClipSet = clipSet;
                        switch (action) {
                            case ACTION_ADD:
                                if (mOnBuildCompleteListener != null) {
                                    mOnBuildCompleteListener.onBuildComplete();
                                }
                                break;
                            case ACTION_DELETE:
                                if (mOnDeleteCompleteListener != null) {
                                    mOnDeleteCompleteListener.onDeleteComplete();
                                }
                                break;
                            case ACTION_MOVE:
                                if (mOnMoveCompletedListener != null) {
                                    mOnMoveCompletedListener.onMoveCompleted();
                                }
                                break;
                            case ACTION_TRIM:
                                if (mOnTrimCompletedListener != null) {
                                    mOnTrimCompletedListener.onTrimCompleted();
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

    void doClearPlayList() {
        PlaylistEditRequest request = new PlaylistEditRequest(PlaylistEditRequest
                .METHOD_CLEAR_PLAYLIST, null, 0, 0, mPlayListID, new VdbResponse.Listener<Integer>() {
            @Override
            public void onResponse(Integer response) {
                doBuildPlaylist(mOriginalClipList);
            }

        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

    private void doBuildPlaylist(final List<Clip> clips) {
        mClipAdded = 0;
        for (Clip clip : clips) {
            PlaylistEditRequest playRequest = new PlaylistEditRequest(PlaylistEditRequest.METHOD_INSERT_CLIP,
                    clip, clip.getStartTimeMs(), clip.getEndTimeMs(), mPlayListID, new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("Add one clip to playlist!!!!!! cid: " + " " + "realId: ");
                    mClipAdded++;
                    if (mClipAdded == clips.size() && mOnBuildCompleteListener != null) {
                        doGetPlaylistInfo(ACTION_ADD);
                    }
                }
            }, new VdbResponse.ErrorListener() {
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
        if (mPlayListClipSet == null) {
            return;
        }
        Clip clip = mPlayListClipSet.getClip(position);
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
        if (mPlayListClipSet == null) {
            return;
        }
        Clip clip = mPlayListClipSet.getClip(fromPosition);
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

    void doTrimClip(int position, long startValue, long endValue) {
        if (mPlayListClipSet == null) {
            return;
        }
        Clip clip = mPlayListClipSet.getClip(position);
        if (clip == null) {
            return;
        }

        mVdbRequestQueue.add(new ClipExtentUpdateRequest(clip,
                startValue,
                endValue,
                new VdbResponse.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        doGetPlaylistInfo(ACTION_TRIM);
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "ClipExtentUpdateRequest", error);
                    }
                }
        ));
    }


    public interface OnBuildCompleteListener {
        void onBuildComplete();
    }

    public interface OnDeleteCompleteListener {
        void onDeleteComplete();
    }

    public interface OnMoveCompletedListener {
        void onMoveCompleted();
    }

    public interface OnTrimCompletedListener {
        void onTrimCompleted();
    }

}
