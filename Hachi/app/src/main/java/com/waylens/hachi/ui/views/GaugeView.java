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
import com.waylens.hachi.rest.response.MomentInfo;
import com.waylens.hachi.ui.clips.player.GaugeInfoItem;
import com.xfdingustc.snipe.vdb.rawdata.GpsData;
import com.xfdingustc.snipe.vdb.rawdata.IioData;
import com.xfdingustc.snipe.vdb.rawdata.ObdData;
import com.xfdingustc.snipe.vdb.rawdata.RawDataItem;
import com.xfdingustc.snipe.vdb.rawdata.WeatherData;


import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class GaugeView extends FrameLayout {
    private static final String TAG = GaugeView.class.getSimpleName();

    public static final int MODE_CAMERA = 0;

    public static final int MODE_MOMENT = 1;

    private WebView mWebView;

    private int mGaugeMode = MODE_CAMERA;

    private DateFormat mDateFormat;

    private boolean mIsLoadingFinish = false;




    private int iioPressure;

    public GaugeView(Context context) {
        super(context);
        init(context);
    }


    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void setGaugeMode(int gaugeMode) {
        switch (gaugeMode) {
            case MODE_CAMERA:
                mGaugeMode = gaugeMode;
                mDateFormat = new SimpleDateFormat("MM dd, yyyy HH:mm:ss"/*, Locale.getDefault()*/);
                break;
            case MODE_MOMENT:
                mGaugeMode = gaugeMode;
                mDateFormat = new SimpleDateFormat("MM dd, yyyy HH:mm:ss");
                break;
            default:
                break;
        }
        if (mGaugeMode != MODE_MOMENT) {
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    initGaugeView();
                    super.onPageFinished(view, url);
                }
            });
        }
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
                mIsLoadingFinish = true;
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
        if (theme.equals("")) {
            theme = "default";
        }
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

    public void changeGaugeSetting(final Map<String, String> overlaySetting, final ArrayList<Long> timePoints) {
        if (mIsLoadingFinish) {
            Logger.t(TAG).d("loading finish");
            doGaugeSetting(overlaySetting);
        } else {
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    doGaugeSetting(overlaySetting);
                    if (timePoints != null) {
                        setRaceTimingPoints(timePoints);
                    }
                }
            });
        }
    }

    public void doGaugeSetting(Map<String, String> overlaySetting) {
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
        if (overlaySetting.get("CountDown") != null) {
            String jsApi = "javascript:setGauge('CountDown','S')";
            Logger.t(TAG).d(jsApi);
            mWebView.loadUrl(jsApi);
        }

    }

    public void setRaceTimingPoints(ArrayList<Long> raceTimingPoints) {
        JSONObject timePoints = new JSONObject();
        try {
            if (raceTimingPoints.get(0) > 0) {
                timePoints.put("t1", raceTimingPoints.get(0));
            }
            timePoints.put("t2", raceTimingPoints.get(1));
            timePoints.put("t3", raceTimingPoints.get(2));
            timePoints.put("t4", raceTimingPoints.get(3));
            if (raceTimingPoints.get(4) > 0) {
                timePoints.put("t5", raceTimingPoints.get(4));
            }
            if (raceTimingPoints.get(5) > 0) {
                timePoints.put("t6", raceTimingPoints.get(5));
            }
        } catch (JSONException e) {
            Logger.t(TAG).d(e.getMessage());
        }
        final String jsApi = "javascript:setTimePoints(" + timePoints.toString() + ")";
        Logger.t(TAG).d(jsApi);
        mWebView.loadUrl(jsApi);
    }

    public void setPlayTime(int currentTime) {
        String playTime = "javascript:setPlayTime(" + currentTime + ")";
        mWebView.loadUrl(playTime);
    }

    public void updateRawDateItem(List<RawDataItem> itemList) {
        JSONObject state = new JSONObject();
        String data = null;
        RawDataItem item = null;
        long pts = 0;
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
                        pts = item.getPtsMs();
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
                        if (!obdData.isIMP) {
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
            if (pts == 0) {
                pts = System.currentTimeMillis();
            }
            String date = mDateFormat.format(pts);
            data = "numericMonthDate('" + date + "')";
            //Logger.t(TAG).d("pts: " + item.getPtsMs() + " date: " + data);
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
