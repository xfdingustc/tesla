package com.waylens.hachi.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.GaugeSettingManager;
import com.waylens.hachi.eventbus.events.GaugeEvent;
import com.waylens.hachi.ui.clips.player.GaugeInfoItem;
import com.waylens.hachi.vdb.rawdata.GpsData;
import com.waylens.hachi.vdb.rawdata.IioData;
import com.waylens.hachi.vdb.rawdata.ObdData;
import com.waylens.hachi.vdb.rawdata.RawDataItem;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class GaugeView extends FrameLayout {
    private static final String TAG = GaugeView.class.getSimpleName();

    private WebView mWebView;

    public GaugeView(Context context) {
        super(context);
        init(context);
    }


    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        if (isInEditMode()) {
            return;
        }
        mWebView = new WebView(context);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mWebView, params);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.loadUrl("file:///android_asset/api.html");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                initGaugeView();
                super.onPageFinished(view, url);
            }
        });

    }



    @Subscribe
    public void onGaugeEvent(GaugeEvent event) {
        switch (event.getWhat()) {
            case GaugeEvent.EVENT_WHAT_SHOW:
                showGauge((Boolean) event.getExtra());
                break;
            case GaugeEvent.EVENT_WHAT_CHANGE_THEME:
                changeGaugeTheme((String) event.getExtra());
                break;
            case GaugeEvent.EVENT_WHAT_UPDATE_SETTING:
                updateGaugeSetting((GaugeInfoItem) event.getExtra());
                break;
        }

    }


    public void initGaugeView() {
        List<GaugeInfoItem> itemList = GaugeSettingManager.getManager().getSetting();
        for (GaugeInfoItem item : itemList) {
            updateGaugeSetting(item);
        }

        changeGaugeTheme(GaugeSettingManager.getManager().getTheme());
    }


    public void showGauge(boolean show) {
        if (show) {
            mWebView.setVisibility(View.VISIBLE);
            initGaugeView();
        } else {
            mWebView.setVisibility(View.INVISIBLE);
        }

    }

    public boolean isGaugeShown() {
        return mWebView.getVisibility() == View.VISIBLE ? true : false;
    }


    private void changeGaugeTheme(String theme) {
        Logger.t(TAG).d("set gauge theme as: " + theme);
        mWebView.loadUrl("javascript:setTheme('" + theme + "')");
    }

    private void updateGaugeSetting(GaugeInfoItem item) {
        String jsApi = "javascript:setGauge('" + item.title + "',";

        if (!item.isEnabled) {
            jsApi += "'')";

        } else {
            if (item.getOption().equals("large")) {
                jsApi += "'L')";
            } else if (item.getOption().equals("middle")) {
                jsApi += "'M')";
            } else if (item.getOption().equals("small")) {
                jsApi += "'S')";
            }

        }

//        Logger.t(TAG).d("call api: " + jsApi);
        mWebView.loadUrl(jsApi);
    }


    public void updateRawDateItem(RawDataItem item) {
        JSONObject state = new JSONObject();
        String data = null;
        try {
            switch (item.getType()) {
                case RawDataItem.DATA_TYPE_IIO:
                    IioData iioData = (IioData) item.data;
                    state.put("roll", iioData.euler_roll);
                    state.put("pitch", iioData.euler_pitch);
                    state.put("gforceBA", iioData.accX);
                    state.put("gforceLR", iioData.accZ);
                    break;
                case RawDataItem.DATA_TYPE_GPS:
                    GpsData gpsData = (GpsData) item.data;
                    state.put("lng", gpsData.coord.lng);
                    state.put("lat", gpsData.coord.lat);
                    break;
                case RawDataItem.DATA_TYPE_OBD:
                    ObdData obdData = (ObdData) item.data;
                    state.put("rpm", obdData.rpm);
                    state.put("mph", obdData.speed);
                    break;
            }
            SimpleDateFormat format = new SimpleDateFormat("MM dd, yyyy hh:mm:ss");
            long pts = item.getPtsMs() == 0 ? System.currentTimeMillis() : item.getPtsMs();
            String date = format.format(pts);
            data = "numericMonthDate('" + date + "')";
//            Logger.t(TAG).d("pts: " + item.getPtsMs() + " date: " + data);
            //state.put("time", data);
        } catch (JSONException e) {
            Logger.t(TAG).e("", e);
        }

        String callJS1 = "javascript:setState(" + state.toString() + ")";
        String callJS2 = "javascript:setState(" + "{time:" + data + "})";
//        Logger.t(TAG).d("callJS: " + callJS1);
        mWebView.loadUrl(callJS1);
        mWebView.loadUrl(callJS2);
        mWebView.loadUrl("javascript:update()");
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return false;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }



}
