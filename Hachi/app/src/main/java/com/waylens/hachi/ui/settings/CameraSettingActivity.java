package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

/**
 * Created by Xiaofei on 2016/5/3.
 */
public class CameraSettingActivity extends BaseActivity {
    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, CameraSettingActivity.class);
        activity.startActivity(intent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_camera_setting);
        setupToolbar();
        CameraSettingFragment fragment = new CameraSettingFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();

    }



    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.camera);
        getToolbar().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
