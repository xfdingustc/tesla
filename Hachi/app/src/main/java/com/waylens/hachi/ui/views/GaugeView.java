package com.waylens.hachi.ui.views;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.eventbus.events.GaugeEvent;
import com.waylens.hachi.ui.fragments.clipplay2.GaugeInfoItem;

import org.greenrobot.eventbus.Subscribe;

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
        mWebView = new WebView(context);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mWebView, params);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.loadUrl("file:///android_asset/api.html");
    }

    @Subscribe()
    public void onGaugeEvent(GaugeEvent event) {
        switch (event.getWhat()) {
            case GaugeEvent.EVENT_WHAT_CHANGE_THEME:
                changeGaugeTheme((String)event.getExtra());
                break;
            case GaugeEvent.EVENT_WHAT_UPDATE_SETTING:
                updateGaugeSetting((GaugeInfoItem)event.getExtra());
                break;
        }

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

        Logger.t(TAG).d("call api: " + jsApi);
        mWebView.loadUrl(jsApi);
    }


}
