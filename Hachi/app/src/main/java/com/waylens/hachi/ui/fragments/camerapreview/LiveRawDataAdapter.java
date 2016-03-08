package com.waylens.hachi.ui.fragments.camerapreview;

import android.util.Log;
import android.webkit.WebView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.LiveRawDataRequest;
import com.waylens.hachi.snipe.toolbox.RawDataMsgHandler;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;


/**
 * Created by Xiaofei on 2015/12/18.
 */
public class LiveRawDataAdapter {
    private static final String TAG = LiveRawDataAdapter.class.getSimpleName();

    private final VdbRequestQueue mVdbRequestQueue;
    private final WebView mGaugeView;

    public LiveRawDataAdapter(VdbRequestQueue mVdbRequestQueue, WebView gaugeView) {
        this.mVdbRequestQueue = mVdbRequestQueue;
        //this.mRawDataListener = listener;
        this.mGaugeView = gaugeView;
        init();
    }

    private void init() {
        LiveRawDataRequest request = new LiveRawDataRequest(RawDataBlock.F_RAW_DATA_GPS +
            RawDataBlock.F_RAW_DATA_ACC + RawDataBlock.F_RAW_DATA_ODB, new
            VdbResponse.Listener<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    Logger.t(TAG).d("LiveRawDataResponse: " + response);
                }
            }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e(TAG, "LiveRawDataResponse ERROR", error);
            }
        });

        mVdbRequestQueue.add(request);
        RawDataMsgHandler rawDataMsgHandler = new RawDataMsgHandler(new VdbResponse.Listener<RawDataItem>() {
            @Override
            public void onResponse(RawDataItem response) {
//                Logger.t(TAG).d(String.format("RawDataMsgHandler: Type[%d]:[%s]", response
//                    .dataType, response.data));

                updateGaugeView(response);
            }

        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {
                Log.e(TAG, "RawDataMsgHandler ERROR", error);
            }
        });
        mVdbRequestQueue.registerMessageHandler(rawDataMsgHandler);
    }



    private void updateGaugeView(RawDataItem item) {
        JSONObject state = new JSONObject();
        String data = null;
        try {
            switch (item.getType()) {
                case RawDataItem.DATA_TYPE_ACC:
                    RawDataItem.AccData accData = (RawDataItem.AccData) item.data;
                    state.put("roll", -accData.euler_roll);
                    state.put("pitch", -accData.euler_pitch);
                    state.put("gforceBA", accData.accX);
                    state.put("gforceLR", accData.accZ);
                    break;
                case RawDataItem.DATA_TYPE_GPS:
                    RawDataItem.GpsData gpsData = (RawDataItem.GpsData) item.data;
                    state.put("lng", gpsData.coord.lng);
                    state.put("lat", gpsData.coord.lat);
                    break;
                case RawDataItem.DATA_TYPE_OBD:
                    RawDataItem.OBDData obdData = (RawDataItem.OBDData) item.data;
                    state.put("rpm", obdData.rpm);
                    state.put("mph", obdData.speed);
                    break;
            }
//            state.put("rpm", 5000);
//            state.put("roll", -2000);
//            state.put("pitch", -50);
            SimpleDateFormat format = new SimpleDateFormat("MM dd, yyyy hh:mm:ss");
            String date = format.format(System.currentTimeMillis());
            data = "numericMonthDate('" + date + "')";
            //Log.e("test", "date: " + data);
            //state.put("time", data);
        } catch (JSONException e) {
            Log.e("test", "", e);
        }

        String callJS1 = "javascript:setState(" + state.toString() + ")";
        String callJS2 = "javascript:setState(" + "{time:" + data + "})";
//        Logger.t(TAG).d("callJS: " + callJS1);
        mGaugeView.loadUrl(callJS1);
        mGaugeView.loadUrl(callJS2);
        mGaugeView.loadUrl("javascript:update()");
    }

}
