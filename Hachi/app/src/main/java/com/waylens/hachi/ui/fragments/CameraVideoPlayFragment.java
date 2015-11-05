package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.transee.vdb.VdbClient;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.ui.entities.MomentOBD;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.PlaybackUrl;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.views.DragLayout;
import com.waylens.hachi.views.GaugeView;

/**
 * Created by Richard on 11/4/15.
 */
public class CameraVideoPlayFragment extends VideoPlayFragment {

    private VdbRequestQueue mVdbRequestQueue;
    private Clip mClip;

    SparseArray<RawDataBlock> mTypedRawData = new SparseArray<>();
    SparseIntArray mTypedState = new SparseIntArray();
    SparseIntArray mTypedPosition = new SparseIntArray();
    GaugeView mObdView;

    public static CameraVideoPlayFragment newInstance(VdbRequestQueue vdbRequestQueue,
                                                      Clip clip,
                                                      DragLayout.OnViewDragListener listener) {
        Bundle args = new Bundle();
        CameraVideoPlayFragment fragment = new CameraVideoPlayFragment();
        fragment.setArguments(args);
        fragment.mVdbRequestQueue = vdbRequestQueue;
        fragment.mClip = clip;
        fragment.mTypedState.put(RawDataBlock.RAW_DATA_ODB, RAW_DATA_STATE_UNKNOWN);
        fragment.mTypedState.put(RawDataBlock.RAW_DATA_ACC, RAW_DATA_STATE_UNKNOWN);
        fragment.mTypedState.put(RawDataBlock.RAW_DATA_GPS, RAW_DATA_STATE_UNKNOWN);
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
    }

    @Override
    protected void displayOverlay(int position) {
        if (mObdView == null) {
            return;
        }
        RawDataItem obd = getRawData(RawDataBlock.RAW_DATA_ODB, position);
        if(obd != null) {
            mObdView.setSpeed(((OBDData)obd.object).speed);
            mObdView.setTargetValue(((OBDData)obd.object).rpm / 1000.0f);
        } else {
            Log.e("test", "Position: " + position + "; mOBDPosition: " + mTypedPosition.get(RawDataBlock.RAW_DATA_ODB));
        }
    }

    RawDataItem getRawData(int dataType, int position) {
        RawDataBlock raw = mTypedRawData.get(dataType);
        int pos = mTypedPosition.get(dataType);
        RawDataItem rawDataItem = null;
        while (pos < raw.dataSize.length) {
            RawDataItem tmp = raw.getRawDataItem(pos);
            if (raw.timeOffsetMs[pos] == position) {
                rawDataItem = tmp;
                mTypedPosition.put(RawDataBlock.RAW_DATA_ODB, pos);
                break;
            } else if (raw.timeOffsetMs[pos] < position) {
                rawDataItem = tmp;
                mTypedPosition.put(RawDataBlock.RAW_DATA_ODB, pos);
                pos++;
            } else if (raw.timeOffsetMs[pos] > position) {
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
        return mTypedState.get(RawDataBlock.RAW_DATA_ODB) == RAW_DATA_STATE_READY
                && mTypedState.get(RawDataBlock.RAW_DATA_ACC) == RAW_DATA_STATE_READY
                && mTypedState.get(RawDataBlock.RAW_DATA_GPS) == RAW_DATA_STATE_READY;
    }

    void loadRawData() {
        mProgressLoading.setVisibility(View.VISIBLE);
        if (mTypedState.get(RawDataBlock.RAW_DATA_ODB) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataBlock.RAW_DATA_ODB);
        }

        if (mTypedState.get(RawDataBlock.RAW_DATA_ACC) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataBlock.RAW_DATA_ACC);
        }
        if (mTypedState.get(RawDataBlock.RAW_DATA_GPS) != RAW_DATA_STATE_READY) {
            loadRawData(RawDataBlock.RAW_DATA_GPS);
        }
    }

    void loadRawData(final int dataType) {
        if (mClip == null || mVdbRequestQueue == null) {
            mRawDataState = RAW_DATA_STATE_ERROR;
            return;
        }

        RawDataBlockRequest obdRequest = new RawDataBlockRequest(mClip, mClip.getStartTime(), mClip.clipLengthMs,
                dataType,
                new VdbResponse.Listener<RawDataBlock>() {
                    @Override
                    public void onResponse(RawDataBlock response) {
                        mTypedRawData.put(dataType, response);
                        mTypedState.put(dataType, RAW_DATA_STATE_READY);
                        onLoadRawDataSuccessfully();
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        mTypedState.put(dataType, RAW_DATA_STATE_ERROR);
                        Log.e("test", "", error);
                    }
                });
        mVdbRequestQueue.add(obdRequest);
    }

    void onLoadRawDataSuccessfully() {
        if (mTypedState.get(RawDataBlock.RAW_DATA_ODB) == RAW_DATA_STATE_UNKNOWN
                || mTypedState.get(RawDataBlock.RAW_DATA_ACC) == RAW_DATA_STATE_UNKNOWN
                || mTypedState.get(RawDataBlock.RAW_DATA_GPS) == RAW_DATA_STATE_UNKNOWN) {
            return;
        }
        mRawDataState = RAW_DATA_STATE_READY;
        loadPlayURL();

        if (mTypedRawData.get(RawDataBlock.RAW_DATA_ODB) != null && mObdView == null) {
            mObdView = new GaugeView(getActivity());
            int defaultSize = ViewUtils.dp2px(64, getResources());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(defaultSize, defaultSize);
            params.gravity = Gravity.BOTTOM | Gravity.END;
            mVideoContainer.addView(mObdView, params);
        }
    }

    void loadPlayURL() {
        if (mProgressLoading.getVisibility() != View.VISIBLE) {
            mProgressLoading.setVisibility(View.VISIBLE);
        }
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTime());

        ClipPlaybackUrlRequest request = new ClipPlaybackUrlRequest(mClip, parameters, new VdbResponse.Listener<PlaybackUrl>() {
            @Override
            public void onResponse(PlaybackUrl playbackUrl) {
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

        mVdbRequestQueue.add(request);
    }
}
