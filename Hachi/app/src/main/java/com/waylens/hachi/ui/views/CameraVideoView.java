package com.waylens.hachi.ui.views;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.transee.vdb.VdbClient;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbImageLoader;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlExRequest;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.ui.entities.SharableClip;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.views.dashboard.adapters.RawDataAdapter;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Richard on 1/18/16.
 */
public class CameraVideoView extends VideoPlayView {

    protected static final String REQUEST_TAG = "RETRIEVE_CAMERA_CLIP_DATA";

    SharableClip mSharableClip;

    VdbRequestQueue mVdbRequestQueue;

    VdbImageLoader mVdbImageLoader;

    PlaybackUrl mPlaybackUrl;

    Thread bgThread;

    CameraRawDataBlockAdapter mRawDataBlockAdapter;

    SparseArray<RawDataBlock> mTypedRawData = new SparseArray<>();

    public CameraVideoView(Context context) {
        this(context, null, 0);
    }

    public CameraVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void cleanup() {
        if (mVdbRequestQueue != null) {
            mVdbRequestQueue.cancelAll(REQUEST_TAG);
        }
        if (bgThread != null && bgThread.isAlive()) {
            bgThread.interrupt();
        }
    }

    public void initVideoPlay(VdbRequestQueue vdbRequestQueue, SharableClip sharableClip) {
        mVdbRequestQueue = vdbRequestQueue;
        mVdbImageLoader = VdbImageLoader.getImageLoader(mVdbRequestQueue);
        mBtnPlay.setVisibility(VISIBLE);
        updateSharableClip(sharableClip, 0);
        mRawDataBlockAdapter = new CameraRawDataBlockAdapter();

    }

    public void updateSharableClip(SharableClip sharableClip, int seekTo) {
        //clean exist clip info
        mPlaybackUrl = null;
        mTypedRawData.clear();

        mSharableClip = sharableClip;
        ClipPos clipPos = sharableClip.getThumbnailClipPos(mSharableClip.clip.getStartTimeMs());
        mVdbImageLoader.displayVdbImage(clipPos, videoCover);
        release(true);
        if (seekTo > 0) {
            seekTo(seekTo);
        }
    }

    public void updateThumbnail(ClipPos clipPos) {
        if (videoCover == null) {
            return;
        }
        if (videoCover.getVisibility() != VISIBLE) {
            videoCover.setVisibility(VISIBLE);
        }
        mVdbImageLoader.displayVdbImage(clipPos, videoCover, true, false);
    }

    @Override
    protected void onClickPlayButton() {
        if (mPlaybackUrl == null) {
            loadVideoInfo();
        } else {
            playVideo();
        }
    }

    @Override
    protected void updateInternalProgress(int position, int duration) {
        if (mRawDataBlockAdapter != null) {
            mRawDataBlockAdapter.refresh(position);
        }
    }

    void loadPlayURL(final CountDownLatch latch) {
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mSharableClip.selectedStartValue);
        parameters.putInt(ClipPlaybackUrlExRequest.PARAMETER_CLIP_LENGTH_MS, mSharableClip.getSelectedLength());

        ClipPlaybackUrlExRequest request = new ClipPlaybackUrlExRequest(mSharableClip.bufferedCid, parameters, new VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl playbackUrl) {
                mPlaybackUrl = playbackUrl;
                if (latch == null) {
                    setSource(playbackUrl.url, VideoPlayView.STATE_PLAYING);
                } else {
                    latch.countDown();
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e("test", "", error);
                latch.countDown();
            }
        });

        mVdbRequestQueue.add(request.setTag(REQUEST_TAG));
    }

    void loadVideoInfo() {
        mBtnPlay.setVisibility(INVISIBLE);
        if (mLoadingIcon.getVisibility() != VISIBLE) {
            mLoadingIcon.setVisibility(VISIBLE);
        }

        bgThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final CountDownLatch latch = new CountDownLatch(4);
                loadRawData(RawDataItem.DATA_TYPE_GPS, latch);
                loadRawData(RawDataItem.DATA_TYPE_ACC, latch);
                loadRawData(RawDataItem.DATA_TYPE_OBD, latch);
                loadPlayURL(latch);
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Log.e("test", "", e);
                    return;
                }
                onVideoInfoFinish();
            }
        }, "get-raw-data-thread");
        bgThread.start();
    }

    void onVideoInfoFinish() {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPlaybackUrl == null) {
                    mLoadingIcon.setVisibility(INVISIBLE);
                    mBtnPlay.setVisibility(VISIBLE);
                    return;
                }
                setSource(mPlaybackUrl.url, VideoPlayView.STATE_PLAYING);
                showOverlay();
            }
        });
    }

    void showOverlay() {
        if (mTypedRawData.size() > 0) {
            mRawDataBlockAdapter.setRawDataBlock(mTypedRawData);
            mOverlayLayout.setAdapter(mRawDataBlockAdapter);
            mOverlayLayout.setVisibility(VISIBLE);
        }
    }

    void loadRawData(final int dataType, final CountDownLatch latch) {
        if (mSharableClip == null || mVdbRequestQueue == null) {
            mRawDataState = RAW_DATA_STATE_ERROR;
            return;
        }

        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, mSharableClip.selectedStartValue);
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, mSharableClip.getSelectedLength());

        RawDataBlockRequest obdRequest = new RawDataBlockRequest(mSharableClip.bufferedCid, params,
                new VdbResponse.Listener<RawDataBlock>() {
                    @Override
                    public void onResponse(RawDataBlock response) {
                        mTypedRawData.put(dataType, response);
                        Log.e("test", "response: " + response);
                        latch.countDown();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        Log.e("test", "", error);
                        latch.countDown();
                    }
                });
        mVdbRequestQueue.add(obdRequest.setTag(REQUEST_TAG));
    }

    static class CameraRawDataBlockAdapter extends RawDataAdapter {

        SparseArray<RawDataBlock> mRawData = new SparseArray<>();
        SparseIntArray mPositions = new SparseIntArray();

        public void setRawDataBlock(SparseArray<RawDataBlock> rawData) {
            mRawData = rawData;
        }

        public void reset() {
            mPositions.clear();
        }

        public void refresh(int position) {
            RawDataItem gps = getRawData(RawDataItem.DATA_TYPE_GPS, position);
            if (gps != null) {
                notifyDataSetChanged(gps);
            }

            RawDataItem acc = getRawData(RawDataItem.DATA_TYPE_ACC, position);
            if (acc != null) {
                notifyDataSetChanged(acc);
            }

            RawDataItem obd = getRawData(RawDataItem.DATA_TYPE_OBD, position);
            if (obd != null) {
                notifyDataSetChanged(obd);
            }

        }

        RawDataItem getRawData(int dataType, int position) {
            RawDataBlock raw = mRawData.get(dataType);
            if (raw == null) {
                return null;
            }
            int pos = mPositions.get(dataType);
            RawDataItem rawDataItem = null;
            while (pos < raw.dataSize.length) {
                RawDataItem tmp = raw.getRawDataItem(pos);
                long timeOffsetMs = raw.timeOffsetMs[pos] + raw.header.mRequestedTimeMs;
                if (timeOffsetMs == position) {
                    rawDataItem = tmp;
                    mPositions.put(dataType, pos);
                    break;
                } else if (timeOffsetMs < position) {
                    rawDataItem = tmp;
                    mPositions.put(dataType, pos);
                    pos++;
                } else if (timeOffsetMs > position) {
                    break;
                }
            }
            return rawDataItem;
        }
    }
}
