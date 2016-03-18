package com.waylens.hachi.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;

import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/3/18.
 */
public class StartupActivity extends BaseActivity {

    @OnClick(R.id.skip)
    public void onSkipClicked() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
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
        setContentView(R.layout.activity_startup);
    }
}
