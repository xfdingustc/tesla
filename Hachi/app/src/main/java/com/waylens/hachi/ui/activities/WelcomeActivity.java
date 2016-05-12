package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;

import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/3/25.
 */
public class WelcomeActivity extends BaseActivity {

    @OnClick(R.id.skip)
    public void onSkipClicked() {
        MainActivity.launch(this);
    }

    @OnClick(R.id.btnCreateNewAccount)
    public void onBtnCreateNewAccountClicked() {

    }

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, WelcomeActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
    }
}
