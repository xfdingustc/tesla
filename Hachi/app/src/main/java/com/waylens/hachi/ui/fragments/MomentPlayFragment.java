package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.entities.Moment;
import com.waylens.hachi.ui.entities.MomentAcc;
import com.waylens.hachi.ui.entities.MomentOBD;
import com.waylens.hachi.utils.ServerMessage;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.views.DragLayout;
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

    GaugeView mObdView;

    int mOBDPosition;

    public static MomentPlayFragment newInstance(Moment moment, DragLayout.OnViewDragListener listener) {
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
        mRequestQueue = Volley.newRequestQueue(getActivity());
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
    public void onDestroy() {
        super.onDestroy();
        mMomentOBD.clear();
        mMomentAcc.clear();
        mRawDataState = RAW_DATA_STATE_UNKNOWN;
    }

    @Override
    protected void displayOverlay(int position) {
        if (mMomentOBD.size() == 0) {
            return;
        }
        MomentOBD obd = getODB(position);
        if(obd != null) {
            mObdView.setSpeed(obd.speed);
            mObdView.setTargetValue(obd.rpm / 1000.0f);
        } else {
            Log.e("test", "Position: " + position + "; mOBDPosition: " + mOBDPosition);
        }
    }

    @Override
    protected void onPlayCompletion() {
        mOBDPosition = 0;
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

        mRawDataState = RAW_DATA_STATE_READY;
        mProgressLoading.setVisibility(View.GONE);
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
        }));
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
                    }));
        } catch (JSONException e) {
            Log.e("test", "", e);
        }
    }

    boolean parseRawData(JSONObject response) {
        try {
            JSONObject obd = response.getJSONObject("obd");
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
            JSONObject acc = response.getJSONObject("acc");
            captureTime = acc.getJSONArray("captureTime");
            JSONArray acceleration = acc.getJSONArray("acceleration");
            for (int i = 0; i < captureTime.length(); i++) {
                JSONObject accObj = acceleration.getJSONObject(i);
                mMomentAcc.add(new MomentAcc(
                        captureTime.getLong(i),
                        accObj.getInt("x"),
                        accObj.getInt("y"),
                        accObj.getInt("z")
                ));
            }
            return true;
        } catch (JSONException e) {
            Log.e("test", "", e);
            return false;
        }
    }
}
