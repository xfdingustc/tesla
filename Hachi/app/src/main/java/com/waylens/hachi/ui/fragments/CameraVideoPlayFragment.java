package com.waylens.hachi.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.annotations.SpriteFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.transee.common.GPSRawData;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.ClipPlaybackUrlRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
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
    MapView mMapView;

    private MarkerOptions mMarkerOptions;
    private PolylineOptions mPolylineOptions;
    private boolean mIsReplay;

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
        if (mMapView != null) {
            mMapView.onStart();
        }
    }

    @Override
    public void onStop() {
        if (mMapView != null) {
            mMapView.onStop();
        }
        super.onStop();
    }

    @Override
    protected void displayOverlay(int position) {
        if (mObdView == null) {
            return;
        }
        RawDataItem obd = getRawData(RawDataBlock.RAW_DATA_ODB, position);
        if (obd != null) {
            mObdView.setSpeed(((OBDData) obd.object).speed);
            mObdView.setTargetValue(((OBDData) obd.object).rpm / 1000.0f);
        } else {
            Log.e("test", "Position: " + position + "; mOBDPosition: " + mTypedPosition.get(RawDataBlock.RAW_DATA_ODB));
        }

        RawDataItem gps = getRawData(RawDataBlock.RAW_DATA_GPS, position);
        if (gps != null) {
            if (mIsReplay) {
                mIsReplay = false;
                mPolylineOptions = new PolylineOptions().color(Color.rgb(252, 219, 12)).width(3);
            }
            GPSRawData gpsRawData = (GPSRawData) gps.object;
            mMapView.removeAllAnnotations();
            LatLng point = new LatLng(gpsRawData.coord.lat_orig, gpsRawData.coord.lng_orig);
            mMarkerOptions.position(point);
            mPolylineOptions.add(point);
            mMapView.addMarker(mMarkerOptions);
            mMapView.addPolyline(mPolylineOptions);
            mMapView.setCenterCoordinate(point);
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
        mIsReplay = true;
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

        Log.e("test", "DataType[1]: " + dataType);
        RawDataBlockRequest obdRequest = new RawDataBlockRequest(mClip, mClip.getStartTimeMs(),
            mClip.getDurationMs(),
                dataType,
                new VdbResponse.Listener<RawDataBlock>() {
                    @Override
                    public void onResponse(RawDataBlock response) {
                        Log.e("test", "DataType[2]: " + dataType);
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
                        Log.e("test", "", error);
                    }
                });
        mVdbRequestQueue.add(obdRequest);
    }

    void onLoadRawDataFinished() {
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

        if (mTypedRawData.get(RawDataBlock.RAW_DATA_GPS) != null && mMapView == null) {
            initMapView();
        }
    }

    private void initMapView() {
        mMapView = new MapView(getActivity(), Constants.MAP_BOX_ACCESS_TOKEN);
        mMapView.setStyleUrl(Style.DARK);
        mMapView.setZoomLevel(14);
        mMapView.setLogoVisibility(View.GONE);
        mMapView.onCreate(null);
        GPSRawData firstGPS = (GPSRawData) mTypedRawData.get(RawDataBlock.RAW_DATA_GPS).getRawDataItem(0).object;
        SpriteFactory spriteFactory = new SpriteFactory(mMapView);
        LatLng firstPoint = new LatLng(firstGPS.coord.lat_orig, firstGPS.coord.lng_orig);
        mMarkerOptions = new MarkerOptions().position(firstPoint)
                .icon(spriteFactory.fromResource(R.drawable.map_car_inner_red_triangle));
        mMapView.addMarker(mMarkerOptions);
        mPolylineOptions = new PolylineOptions().color(Color.rgb(252, 219, 12)).width(3).add(firstPoint);
        mMapView.setCenterCoordinate(firstPoint);
        mMapView.addPolyline(mPolylineOptions);

        int defaultSize = ViewUtils.dp2px(96, getResources());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(defaultSize, defaultSize);
        mVideoContainer.addView(mMapView, params);
    }

    void loadPlayURL() {
        if (mProgressLoading.getVisibility() != View.VISIBLE) {
            mProgressLoading.setVisibility(View.VISIBLE);
        }
        Bundle parameters = new Bundle();
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_URL_TYPE, VdbClient.URL_TYPE_HLS);
        parameters.putInt(ClipPlaybackUrlRequest.PARAMETER_STREAM, VdbClient.STREAM_SUB_1);
        parameters.putBoolean(ClipPlaybackUrlRequest.PARAMETER_MUTE_AUDIO, false);
        parameters.putLong(ClipPlaybackUrlRequest.PARAMETER_CLIP_TIME_MS, mClip.getStartTimeMs());

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
