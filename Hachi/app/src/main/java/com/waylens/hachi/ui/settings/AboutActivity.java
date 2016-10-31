package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.WebViewActivity;

import butterknife.OnClick;

/**
 * Created by lshw on 16/7/28.
 */
public class AboutActivity extends BaseActivity{
    public static final String TAG =  AboutActivity.class.getSimpleName();


    @OnClick(R.id.terms_of_use)
    public void onTermsOfUseClicked() {
        WebViewActivity.launch(this, WebViewActivity.PAGE_TERMS_OF_USE);
    }

    @OnClick(R.id.license_agreement)
    public void onLicenseAgreementClicked() {
        WebViewActivity.launch(this, WebViewActivity.PAGE_LICENSE);

    }

    @OnClick(R.id.privacy_policy)
    public void onPrivacyPolicyClicked() {
        WebViewActivity.launch(this, WebViewActivity.PAGE_PRIVACY);

    }

    @OnClick(R.id.version)
    public void onVersionClicked() {
        VersionCheckActivity.launch(this);
    }

    @OnClick(R.id.quick_start)
    public void onSupportClicked() {
        WebViewActivity.launch(this, WebViewActivity.PAGE_SUPPORT);
    }

    @OnClick(R.id.feed_back)
    public void onFeedbackClicked() {
        FeedbackActivity.launch(this);
    }


    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, AboutActivity.class);
        activity.startActivity(intent);
    }

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
        setContentView(R.layout.activity_about);
        setupToolbar();
    }


    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
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
        getToolbar().setTitle("About");

    }

}
