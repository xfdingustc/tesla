package com.waylens.hachi.ui.fragments.clipplay2;

import android.content.Context;
import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.PlaylistEditRequest;
import com.waylens.hachi.snipe.toolbox.PlaylistSetRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.Playlist;
import com.waylens.hachi.vdb.PlaylistSet;

import java.util.List;

/**
 * Created by Xiaofei on 2016/2/29.
 */
public class PlaylistEditor {
    private static final String TAG = PlaylistEditor.class.getSimpleName();
    private final VdbRequestQueue mVdbRequestQueue;
    private final List<Clip> mCliplist;
    private Playlist mPlaylist;

    private OnBuildCompleteListener mOnBuildCompleteListener;

    private int mClipAdded;

    public PlaylistEditor(Context context, @NonNull VdtCamera vdtCamera, @NonNull List<Clip>
        clipList) {
        this.mVdbRequestQueue = Snipe.newRequestQueue(context, vdtCamera);
        this.mCliplist = clipList;
    }

    public List<Clip> getClipList() {
        return mCliplist;
    }

    public Playlist getPlaylist() {
        return mPlaylist;
    }

    public interface OnBuildCompleteListener {
        void onBuildComplete(Playlist playlist);
    }


    public void build(@NonNull OnBuildCompleteListener listener) {
        mOnBuildCompleteListener = listener;
        doGetPlaylistInfo();
    }

    private void doGetPlaylistInfo() {
        Logger.t(TAG).d("Get Play list info");

        PlaylistSetRequest request = new PlaylistSetRequest(0, new VdbResponse.Listener<PlaylistSet>() {
            @Override
            public void onResponse(PlaylistSet response) {
                Logger.t(TAG).d("Get Response!!!!!!");
                //mStory.setPlaylist(response.getPlaylist(0));
                mPlaylist = response.getPlaylist(0);
                doClearPlayList();
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

    private void doClearPlayList() {
        PlaylistEditRequest request = new PlaylistEditRequest(PlaylistEditRequest
            .METHOD_CLEAR_PLAYLIST, null, 0, 0, mPlaylist.getId(), new VdbResponse.Listener<Integer>() {
            @Override
            public void onResponse(Integer response) {
                doBuildPlaylist();
            }


        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
    }

    private void doBuildPlaylist() {
        mClipAdded = 0;

        for (Clip clip : mCliplist) {
            PlaylistEditRequest playRequest = new PlaylistEditRequest(PlaylistEditRequest.METHOD_INSERT_CLIP,
                clip, clip.getStartTimeMs(), clip.getEndTimeMs(), mPlaylist.getId(), new VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("Add one clip to playlist!!!!!! cid: " + " " + "realId: ");
                    mClipAdded++;
                    if (mClipAdded == mCliplist.size() && mOnBuildCompleteListener != null) {
                        mOnBuildCompleteListener.onBuildComplete(mPlaylist);
                    }
                }
            }, new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {

                }
            });

            mVdbRequestQueue.add(playRequest);
        }
    }




}
