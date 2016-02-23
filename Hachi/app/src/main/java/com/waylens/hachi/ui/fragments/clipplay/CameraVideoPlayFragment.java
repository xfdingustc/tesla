package com.waylens.hachi.ui.fragments.clipplay;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.toolbox.PlaylistPlaybackUrlRequest;
import com.waylens.hachi.vdb.Playlist;
import com.waylens.hachi.vdb.urls.PlaylistPlaybackUrl;
import com.waylens.hachi.vdb.Vdb;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.urls.PlaybackUrl;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

import java.io.IOException;

/**
 * Play Camera Video
 * Created by Richard on 11/4/15.
 */
public class CameraVideoPlayFragment extends VideoPlayFragment {
    private static final String TAG = CameraVideoPlayFragment.class.getSimpleName();

    private static final int MODE_SINGLE_CLIP = 0;
    private static final int MODE_PLAYLIST = 1;

    private VdbRequestQueue mVdbRequestQueue;
    private Clip mClip;
    private Playlist mPlayList;
    private int mPlayMode;

    MediaPlayer mAudioPlayer;

    SparseArray<RawDataBlock> mTypedRawData = new SparseArray<>();
    SparseIntArray mTypedState = new SparseIntArray();
    SparseIntArray mTypedPosition = new SparseIntArray();

    private PlaybackUrl mPlaybackUrl;
    private PlaylistPlaybackUrl mPlaybackListUrl;
    long mInitPosition;

    String mAudioPath;

    boolean isAudioPrepared;

    public static CameraVideoPlayFragment newInstance(VdbRequestQueue vdbRequestQueue,
                                                      Clip clip,
                                                      OnViewDragListener listener) {
        Bundle args = new Bundle();
        CameraVideoPlayFragment fragment = new CameraVideoPlayFragment();
        fragment.setArguments(args);
        fragment.mVdbRequestQueue = vdbRequestQueue;
        fragment.mClip = clip;
        fragment.mPlayMode = MODE_SINGLE_CLIP;
        fragment.mTypedState.put(RawDataItem.DATA_TYPE_OBD, RAW_DATA_STATE_UNKNOWN);
        fragment.mTypedState.put(RawDataItem.DATA_TYPE_ACC, RAW_DATA_STATE_UNKNOWN);
        fragment.mTypedState.put(RawDataItem.DATA_TYPE_GPS, RAW_DATA_STATE_UNKNOWN);
        fragment.mDragListener = listener;
        return fragment;
    }

    public static CameraVideoPlayFragment newInstance(VdbRequestQueue vdbRequestQueue,
                                                      Playlist mPlaylist,
                                                      OnViewDragListener listener) {
        Bundle args = new Bundle();
        CameraVideoPlayFragment fragment = new CameraVideoPlayFragment();
        fragment.setArguments(args);
        fragment.mVdbRequestQueue = vdbRequestQueue;
        fragment.mPlayList = mPlaylist;
        fragment.mPlayMode = MODE_PLAYLIST;
        fragment.mOverlayShouldDisplay = false;
//        fragment.mClip = clip;
//        fragment.mTypedState.put(RawDataItem.DATA_TYPE_OBD, RAW_DATA_STATE_UNKNOWN);
//        fragment.mTypedState.put(RawDataItem.DATA_TYPE_ACC, RAW_DATA_STATE_UNKNOWN);
//        fragment.mTypedState.put(RawDataItem.DATA_TYPE_GPS, RAW_DATA_STATE_UNKNOWN);
        fragment.mDragListener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!isRawDataReady()) {
            loadRawData();
        } else {
            loadPlayURL();
        }
        openAudio();
    }

    @Override
    public void onStop() {
        super.onStop();
        releaseAudioPlayer();
    }

    @Override
    public void onDestroyView() {
        //mVdbRequestQueue.cancelAll(REQUEST_TAG);
        super.onDestroyView();
    }

    @Override
    void release(boolean clearTargetState) {
        super.release(clearTargetState);
        releaseAudioPlayer();
    }

    @Override
    protected void openVideo() {
        if (shouldPlayAudio()) {
            openAudio();
        }
        super.openVideo();
    }

    void releaseAudioPlayer() {
        if (mAudioPlayer != null) {
            mAudioPlayer.reset();
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
    }

    public void setBackgroundMusic(String audioPath) {
        mAudioPath = audioPath;
    }

    void openAudio() {
        if (mAudioPath == null) {
            return;
        }
        mAudioPlayer = new MediaPlayer();
        try {
            mAudioPlayer.setDataSource(mAudioPath);
            mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    onAudioPrepared(mp);
                }
            });
            mAudioPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("test", "", e);
        }

    }

    @Override
    void pauseVideo() {
        super.pauseVideo();
        if (mAudioPlayer != null && mAudioPlayer.isPlaying()) {
            mAudioPlayer.pause();
        }
    }

    @Override
    void start() {
        if (shouldPlayAudio()) {
            if (isInPlaybackState() && isInAudioPlayState()) {
                mute(true);
                super.start();
                mAudioPlayer.start();
            }
        } else {
            mute(false);
            super.start();
        }
    }

    boolean isInAudioPlayState() {
        return mAudioPath != null
            && mAudioPlayer != null
            && isAudioPrepared;
    }

    boolean shouldPlayAudio() {
        return mAudioPath != null;
    }

    void onAudioPrepared(MediaPlayer mp) {
        isAudioPrepared = true;
        start();
    }


    protected void setProgress(int currentPosition, int duration) {
        // TODO:
        int position = currentPosition;

        if (mPlayMode == MODE_SINGLE_CLIP) {
            if (mPlaybackUrl.realTimeMs != 0
                && mInitPosition == 0
                && currentPosition != 0
                && Math.abs(mPlaybackUrl.realTimeMs - currentPosition) < 200) {
                mInitPosition = mPlaybackUrl.realTimeMs;
                Logger.t(TAG).d("setProgress - deviation: " + Math.abs(mPlaybackUrl
                    .realTimeMs - currentPosition));
            }

            if (duration > 0) {
                //Log.e("test", "setProgress - position: " + position + "; real: "
                //        + mPlaybackUrl.realTimeMs + "; duration2: " + mPlaybackUrl.lengthMs);
                if (mInitPosition == 0) {
                    position = currentPosition + (int) mPlaybackUrl.realTimeMs;
                }
            }
        }
        if (mOverlayShouldDisplay) {
            displayOverlay(position);
        }

        if (mProgressBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = MAX_PROGRESS * position / duration;
                mProgressBar.setProgress((int) pos);
            }
        }
        if (mProgressListener != null) {
            Logger.t(TAG).d("currentPosition: " + currentPosition + " duration: " + duration);
            mProgressListener.onProgress(position, duration);
        }

    }

    @Override
    protected void displayOverlay(int position) {

    }

    RawDataItem getRawData(int dataType, int position) {
        RawDataBlock raw = mTypedRawData.get(dataType);
        if (raw == null) {
            return null;
        }
        int pos = mTypedPosition.get(dataType);
        RawDataItem rawDataItem = null;
        while (pos < raw.dataSize.length) {
            RawDataItem tmp = raw.getRawDataItem(pos);
            long timeOffsetMs = raw.timeOffsetMs[pos] + raw.header.mRequestedTimeMs;
            if (timeOffsetMs == position) {
                rawDataItem = tmp;
                mTypedPosition.put(dataType, pos);
                break;
            } else if (timeOffsetMs < position) {
                rawDataItem = tmp;
                mTypedPosition.put(dataType, pos);
                pos++;
            } else if (timeOffsetMs > position) {
                break;
            }
        }
        return rawDataItem;
    }

    @Override
    protected void onPlayCompletion() {
        mTypedPosition.clear();
    }

    boolean isRawDataReady() {
        if (mPlayMode == MODE_PLAYLIST) {
            return true;
        }
        return mTypedState.get(RawDataItem.DATA_TYPE_OBD) == RAW_DATA_STATE_READY
            && mTypedState.get(RawDataItem.DATA_TYPE_ACC) == RAW_DATA_STATE_READY
            && mTypedState.get(RawDataItem.DATA_TYPE_GPS) == RAW_DATA_STATE_READY;
    }

    void loadRawData() {
        mProgressLoading.setVisibility(View.VISIBLE);
        if (mTypedState.get(RawDataItem.DATA_TYPE_OBD) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataItem.DATA_TYPE_OBD);
        }

        if (mTypedState.get(RawDataItem.DATA_TYPE_ACC) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataItem.DATA_TYPE_ACC);
        }
        if (mTypedState.get(RawDataItem.DATA_TYPE_GPS) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataItem.DATA_TYPE_GPS);
        }
    }

    void loadRawData(final int dataType) {
        if (mClip == null || mVdbRequestQueue == null) {
            mRawDataState = RAW_DATA_STATE_ERROR;
            return;
        }

        ClipFragment clipFragment = new ClipFragment(mClip);
        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, clipFragment.getStartTimeMs());
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, clipFragment.getDurationMs());

        RawDataBlockRequest obdRequest = new RawDataBlockRequest(clipFragment.getClip().cid, params,
            new VdbResponse.Listener<RawDataBlock>() {
                @Override
                public void onResponse(RawDataBlock response) {
                    Logger.t(TAG).d("resoponse datatype: " + dataType);
                    mTypedRawData.put(dataType, response);
                    mTypedState.put(dataType, RAW_DATA_STATE_READY);
                    onLoadRawDataFinished();
                }
            },
            new VdbResponse.ErrorListener() {
                @Override
                public void onErrorResponse(SnipeError error) {
                    mTypedState.put(dataType, RAW_DATA_STATE_ERROR);
                    onLoadRawDataFinished();
                    Logger.t(TAG).d("error response:");
                }
            });
        mVdbRequestQueue.add(obdRequest.setTag(REQUEST_TAG));
    }

    void onLoadRawDataFinished() {
        if (mTypedState.get(RawDataItem.DATA_TYPE_OBD) == RAW_DATA_STATE_UNKNOWN
            || mTypedState.get(RawDataItem.DATA_TYPE_ACC) == RAW_DATA_STATE_UNKNOWN
            || mTypedState.get(RawDataItem.DATA_TYPE_GPS) == RAW_DATA_STATE_UNKNOWN) {
            return;
        }
        mRawDataState = RAW_DATA_STATE_READY;
        loadPlayURL();
    }

    private void loadPlayURL() {
        if (mProgressLoading.getVisibility() != View.VISIBLE) {
            mProgressLoading.setVisibility(View.VISIBLE);
        }
        if (mPlayMode == MODE_SINGLE_CLIP) {
            loadPlaySingleClip();
        } else {
            loadPlaylist();
        }
    }

    private void loadPlaySingleClip() {

        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, Vdb.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, Vdb.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTimeMs());

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(mClip.cid, parameters, new VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl playbackUrl) {
                mPlaybackUrl = playbackUrl;
                setSource(playbackUrl.url);
                mProgressLoading.setVisibility(View.GONE);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                mProgressLoading.setVisibility(View.GONE);
                Log.e("test", "", error);
            }
        });

        mVdbRequestQueue.add(request.setTag(REQUEST_TAG));
    }

    private void loadPlaylist() {
        PlaylistPlaybackUrlRequest request = new PlaylistPlaybackUrlRequest(mPlayList,
            0, new VdbResponse.Listener<PlaylistPlaybackUrl>() {
            @Override
            public void onResponse(PlaylistPlaybackUrl response) {
                mPlaybackListUrl = response;
                Logger.t(TAG).d("Get playlist: " + response.url);
                setSource(response.url);
                mProgressLoading.setVisibility(View.GONE);
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                mProgressLoading.setVisibility(View.GONE);
            }
        });
        mVdbRequestQueue.add(request.setTag(REQUEST_TAG));
    }
}