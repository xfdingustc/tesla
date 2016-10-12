package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;

import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/9/28.
 */

public class SettingActivity extends BaseActivity {

    @OnClick(R.id.btn_logout)
    public void onBtnLogoutClicked() {
        SessionManager.getInstance().logout();
    }

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, SettingActivity.class);
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
        setContentView(R.layout.activity_setting);
    }
}
