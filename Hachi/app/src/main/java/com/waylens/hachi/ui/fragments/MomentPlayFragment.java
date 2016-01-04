package com.waylens.hachi.ui.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.annotations.SpriteFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.MomentAcc;
import com.waylens.hachi.ui.entities.MomentGPS;
import com.waylens.hachi.ui.entities.MomentOBD;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.views.GaugeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Play Moment video
 * Created by Richard on 11/3/15.
 */
public class MomentPlayFragment extends VideoPlayFragment {

    public static final int ODB_DELAY = 100;

    long mMomentID = Moment.INVALID_MOMENT_ID;
    RequestQueue mRequestQueue;
    JSONArray mRawDataUrls;

    ArrayList<MomentOBD> mMomentOBD = new ArrayList<>();
    ArrayList<MomentAcc> mMomentAcc = new ArrayList<>();
    ArrayList<MomentGPS> mMomentGPS = new ArrayList<>();

    GaugeView mObdView;
    MapView mMapView;

    int mOBDPosition;
    private MarkerOptions mMarkerOptions;
    private PolylineOptions mPolylineOptions;

    private int mGPSPosition;
    private boolean mIsReplay;

    public static MomentPlayFragment newInstance(Moment moment, OnViewDragListener listener) {
        Bundle args = new Bundle();
        MomentPlayFragment fragment = new MomentPlayFragment();
        fragment.setArguments(args);
        fragment.mMomentID = moment.id;
        fragment.setSource(moment.videoURL);
        //fragment.setSource("http://devimages.apple.com/iphone/samples/bipbop/gear2/prog_index.m3u8");
        //fragment.setSource("http://monitor.vidit.com.cn:8083/waylens/hls/s2.m3u8");
        /*
        HashMap<String, String> headers = new HashMap<>();
        String cookie = "CloudFront-Policy=eyJTdGF0ZW1lbnQiOiBbeyJSZXNvdXJjZSI6Imh0dHBzOi8vZDE1eWo3dGs3ajA0N3UuY2xvdWRmcm9udC5uZXQvKiIsIkNvbmRpdGlvbiI6eyJEYXRlTGVzc1RoYW4iOnsiQVdTOkVwb2NoVGltZSI6MTQ0ODQ5MDAwMH0sIklwQWRkcmVzcyI6eyJBV1M6U291cmNlSXAiOiIwLjAuMC4wLzAifSwiRGF0ZUdyZWF0ZXJUaGFuIjp7IkFXUzpFcG9jaFRpbWUiOjE0NDgzNDY3MTZ9fX1dfQ__; Path=/; Domain=cloudfront.net; Secure; HTTPOnly;"
                + "CloudFront-Signature=K6z6ZN5EFVOLkIpbG2jVEq8gdi62qX7JL--rpgSCQ00f-LfWLyTnOrohqB~ItulFax1TYoaqFi4HFch0yoHqhInloGwL9NVAYxiFCd2e-1nBHDSXi80OwT9qY-KQ9efB8W8y8QRC-g0y38UDIy2xuG2sSoSE~Pyb-1p9QMZpaFvXTXuYIQTfZRSDPw-dELWjUaG8hX4X~9As1aCrPriDTqJqtEDijwgXzaRjQDcEsgus7-d~OT0yoDgDS-uUbkeshSdCQJvtHrNgVXu2ep5bHxD7~lWhJa1pdnMx~~XA20azpzJdwmpCrkPRjW9Gt8noL1B~Mf2mcit9GzXBZsKV1g__; Path=/; Domain=cloudfront.net; Secure; HTTPOnly;"
                + "CloudFront-Key-Pair-Id=APKAIZCLN7KQEMMEJXJQ; Path=/; Domain=cloudfront.net; Secure; HTTPOnly";
        headers.put("Cookie", cookie);
        */
        //fragment.setSource("https://d2o7nk9c8hjp89.cloudfront.net/public/8119898bbc8c44f09ad1910a5f6bfaa3/195/play_6.m3u8", null);
        fragment.mDragListener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
        mRequestQueue.start();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mRawDataState != RAW_DATA_STATE_READY) {
            readRawURL();
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
    public void onDestroyView() {
        mRequestQueue.cancelAll(REQUEST_TAG);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMomentOBD.clear();
        mMomentAcc.clear();
        mRawDataState = RAW_DATA_STATE_UNKNOWN;
    }

    protected void setProgress(int position, int duration) {
        //Log.e("test", "duration: " + duration + "; position: " + position);
        if (mProgressBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = MAX_PROGRESS * position / duration;
                mProgressBar.setProgress((int) pos);
            }
            //int percent = mMediaPlayer.getBufferPercentage();
            //mProgress.setSecondaryProgress(percent * 10);
        }
        updateVideoTime(position, duration);
        displayOverlay(position);
        if (mProgressListener != null) {
            mProgressListener.onProgress(position, duration);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateVideoTime(int position, int duration) {
        mVideoTime.setText(DateUtils.formatElapsedTime(position / 1000) + " / " + DateUtils.formatElapsedTime(duration / 1000));
    }

    @Override
    protected void displayOverlay(int position) {
        if (mMomentOBD.size() > 0) {
            MomentOBD obd = getODB(position);
            if (obd != null && mObdView != null) {
                mObdView.setSpeed(obd.speed);
                mObdView.setTargetValue(obd.rpm / 1000.0f);
            } else {
                Log.e("test", "Position: " + position + "; mOBDPosition: " + mOBDPosition);
            }
        }

        if (mMomentGPS.size() > 0) {
            if (mIsReplay) {
                mIsReplay = false;
                mPolylineOptions = new PolylineOptions().color(Color.rgb(252, 219, 12)).width(3);
            }
            MomentGPS gps = getGPS(position);
            if (gps != null && mMapView != null) {
                mMapView.removeAllAnnotations();
                LatLng point = new LatLng(gps.latitude, gps.longitude);
                mMarkerOptions.position(point);
                mPolylineOptions.add(point);
                mMapView.addMarker(mMarkerOptions);
                mMapView.addPolyline(mPolylineOptions);
                mMapView.setCenterCoordinate(point);
            }
        }
    }

    private MomentGPS getGPS(int position) {
        MomentGPS gps = null;
        while (mGPSPosition < mMomentGPS.size()) {
            MomentGPS tmp = mMomentGPS.get(mGPSPosition);
            if (tmp.captureTime == position) {
                gps = tmp;
                break;
            } else if (tmp.captureTime < position) {
                gps = tmp;
                mGPSPosition++;
            } else if (tmp.captureTime > position) {
                break;
            }
        }
        return gps;
    }

    @Override
    protected void onPlayCompletion() {
        mOBDPosition = 0;
        mGPSPosition = 0;
        mIsReplay = true;
    }

    MomentOBD getODB(int position) {
        MomentOBD obd = null;
        while (mOBDPosition < mMomentOBD.size()) {
            MomentOBD tmp = mMomentOBD.get(mOBDPosition);
            if (tmp.captureTime == position) {
                obd = tmp;
                break;
            } else if (tmp.captureTime < position) {
                obd = tmp;
                mOBDPosition++;
            } else if (tmp.captureTime > position) {
                break;
            }
        }
        return obd;
    }

    void onLoadRawDataSuccessfully() {
        if (mMomentOBD.size() > 0 && mObdView == null) {
            mObdView = new GaugeView(getActivity());
            int defaultSize = ViewUtils.dp2px(64, getResources());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(defaultSize, defaultSize);
            params.gravity = Gravity.BOTTOM | Gravity.END;
            mVideoContainer.addView(mObdView, params);
        }


        if (mMomentGPS.size() > 0 && mMapView == null) {
            initMapView();
        }

        mRawDataState = RAW_DATA_STATE_READY;
        mProgressLoading.setVisibility(View.GONE);
        openVideo();
    }

    private void initMapView() {
        mMapView = new MapView(getActivity(), Constants.MAP_BOX_ACCESS_TOKEN);
        mMapView.setStyleUrl(Style.DARK);
        mMapView.setZoomLevel(14);
        mMapView.setLogoVisibility(View.GONE);
        mMapView.onCreate(null);
        MomentGPS firstGPS = mMomentGPS.get(0);
        SpriteFactory spriteFactory = new SpriteFactory(mMapView);
        LatLng firstPoint = new LatLng(firstGPS.latitude, firstGPS.longitude);
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

    void onLoadRawDataError(String msg) {
        Log.e("test", "msg: " + msg);
        mRawDataState = RAW_DATA_STATE_ERROR;
        mProgressLoading.setVisibility(View.GONE);
        openVideo();
    }

    void readRawURL() {
        if (mMomentID == Moment.INVALID_MOMENT_ID) {
            return;
        }
        String url = Constants.API_MOMENT_PLAY + mMomentID;
        mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                mRawDataUrls = response.optJSONArray("rawDataUrl");
                loadRawData(0);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                onLoadRawDataError("ErrorCode: " + errorInfo.errorCode);
            }
        }).setTag(REQUEST_TAG));
        mProgressLoading.setVisibility(View.VISIBLE);
    }

    void loadRawData(final int index) {
        if (mRawDataUrls == null || index >= mRawDataUrls.length()) {
            onLoadRawDataSuccessfully();
            return;
        }

        try {
            JSONObject jsonObject = mRawDataUrls.getJSONObject(index);
            String url = jsonObject.getString("url");
            mRequestQueue.add(new AuthorizedJsonRequest(Request.Method.GET, url,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            if (parseRawData(response)) {
                                int nextIndex = index + 1;
                                if (nextIndex < mRawDataUrls.length()) {
                                    loadRawData(nextIndex);
                                } else {
                                    onLoadRawDataSuccessfully();
                                }
                            } else {
                                onLoadRawDataError("Load Raw data error");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            ServerMessage.ErrorMsg errorInfo = ServerMessage.parseServerError(error);
                            onLoadRawDataError("ErrorCode: " + errorInfo.errorCode);
                        }
                    }).setTag(REQUEST_TAG));
        } catch (JSONException e) {
            Log.e("test", "", e);
        }
    }

    boolean parseRawData(JSONObject response) {
        try {
            JSONObject obd = response.optJSONObject("obd");
            if (obd != null) {
                JSONArray captureTime = obd.getJSONArray("captureTime");
                JSONArray speed = obd.getJSONArray("speed");
                JSONArray rpm = obd.getJSONArray("rpm");
                JSONArray temperature = obd.getJSONArray("temperature");
                JSONArray tp = obd.getJSONArray("tp");
                JSONArray imp = obd.getJSONArray("imp");
                JSONArray bp = obd.getJSONArray("bp");
                JSONArray bhp = obd.getJSONArray("bhp");
                for (int i = 0; i < captureTime.length(); i++) {
                    mMomentOBD.add(new MomentOBD(
                            captureTime.getLong(i),
                            speed.getInt(i),
                            rpm.getInt(i),
                            temperature.getInt(i),
                            tp.getInt(i),
                            imp.getInt(i),
                            bp.getInt(i),
                            bhp.getInt(i)
                    ));
                }
            }
            JSONObject acc = response.optJSONObject("acc");
            if (acc != null) {
                JSONArray captureTime = acc.getJSONArray("captureTime");
                JSONArray acceleration = acc.getJSONArray("acceleration");
                for (int i = 0; i < captureTime.length(); i++) {
                    JSONObject accObj = acceleration.getJSONObject(i);
                    mMomentAcc.add(new MomentAcc(
                            captureTime.getLong(i),
                            accObj.getInt("accelX"), accObj.getInt("accelY"), accObj.getInt("accelZ"),
                            accObj.getInt("gyroX"), accObj.getInt("gyroY"), accObj.getInt("gyroZ"),
                            accObj.getInt("magnX"), accObj.getInt("magnY"), accObj.getInt("magnZ"),
                            accObj.getInt("eulerHeading"), accObj.getInt("eulerRoll"), accObj.getInt("eulerPitch"),
                            accObj.getInt("quaternionW"), accObj.getInt("quaternionX"), accObj.getInt("quaternionY"), accObj.getInt("quaternionZ"),
                            accObj.getInt("pressure")
                    ));
                }
            }
            JSONObject gps = response.optJSONObject("gps");
            if (gps != null) {
                JSONArray captureTime = gps.getJSONArray("captureTime");
                JSONArray coordinates = gps.getJSONObject("coordinate").getJSONArray("coordinates");

                for (int i = 0; i < captureTime.length(); i++) {
                    JSONArray coordinateObj = coordinates.getJSONArray(i);
                    mMomentGPS.add(new MomentGPS(
                            captureTime.getLong(i),
                            coordinateObj.getDouble(0),
                            coordinateObj.getDouble(1),
                            coordinateObj.getDouble(2)
                    ));
                }
            }

            return true;
        } catch (JSONException e) {
            Log.e("test", "", e);
            return false;
        }
    }
}
