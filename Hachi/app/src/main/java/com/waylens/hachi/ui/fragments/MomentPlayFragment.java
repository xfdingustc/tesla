package com.waylens.hachi.ui.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.views.OnViewDragListener;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.VolleyUtil;
import com.waylens.hachi.vdb.AccData;
import com.waylens.hachi.vdb.GPSRawData;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.views.dashboard.adapters.RawDataAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Play Moment video
 * Created by Richard on 11/3/15.
 */
public class MomentPlayFragment extends VideoPlayFragment {

    long mMomentID = Moment.INVALID_MOMENT_ID;
    RequestQueue mRequestQueue;
    JSONArray mRawDataUrls;

    private MomentRawDataAdapter mRawDataAdapter = new MomentRawDataAdapter();


    private boolean mIsReplay;

    public static MomentPlayFragment newInstance(Moment moment, OnViewDragListener listener) {
        Bundle args = new Bundle();
        MomentPlayFragment fragment = new MomentPlayFragment();
        fragment.setArguments(args);
        fragment.mMomentID = moment.id;
        fragment.setSource(moment.videoURL);

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
    }

    @Override
    public void onStop() {
        mRawDataAdapter.reset();
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
        mRawDataState = RAW_DATA_STATE_UNKNOWN;
    }

    protected void setProgress(int position, int duration) {
        if (mProgressBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = MAX_PROGRESS * position / duration;
                mProgressBar.setProgress((int) pos);
            }
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
        mRawDataAdapter.updateCurrentTime(position);

    }



    @Override
    protected void onPlayCompletion() {

        mIsReplay = true;
    }


    void onLoadRawDataSuccessfully() {
        mRawDataState = RAW_DATA_STATE_READY;
        mProgressLoading.setVisibility(View.GONE);
        mDashboardLayout.setAdapter(mRawDataAdapter);
        openVideo();
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
                    mRawDataAdapter.addObdData(
                        captureTime.getLong(i),
                        speed.getInt(i),
                        rpm.getInt(i),
                        temperature.getInt(i),
                        tp.getInt(i),
                        imp.getInt(i),
                        bp.getInt(i),
                        bhp.getInt(i));

                }
            }
            JSONObject acc = response.optJSONObject("acc");
            if (acc != null) {
                JSONArray captureTime = acc.getJSONArray("captureTime");
                JSONArray acceleration = acc.getJSONArray("acceleration");
                for (int i = 0; i < captureTime.length(); i++) {
                    JSONObject accObj = acceleration.getJSONObject(i);
                    mRawDataAdapter.addAccData(
                        captureTime.getLong(i),
                        accObj.getInt("accelX"), accObj.getInt("accelY"), accObj.getInt("accelZ"),
                        accObj.getInt("gyroX"), accObj.getInt("gyroY"), accObj.getInt("gyroZ"),
                        accObj.getInt("magnX"), accObj.getInt("magnY"), accObj.getInt("magnZ"),
                        accObj.getInt("eulerHeading"), accObj.getInt("eulerRoll"), accObj.getInt("eulerPitch"),
                        accObj.getInt("quaternionW"), accObj.getInt("quaternionX"), accObj.getInt("quaternionY"), accObj.getInt("quaternionZ"),
                        accObj.getInt("pressure")
                    );
                }
            }
            JSONObject gps = response.optJSONObject("gps");
            if (gps != null) {
                JSONArray captureTime = gps.getJSONArray("captureTime");
                JSONArray coordinates = gps.getJSONObject("coordinate").getJSONArray("coordinates");

                for (int i = 0; i < captureTime.length(); i++) {
                    JSONArray coordinateObj = coordinates.getJSONArray(i);
                    mRawDataAdapter.addGpsData(
                        captureTime.getLong(i),
                        coordinateObj.getDouble(0),
                        coordinateObj.getDouble(1),
                        coordinateObj.getDouble(2)
                    );
                }
            }

            return true;
        } catch (JSONException e) {
            Log.e("test", "", e);
            return false;
        }
    }


    private static class MomentRawDataAdapter extends RawDataAdapter {
        private static final String TAG = MomentRawDataAdapter.class.getSimpleName();
        List<RawDataBlock.RawDataItem> mOBDData = new ArrayList<>();
        List<RawDataBlock.RawDataItem> mAccData = new ArrayList<>();
        List<RawDataBlock.RawDataItem> mGPSData = new ArrayList<>();

        private int mObdDataIndex = 0;
        private int mAccDataIndex = 0;
        private int mGpsDataIndex = 0;

        public void reset() {
            mObdDataIndex = 0;
            mAccDataIndex = 0;
            mGpsDataIndex = 0;
        }


        public void addObdData(long captureTime, int speed, int rpm, int temperature, int tp, int imp, int bp, int bhp) {
            RawDataBlock.RawDataItem item = new RawDataBlock.RawDataItem(RawDataBlock.RawDataItem
                .RAW_DATA_ODB, captureTime);

            OBDData data = new OBDData(speed, temperature, rpm);
            item.object = data;
            mOBDData.add(item);
        }

        public void addAccData(long captureTime, int accX, int accY, int accZ,
                               int gyroX, int gyroY, int gyroZ,
                               int magnX, int magnY, int magnZ,
                               int eulerHeading, int eulerRoll, int eulerPitch,
                               int quaternionW, int quaternionX, int quaternionY, int quaternionZ,
                               int pressure) {
            RawDataBlock.RawDataItem item = new RawDataBlock.RawDataItem(RawDataBlock.RawDataItem
                .RAW_DATA_ACC, captureTime);
            AccData data = new AccData();

            data.accX = accX;
            data.accY = accY;
            data.accZ = accZ;
            data.euler_roll = eulerRoll;
            data.euler_pitch = eulerPitch;

            item.object = data;
            mAccData.add(item);
        }

        public void addGpsData(long captureTime, double longitude, double latitude, double altitude) {
            RawDataBlock.RawDataItem item = new RawDataBlock.RawDataItem(RawDataBlock.RawDataItem
                .RAW_DATA_GPS, captureTime);
            GPSRawData data = new GPSRawData();
            data.coord.lat = latitude;
            data.coord.lat_orig = latitude;
            data.coord.lng = longitude;
            data.coord.lng_orig = longitude;
            data.altitude = altitude;
            item.object = data;

            mGPSData.add(item);
        }

        public void updateCurrentTime(int currentTime) {
            if (checkIfUpdated(mAccData, mAccDataIndex, currentTime)) {
                mAccDataIndex++;
            }
            if (checkIfUpdated(mGPSData, mGpsDataIndex, currentTime)) {
                mGpsDataIndex++;
            }
            if (checkIfUpdated(mOBDData, mObdDataIndex, currentTime)) {
                mObdDataIndex++;
            }
        }

        private boolean checkIfUpdated(List<RawDataBlock.RawDataItem> list, int fromPosition, int currentTime) {
            int index = fromPosition;
            if (index >= list.size()) {
                return false;
            }

            RawDataBlock.RawDataItem item = list.get(index);
            if (item.getPtsMs() < currentTime) {
                fromPosition++;
                notifyDataSetChanged(item);
                return true;
            }

            return false;
        }

        @Override
        public RawDataBlock.RawDataItem getAccDataItem(long pts) {
            return null;
        }

        @Override
        public RawDataBlock.RawDataItem getObdDataItem(long pts) {
            return null;
        }

        @Override
        public RawDataBlock.RawDataItem getGpsDataItem(long pts) {
            return null;
        }
    }
}
