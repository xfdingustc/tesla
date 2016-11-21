package com.waylens.hachi.ui.views.gauge;

import android.content.Context;
import android.graphics.Bitmap;
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
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.ui.clips.player.GaugeInfoItem;
import com.waylens.mediatranscoder.engine.OverlayProvider;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class GaugeView extends FrameLayout implements OverlayProvider {
    private static final String TAG = GaugeView.class.getSimpleName();

    private static final int PENDING_ACTION_INIT_GAUGE_BY_SETTING = 0x1001;
    private static final int PENDING_ACTION_ROTATE = 0x1002;
    private static final int PENDING_ACTION_MOMENT_SETTING = 0x1003;
    private static final int PENDING_ACTION_TIME_POINT = 0x1004;
    private static final int PENDING_ACTION_SHOW_DEFAULT_GAUGE = 0x1005;

    public static final int MODE_CAMERA = 0;

    public static final int MODE_MOMENT = 1;

    private WebView mWebView;

    private int mGaugeMode = MODE_CAMERA;

    private DateFormat mDateFormat;

    private final Object mLock = new Object();

    boolean mIsLoadingFinish = false;

    private List<PendingActionItem> mPendingActions = new ArrayList<>();

    public GaugeView(Context context) {
        super(context);
        init(context);
    }

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    public Bitmap updateTexImage(long pts) {
        Logger.t(TAG).d("update text image");
        return getDrawingCache();
    }


    private void init(Context context) {
        if (isInEditMode()) {
            return;
        }
        setDrawingCacheEnabled(true);
        mWebView = new WebView(context);
        final LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mWebView, params);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.loadUrl("file:///android_asset/build/api.html");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                synchronized (mLock) {
                    mIsLoadingFinish = true;
                    for (PendingActionItem item : mPendingActions) {
                        Logger.t(TAG).d("item type: " + item.type);
                        switch (item.type) {
                            case PENDING_ACTION_INIT_GAUGE_BY_SETTING:
                                initGaugeViewBySetting();
                                break;
                            case PENDING_ACTION_ROTATE:
                                setRotate((Boolean) item.param1);
                                break;
                            case PENDING_ACTION_MOMENT_SETTING:
                                doGaugeSetting((Map<String, String>) item.param1);
                                ArrayList<Long> timePoints = (ArrayList) item.param2;
                                if (timePoints != null && timePoints.size() == 6) {
                                    setRaceTimingPoints(timePoints);
                                }
                                break;
                            case PENDING_ACTION_TIME_POINT:
                                Logger.t(TAG).d("set time points");
                                ArrayList<Long> timePoint = (ArrayList) item.param1;
                                setDefaultViewAndTimePoints(timePoint);
                                break;
                            case PENDING_ACTION_SHOW_DEFAULT_GAUGE:
                                showDefaultGauge();
                                break;
                            default:
                                break;
                        }
                    }
                    setUnit();
                }

            }
        });
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
            initGaugeViewBySetting();
        }
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


    public void initGaugeViewBySetting() {
        synchronized (mLock) {
            if (mIsLoadingFinish) {
                List<GaugeInfoItem> itemList = GaugeSettingManager.getManager().getSetting();
                for (GaugeInfoItem item : itemList) {
                    updateGaugeSetting(item);
                }
                changeGaugeTheme(GaugeSettingManager.getManager().getTheme());
            } else {
                mPendingActions.add(new PendingActionItem(PENDING_ACTION_INIT_GAUGE_BY_SETTING, null));
            }
        }

    }

    public void setDefaultViewAndTimePoints(final List<Long> timepoints) {
        synchronized (mLock) {
            if (mIsLoadingFinish) {
                initGaugeViewBySetting();
                if (timepoints != null) {
                    String jsApi = "javascript:setGauge('CountDown','S')";
                    Logger.t(TAG).d(jsApi);
                    mWebView.loadUrl(jsApi);
                }
                setRaceTimingPoints(timepoints);
            } else {
                Logger.t(TAG).d("set time point delay");
//                mPendingActions.add(new PendingActionItem(PENDING_ACTION_INIT_GAUGE_BY_SETTING, null));
                mPendingActions.add(new PendingActionItem(PENDING_ACTION_TIME_POINT, timepoints));
            }
        }
    }

    public void showDefaultGauge() {
        synchronized (mLock) {
            if (mIsLoadingFinish) {
                mWebView.loadUrl(GaugeJsHelper.jsInitDefaultGauge());
            } else {
                mPendingActions.add(new PendingActionItem(PENDING_ACTION_SHOW_DEFAULT_GAUGE, null));
            }
        }

    }


    public void setRotate(boolean ifRoate) {
        synchronized (mLock) {
            if (mIsLoadingFinish) {
                mWebView.loadUrl(GaugeJsHelper.jsSetRotate(ifRoate));
                mWebView.loadUrl(GaugeJsHelper.jsUpdate());
            } else {
                mPendingActions.add(new PendingActionItem(PENDING_ACTION_ROTATE, ifRoate));
            }
        }
    }


    public void showGauge(boolean show) {
        showGauge(show, false);
    }

    public void showGauge(boolean show, final boolean showAll) {
        //CameraPreview and ClipPlay need to initialize gauge style, using initGaugeViewBySetting
        if (show) {
            mWebView.setVisibility(View.VISIBLE);
        } else {
            mWebView.setVisibility(View.INVISIBLE);
        }

    }

    public boolean isGaugeShown() {
        return mWebView.getVisibility() == View.VISIBLE;
    }


    private void changeGaugeTheme(String theme) {
        mWebView.loadUrl(GaugeJsHelper.jsSetTheme(theme));
    }

    public void updateGaugeSetting(GaugeInfoItem item) {
        mWebView.loadUrl(GaugeJsHelper.jsUpdateGaugeSetting(item));
    }

    public void setUnit() {
        String jsSetUnit = GaugeJsHelper.jsSetMetric();
        Logger.t(TAG).d("set unit: " + jsSetUnit);
        mWebView.loadUrl(jsSetUnit);
        mWebView.loadUrl(GaugeJsHelper.jsUpdate());
    }

    public void changeGaugeSetting(final Map<String, String> overlaySetting, final ArrayList<Long> timePoints) {
        synchronized (mLock) {
            if (mIsLoadingFinish) {
                Logger.t(TAG).d("loading finish");
                doGaugeSetting(overlaySetting);
                if (timePoints != null && timePoints.size() == 6) {
                    setRaceTimingPoints(timePoints);
                }
            } else {
                mPendingActions.add(new PendingActionItem(PENDING_ACTION_MOMENT_SETTING, overlaySetting, timePoints));
            }
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
        mWebView.loadUrl(GaugeJsHelper.jsUpdate());
        if (overlaySetting.get("CountDown") != null) {
            String jsApi = "javascript:setGauge('CountDown','S')";
            Logger.t(TAG).d(jsApi);
            mWebView.loadUrl(jsApi);
        }

    }

    public void setRaceTimingPoints(List<Long> raceTimingPoints) {
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
        mWebView.loadUrl(GaugeJsHelper.jsUpdateRawData(itemList, mGaugeMode));
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }

    private class PendingActionItem {
        int type;
        Object param1;
        Object param2;

        PendingActionItem(int type, Object param1) {
            this.type = type;
            this.param1 = param1;
        }

        PendingActionItem(int type, Object param1, Object param2) {
            this.type = type;
            this.param1 = param1;
            this.param2 = param2;
        }
    }
}
