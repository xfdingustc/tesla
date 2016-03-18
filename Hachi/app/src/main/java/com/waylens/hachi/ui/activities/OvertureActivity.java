package com.waylens.hachi.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.fragments.BaseFragment;

/**
 * Created by Xiaofei on 2016/3/18.
 */
public class OvertureActivity extends BaseActivity {

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
        setContentView(R.layout.activity_overture);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                redirectTo();
            }
        }, 2000);
    }


    private void redirectTo() {
        Intent intent = new Intent();
        boolean enterSetup = VdtCameraManager.getManager().isConnected();
        if (enterSetup == false) {
            intent.setClass(this, StartupActivity.class);
        } else {
            intent.setClass(this, MainActivity.class);
        }
        startActivity(intent);
        //overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
        finish();
    }
}
