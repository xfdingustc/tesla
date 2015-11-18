package com.waylens.hachi.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.WindowManager;

import com.waylens.hachi.R;
import com.waylens.hachi.views.dashboard.DashboardView;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2015/9/6.
 */
public class DashboardActivity extends BaseActivity {

    @Bind(R.id.dashboard)
    DashboardView mDashboardView;


    public static void launch(Context context) {
        Intent intent = new Intent(context, DashboardActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_dashboard);
    }

}
