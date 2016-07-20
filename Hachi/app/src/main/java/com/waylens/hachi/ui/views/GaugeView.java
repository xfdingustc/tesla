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
import com.waylens.hachi.library.vdb.rawdata.GpsData;
import com.waylens.hachi.library.vdb.rawdata.IioData;
import com.waylens.hachi.library.vdb.rawdata.ObdData;
import com.waylens.hachi.library.vdb.rawdata.RawDataItem;
import com.waylens.hachi.library.vdb.rawdata.WeatherData;
import com.waylens.hachi.ui.clips.player.GaugeInfoItem;


import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class GaugeView extends FrameLayout {
    private static final String TAG = GaugeView.class.getSimpleName();

    private WebView mWebView;

    private boolean mIsOdbGaugeShow;

    private boolean mIsIioGaugeShow;

    private boolean mIsGpsGaugeShow;

    private int iioPressure;

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
        mWebView.loadUrl("file:///android_asset/build/api.html");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                initGaugeView();
                super.onPageFinished(view, url);
            }
        });
        mIsGpsGaugeShow = false;
        mIsIioGaugeShow = false;
        mIsOdbGaugeShow = false;
    }

    public void setEnhanceMode() {
        mIsGpsGaugeShow = true;
        mIsIioGaugeShow = true;
        mIsOdbGaugeShow = true;
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

    public void setVisibility(boolean show) {
        if (show) {
            mWebView.setVisibility(View.VISIBLE);
        }
        else {
            mWebView.setVisibility(View.INVISIBLE);
        }
    }


    public void showGauge(boolean show) {
        //CameraPreview and ClipPlay need to initialize gauge style, using initGaugeView
        if (show) {
            mWebView.setVisibility(View.VISIBLE);
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    initGaugeView();
                }
            });
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

    public void updateGaugeSetting(GaugeInfoItem item) {
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

    public void changeGaugeSetting(final Map<String, String> overlaySetting) {


        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                String theme = overlaySetting.get("theme");
                if (theme == null) {
                    theme = GaugeSettingManager.getManager().getTheme();
                }
                changeGaugeTheme(theme);
                Logger.t(TAG).d("theme:" + theme);

                String[] gaugeParams = GaugeInfoItem.OPTION_JS_PARAMS;
                String param = null;
                for (int i = 0; i < gaugeParams.length; i++) {
                    if ((param = overlaySetting.get(gaugeParams[i])) != null) {
                        if (param.equals("L") || param.equals("M") || param.equals("S")) {
                            String jsApi = "javascript:setState({'" + gaugeParams[i] + "':'" + param + "'})";
                            Logger.t(TAG).d(jsApi);
                            mWebView.loadUrl(jsApi);
                        }
                    }
                }
                mWebView.loadUrl("javascript:update()");

            }
        });
    }



    public void updateRawDateItem(List<RawDataItem> itemList) {
        JSONObject state = new JSONObject();
        String data = null;
        RawDataItem item = null;
        try {
            for (int i = 0; i < itemList.size(); i++) {
                item = itemList.get(i);
                switch (item.getType()) {
                    case RawDataItem.DATA_TYPE_IIO:
/*                    if (!mIsIioGaugeShow) {
                        mIsIioGaugeShow = true;
                        showIioGauge(mIsIioGaugeShow);
                    }*/
                        //Logger.t(TAG).d("IIO data");
                        IioData iioData = (IioData) item.data;
                        state.put("roll", iioData.euler_roll);
                        state.put("pitch", iioData.euler_pitch);
                        state.put("gforceBA", iioData.accX);
                        state.put("gforceLR", iioData.accZ);
                        iioPressure = iioData.pressure;
                        break;
                    case RawDataItem.DATA_TYPE_GPS:
/*                    if (!mIsGpsGaugeShow) {
                        mIsGpsGaugeShow = true;
                        showIioGauge(mIsGpsGaugeShow);
                    }*/
                        //Logger.t(TAG).d("GPS data");
                        GpsData gpsData = (GpsData) item.data;
                        state.put("lng", gpsData.coord.lng);
                        state.put("lat", gpsData.coord.lat);
                        state.put("gpsSpeed", gpsData.speed);
                        break;
                    case RawDataItem.DATA_TYPE_OBD:
/*                  if (!mIsOdbGaugeShow) {
                        mIsOdbGaugeShow = true;
                        showOdbGauge(mIsOdbGaugeShow);
                    }*/
                        //Logger.t(TAG).d("OBD data");
                        ObdData obdData = (ObdData) item.data;
                        state.put("rpm", obdData.rpm);
                        state.put("obdSpeed", obdData.speed);
                        state.put("throttle", obdData.throttle);
                        //state.put("mph", obdData.speed); deprecated in new version of svg
                        if (obdData.isIMP) {
                            state.put("psi", obdData.psi);
                        } else {
                            state.put("psi", obdData.psi - iioPressure / 3386000);
                        }
//                    Logger.t(TAG).d(Double.toString(obdData.psi));
                        break;
                    case RawDataItem.DATA_TYPE_WEATHER:
                        //Logger.t(TAG).d("Weather data");
                        WeatherData weatherData = (WeatherData) item.data;
                        JSONObject ambient = new JSONObject();
                        ambient.put("tmpF", weatherData.tempF);
                        ambient.put("windSpeedMiles", weatherData.windSpeedMiles);
                        ambient.put("pressure", weatherData.pressure);
                        ambient.put("humidity", weatherData.humidity);
                        ambient.put("weatherCode", weatherData.weatherCode);
                        state.put("ambient", ambient);
                        break;
                    default:
                        break;
                }
            }
            SimpleDateFormat format = new SimpleDateFormat("MM dd, yyyy hh:mm:ss");
            long pts = item.getPtsMs() == 0 ? System.currentTimeMillis() : item.getPtsMs();
            String date = format.format(pts);
            data = "numericMonthDate('" + date + "')";
//            Logger.t(TAG).d("pts: " + item.getPtsMs() + " date: " + data);
        } catch (JSONException e) {
            Logger.t(TAG).e("", e);
        }

        StringBuffer sb = new StringBuffer(state.toString());
        sb.insert(state.toString().length() - 1, ",time:" + data);


        String callJS1 = "javascript:setRawData(" + sb.toString() + ")";
//        Logger.t(TAG).d(callJS1);
//        String callJS2 = "javascript:setRawData(" + "{time:" + data + "})";
//        Logger.t(TAG).d("callJS: " + callJS1);
        mWebView.loadUrl(callJS1);
//        mWebView.loadUrl(callJS2);
        //mWebView.loadUrl("javascript:update()");
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return false;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }


    public void showTimeDateGauge(boolean show) {
        JSONObject state = new JSONObject();
        try {
            if (show) {
                state.put("showTimeDate", "M");
            } else {
                state.put("showTimeDate", "");
            }
        } catch (JSONException e) {
            Logger.t(TAG).e("showTimeDate", e);
        }
        String callJS = "javascript:setState(" + state.toString() + ")";
        mWebView.loadUrl(callJS);
        mWebView.loadUrl("javascript:update");
    }


    public void showGpsGauge(boolean show) {
        JSONObject state = new JSONObject();
        try {
            if (show) {
                state.put("showGps", "M");
                state.put("showAmbient", "M");
            } else {
                state.put("showGps", "");
                state.put("showAmbient", "");
            }
        } catch (JSONException e) {
            Logger.t(TAG).e("showGpsGauge", e);
        }
        String callJS = "javascript:setState(" + state.toString() + ")";
        mWebView.loadUrl(callJS);
        mWebView.loadUrl("javascript:update");
    }

    public void showOdbGauge(boolean show) {
        JSONObject state = new JSONObject();
        try {
            if (show) {
                state.put("showSpeedThrottle", "M");
                state.put("showRpm", "M");
                state.put("showPsi", "M");
            } else {
                state.put("showSpeedThrottle", "");
                state.put("showRpm", "");
                state.put("showPsi", "");
            }
        } catch (JSONException e) {
            Logger.t(TAG).e("showOdbGauge", e);
        }
        String callJS = "javascript:setState(" + state.toString() + ")";
        mWebView.loadUrl(callJS);
        mWebView.loadUrl("javascript:update");
    }

    public void showIioGauge(boolean show) {
        JSONObject state = new JSONObject();
        try {
            if (show) {
                state.put("showGforce", "M");
                state.put("showRollPitch", "M");
            } else {
                state.put("showGforce", "");
                state.put("showRollPitch", "");
            }
        }catch (JSONException e) {
            Logger.t(TAG).e("showIioGauge", e);
        }
        String callJS = "javascript:setState(" + state.toString() + ")";
        mWebView.loadUrl(callJS);
        mWebView.loadUrl("javascript:update");
    }
}
