package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.waylens.hachi.R;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/6/29.
 */
public class WaylensAgreementActivity extends BaseActivity {

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, WaylensAgreementActivity.class);
        activity.startActivity(intent);
    }

    @BindView(R.id.agreement_web)
    WebView mAgreeWeb;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_waylens_agreement);
        mAgreeWeb.setWebViewClient(new WebViewClient());
        mAgreeWeb.loadUrl("file:///android_asset/license/license.htm");

    }
}
