package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2015/12/10.
 */


public class CameraControlActivity2 extends BaseActivity {

    public static void launch(Activity startingActivity) {
        Intent intent = new Intent(startingActivity, CameraControlActivity2.class);
        startingActivity.startActivity(intent);
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
        setContentView(R.layout.activity_camera_control2);
    }
}
