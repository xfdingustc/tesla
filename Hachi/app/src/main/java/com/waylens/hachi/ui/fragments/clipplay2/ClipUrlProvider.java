package com.waylens.hachi.ui.fragments.clipplay2;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.PlaylistEditRequest;
import com.waylens.hachi.snipe.toolbox.PlaylistPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.PlaylistSetRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.Playlist;
import com.waylens.hachi.vdb.PlaylistSet;
import com.waylens.hachi.vdb.Vdb;
import com.waylens.hachi.vdb.urls.PlaybackUrl;
import com.waylens.hachi.vdb.urls.PlaylistPlaybackUrl;
import com.waylens.hachi.vdb.urls.VdbUrl;

import java.util.List;

/**
 * Created by Xiaofei on 2016/2/22.
 */
public class ClipUrlProvider implements UrlProvider {
    private static final String TAG = ClipUrlProvider.class.getSimpleName();
    private VdbRequestQueue mVdbRequestQueue;
    private OnUriLoadedListener mListener;
    private Clip mClip;

    private Playlist mPlaylist;

    private int mClipAdded;


    public ClipUrlProvider(@NonNull VdbRequestQueue requestQueue, Clip clip) {
        this.mVdbRequestQueue = requestQueue;
        this.mClip = clip;
    }

    @Override
    public void getUri(long clipTimeMs, OnUriLoadedListener listener) {
        Logger.t(TAG).d("Start load clip url:");

        mListener = listener;


        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, Vdb.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, Vdb.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, clipTimeMs);

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(mClip.cid,
            parameters, new
            VdbResponse.Listener<PlaybackUrl>() {
                @Override
                public void onResponse(PlaybackUrl playbackUrl) {
                    if (mListener != null) {
                        mListener.onUriLoaded(playbackUrl);
                    }

                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Logger.t(TAG).d("", error);
            }
        });

        mVdbRequestQueue.add(request);
    }



//    private void startLoadPlaylist() {
//        doGetPlaylistInfo();
//    }

//    private void doGetPlaylistInfo() {
//        Logger.t(TAG).d("Get Play list info");
//
//        PlaylistSetRequest request = new PlaylistSetRequest(0, new VdbResponse.Listener<PlaylistSet>() {
//            @Override
//            public void onResponse(PlaylistSet response) {
//                Logger.t(TAG).d("Get Response!!!!!!");
//                //mStory.setPlaylist(response.getPlaylist(0));
//                mPlaylist = response.getPlaylist(0);
//                doClearPlayList();
//            }
//        }, new VdbResponse.ErrorListener() {
//            @Override
//            public void onErrorResponse(SnipeError error) {
//
//            }
//        });
//        mVdbRequestQueue.add(request);
//    }


//    private void doClearPlayList() {
//        PlaylistEditRequest request = new PlaylistEditRequest(PlaylistEditRequest
//            .METHOD_CLEAR_PLAYLIST, null, 0, 0, mPlaylist.getId(), new VdbResponse.Listener<Integer>() {
//            @Override
//            public void onResponse(Integer response) {
//                doConstructPlaylist();
//            }
//
//
//        }, new VdbResponse.ErrorListener() {
//            @Override
//            public void onErrorResponse(SnipeError error) {
//
//            }
//        });
//        mVdbRequestQueue.add(request);
//    }

//    private void doConstructPlaylist() {
//        mClipAdded = 0;
//
//        for (Clip clip : mClipList) {
//            PlaylistEditRequest playRequest = new PlaylistEditRequest(PlaylistEditRequest.METHOD_INSERT_CLIP,
//                clip, clip.getStartTimeMs(), clip.getEndTimeMs(), mPlaylist.getId(), new VdbResponse.Listener<Integer>() {
//                @Override
//                public void onResponse(Integer response) {
//                    Logger.t(TAG).d("Add one clip to playlist!!!!!! cid: " + " " + "realId: ");
//
//                    mClipAdded++;
//
//
//                    if (mClipAdded == mClipList.size()) {
//                        doGetPlaylistUri();
//                    }
//                }
//            }, new VdbResponse.ErrorListener() {
//                @Override
//                public void onErrorResponse(SnipeError error) {
//
//                }
//            });
//
//            mVdbRequestQueue.add(playRequest);
//        }
//    }


}
