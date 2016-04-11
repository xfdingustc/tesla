package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/4/11.
 */
public class SmartRemixActivity extends BaseActivity {

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, SmartRemixActivity.class);
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
        setContentView(R.layout.activity_smart_remix);
    }

    @Override
    public void setupToolbar() {
        mToolbar.setTitle(R.string.smart_remix);
        mToolbar.setNavigationIcon(R.drawable.navbar_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        super.setupToolbar();
    }
}
