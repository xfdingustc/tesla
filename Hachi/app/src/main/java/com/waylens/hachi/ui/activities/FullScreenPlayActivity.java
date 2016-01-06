package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/1/6.
 */
public class FullScreenPlayActivity extends BaseActivity {

    public static void launch(Activity startActivity) {
        Intent intent = new Intent(startActivity, FullScreenPlayActivity.class);
        startActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void init() {
        super.init();
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_fullscreen_play);
    }
}
