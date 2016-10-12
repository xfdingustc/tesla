package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.waylens.hachi.R;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/6/29.
 */
public class WaylensAgreementActivity extends BaseActivity {

    public static final int PAGE_LICENSE = 0;
    public static final int PAGE_PRIVACY = 1;
    public static final int PAGE_TERMS_OF_USE = 2;


    private int requestCode;

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, WaylensAgreementActivity.class);
        activity.startActivity(intent);
    }

    public static void launch(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, WaylensAgreementActivity.class);
        intent.putExtra("code", requestCode);
        activity.startActivity(intent);
    }

    @BindView(R.id.agreement_web)
    WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        requestCode = intent.getIntExtra("code", 0);
        init();
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_waylens_agreement);
        mWebView.setWebViewClient(new WebViewClient());
        switch (requestCode) {
            case PAGE_LICENSE:
                mWebView.loadUrl("file:///android_asset/license/license.htm");
                break;
            case PAGE_PRIVACY:
                mWebView.loadUrl("file:///android_asset/privacy/privacy.html");
                break;
            case PAGE_TERMS_OF_USE:
                mWebView.loadUrl("file:///android_asset/terms/terms.html");
                break;
        }
        setupToolbar();

    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();

        getToolbar().setNavigationIcon(R.drawable.ic_arrow_back);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        switch (requestCode) {
            case PAGE_LICENSE:
                getToolbar().setTitle(R.string.license_agreement);
                break;
            case PAGE_PRIVACY:
                getToolbar().setTitle(R.string.privacy_policy);
                break;
            case PAGE_TERMS_OF_USE:
                getToolbar().setTitle(R.string.terms_of_use);
                break;
        }
    }
}
