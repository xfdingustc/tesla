package com.waylens.hachi.view.gauge;

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
import com.waylens.hachi.eventbus.events.GaugeEvent;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.utils.rxjava.RxBus;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by Xiaofei on 2016/4/6.
 */
public class GaugeView extends FrameLayout {
    private static final String TAG = GaugeView.class.getSimpleName();

    private static final int PENDING_ACTION_INIT_GAUGE_BY_SETTING = 0x1001;
    private static final int PENDING_ACTION_ROTATE = 0x1002;
    private static final int PENDING_ACTION_MOMENT_SETTING = 0x1003;
    private static final int PENDING_ACTION_TIME_POINT = 0x1004;
    private static final int PENDING_ACTION_SHOW_DEFAULT_GAUGE = 0x1005;
    private static final int PENDInG_ACTION_RESIZE_MAP = 0x1006;

    public static final int MODE_CAMERA = 0;

    public static final int MODE_MOMENT = 1;

    private WebView mWebView;

    private int mGaugeMode = MODE_CAMERA;

    private GaugeViewAdapter mAdapter;

    private GaugeViewAdapterObserver mObserver;


    private Subscription mActionSubscription;

    private List<PendingActionItem> mPendingActions = new ArrayList<>();

    public GaugeView(Context context) {
        super(context);
        init(context);
    }

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void hanldePendingActionItems() {
        for (PendingActionItem item : mPendingActions) {
            ArrayList<Long> timePoints;
            switch (item.type) {
                case PENDING_ACTION_INIT_GAUGE_BY_SETTING:
                    List<GaugeInfoItem> itemList = GaugeSettingManager.getManager().getSetting();
                    for (GaugeInfoItem gaugeInfoItem : itemList) {
                        updateGaugeSetting(gaugeInfoItem);
                    }
                    changeGaugeTheme(GaugeSettingManager.getManager().getTheme());
                    break;
                case PENDING_ACTION_ROTATE:
                    mWebView.loadUrl(GaugeJsHelper.jsSetRotate((Boolean) item.param1));
                    mWebView.loadUrl(GaugeJsHelper.jsUpdate());
                    break;
                case PENDING_ACTION_MOMENT_SETTING:
                    doGaugeSetting((Map<String, String>) item.param1);
                    timePoints = (ArrayList) item.param2;
                    if (timePoints != null && timePoints.size() == 6) {
                        setRaceTimingPoints(timePoints);
                    }

                    break;
                case PENDING_ACTION_TIME_POINT:
                    Logger.t(TAG).d("set time points");
                    timePoints = (ArrayList) item.param1;
                    if (timePoints != null) {
                        String jsApi = "javascript:setGauge('CountDown','S')";
                        Logger.t(TAG).d(jsApi);
                        mWebView.loadUrl(jsApi);
                    }
                    setRaceTimingPoints(timePoints);
                    break;
                case PENDING_ACTION_SHOW_DEFAULT_GAUGE:
                    mWebView.loadUrl(GaugeJsHelper.jsInitDefaultGauge());
                    break;
                case PENDInG_ACTION_RESIZE_MAP:
                    mWebView.loadUrl(GaugeJsHelper.jsResizeMap());
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mActionSubscription != null && !mActionSubscription.isUnsubscribed()) {
            mActionSubscription.unsubscribe();
        }
    }


    private void init(Context context) {
        if (isInEditMode()) {
            return;
        }
        mWebView = new WebView(context);
        final LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mWebView, params);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.loadUrl("file:///android_asset/build/api.html");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mActionSubscription = RxBus.getDefault().toObserverable(EventPendingActionAdded.class)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SimpleSubscribe<EventPendingActionAdded>() {
                        @Override
                        public void onNext(EventPendingActionAdded pendingActionItem) {
                            hanldePendingActionItems();

                        }
                    });
                setUnit();
                mPendingActions.add(new PendingActionItem(PENDInG_ACTION_RESIZE_MAP, null));
                RxBus.getDefault().post(new EventPendingActionAdded());
            }
        });
        mObserver = new GaugeViewAdapterObserver() {
            @Override
            public void notifyRawDataItemUpdated(List<RawDataItem> rawDataItemList) {
                updateRawDateItem(rawDataItemList);
            }

            @Override
            public void notifyRacingTimePoints(List<Long> racingTimePoints) {
                setDefaultViewAndTimePoints(racingTimePoints);
            }
        };
    }



    public void setAdapter(GaugeViewAdapter adapter) {
        this.mAdapter = adapter;
        mAdapter.registerAdapterDataObserver(mObserver);
    }

    public void setGaugeMode(int gaugeMode) {
        switch (gaugeMode) {
            case MODE_CAMERA:
                mGaugeMode = gaugeMode;
                break;
            case MODE_MOMENT:
                mGaugeMode = gaugeMode;
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
        mPendingActions.add(new PendingActionItem(PENDING_ACTION_INIT_GAUGE_BY_SETTING, null));
        RxBus.getDefault().post(new EventPendingActionAdded());
    }

    public void setDefaultViewAndTimePoints(final List<Long> timepoints) {
        Logger.t(TAG).d("set time point delay");
        mPendingActions.add(new PendingActionItem(PENDING_ACTION_INIT_GAUGE_BY_SETTING, null));
        mPendingActions.add(new PendingActionItem(PENDING_ACTION_TIME_POINT, timepoints));
        RxBus.getDefault().post(new EventPendingActionAdded());
    }

    public void showDefaultGauge() {
        mPendingActions.add(new PendingActionItem(PENDING_ACTION_SHOW_DEFAULT_GAUGE, null));
        RxBus.getDefault().post(new EventPendingActionAdded());

    }


    public void setRotate(boolean ifRoate) {
        mPendingActions.add(new PendingActionItem(PENDING_ACTION_ROTATE, ifRoate));
        RxBus.getDefault().post(new EventPendingActionAdded());
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
        mPendingActions.add(new PendingActionItem(PENDING_ACTION_MOMENT_SETTING, overlaySetting, timePoints));
        RxBus.getDefault().post(new EventPendingActionAdded());
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

    private class EventPendingActionAdded {

    }

    public abstract static class GaugeViewAdapter {
        private GaugeViewAdapterObserver mObserver;



        public void registerAdapterDataObserver(GaugeViewAdapterObserver observer) {
            mObserver = observer;
            if (getRacingTimeList() != null && mObserver != null) {
                mObserver.notifyRacingTimePoints(getRacingTimeList());
            }
        }

        public abstract List<RawDataItem> getRawDataItemList(long pts);

        public abstract List<Long> getRacingTimeList();

        public void notifyRawDataItemUpdated(List<RawDataItem> rawDataItemList) {
            if (mObserver != null) {
                mObserver.notifyRawDataItemUpdated(rawDataItemList);
            }
        }

        public void notifyRacingTimePoints(List<Long> racingTimePoints) {
            if (mObserver != null) {
                mObserver.notifyRacingTimePoints(racingTimePoints);
            }
        }
    }

    private abstract class GaugeViewAdapterObserver {
        public void notifyRawDataChanged() {

        }

        public void notifyRawDataItemUpdated(List<RawDataItem> rawDataItemList) {

        }

        public void notifyRacingTimePoints(List<Long> racingTimePoints) {

        }
    }

}
