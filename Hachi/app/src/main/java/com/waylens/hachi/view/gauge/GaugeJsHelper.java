package com.waylens.hachi.view.gauge;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.vdb.rawdata.GpsData;
import com.waylens.hachi.snipe.vdb.rawdata.IioData;
import com.waylens.hachi.snipe.vdb.rawdata.ObdData;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.snipe.vdb.rawdata.WeatherData;
import com.waylens.hachi.utils.SettingHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Xiaofei on 2016/11/2.
 */

public class GaugeJsHelper {
    private static final String TAG = GaugeJsHelper.class.getSimpleName();
    private static int mIioPressure;

    public static String jsInitDefaultGauge() {
        return "javascript:initDefaultGauge()";
    }

    public static String jsSetRotate(boolean rotate) {
        return "javascript:setState({" + "ifRotate:" + rotate + "})";
    }

    public static String jsSetTail(int startTailMs) {
        return "javascript:setState({" + "startTail:" + startTailMs + "})";
    }

    public static String jsUpdate() {
        return "javascript:update()";
    }

    public static String jsSetTheme(String theme) {
        if (theme.equals("") || theme.equals("elegant")) {
            theme = "default";
        }
        return "javascript:setTheme('" + theme + "')";
    }

    public static String jsUpdateGaugeSetting(GaugeInfoItem item) {
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
        return jsApi;
    }

    public static String jsUpdateRawData(List<RawDataItem> itemList, int gaugeMode) {
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
                        state.put("gforceBA", iioData.accZ);
                        state.put("gforceLR", iioData.accX);
                        mIioPressure = iioData.pressure;
                        pts = item.getPtsMs();
                        break;
                    case RawDataItem.DATA_TYPE_GPS:
/*                    if (!mIsGpsGaugeShow) {
                        mIsGpsGaugeShow = true;
                        showIioGauge(mIsGpsGaugeShow);
                    }*/
                        //Logger.t(TAG).d("GPS data");
                        GpsData gpsData = (GpsData) item.data;
                        state.put("lng", gpsData.coord.lng_orig);
                        state.put("lat", gpsData.coord.lat_orig);
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
                        if (!obdData.isIMP || (obdData.psi == 0)) {
                            state.put("psi", obdData.psi);
                        } else {
                            state.put("psi", obdData.psi - mIioPressure / 3386000);
                        }
                        Logger.t(TAG).d(Double.toString(obdData.psi));
                        Logger.t(TAG).d(mIioPressure);
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
            if (pts == 0 && gaugeMode == GaugeView.MODE_CAMERA) {
                pts = System.currentTimeMillis();
            }
            if (pts != 0) {
                DateFormat mDateFormat = new SimpleDateFormat("MM dd, yyyy HH:mm:ss");
                String date = mDateFormat.format(pts);
                data = "numericMonthDate('" + date + "')";
            }
            //Logger.t(TAG).d("pts: " + item.getPtsMs() + " date: " + data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        StringBuffer sb = new StringBuffer(state.toString());
        if (data != null) {
            sb.insert(state.toString().length() - 1, ",time:" + data);
        }
        return "javascript:setRawData(" + sb.toString() + ")";
    }

    public static String jsSetMetric() {
        if (SettingHelper.isMetricUnit()) {
            return "javascript:setState({isMetric: true})";
        } else {
            return "javascript:setState({isMetric: false})";
        }

    }

    public static String jsResizeMap() {
        return "javascript:fixRenderingsByResizeMap()";
    }

    public static String jsSetPlayTime(int msecs) {
        return "javascript:setState({playTime:" + msecs + "})";
    }
}
