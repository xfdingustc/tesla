package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.liveview.CameraPreviewFragment;

/**
 * Created by Xiaofei on 2016/5/23.
 */
public class WifiSettingActivity extends BaseActivity {

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, WifiSettingActivity.class);
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
        setContentView(R.layout.activity_wifi_setting);
        setupToolbar();
        WifiSettingFragment fragment = new WifiSettingFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();

    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.wifi);
    }
}
