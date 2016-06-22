package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

/**
 * Created by Xiaofei on 2016/5/11.
 */
public class ConnectivitySettingActivity extends BaseActivity {

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, ConnectivitySettingActivity.class);
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
        setContentView(R.layout.activity_connectivity_setting);
        setupToolbar();
        ConnectivitySettingFragment fragment = new ConnectivitySettingFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.connectivity);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
