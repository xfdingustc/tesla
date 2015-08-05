package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.waylens.hachi.R;

import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class SignupActivity extends BaseActivity {

    @OnClick(R.id.btnHaveAccount)
    public void onBtnHaveAccountClicked() {
        LoginActivity.launch(this);
        finish();
    }

    public static void launch(Activity startActivity) {
        Intent intent = new Intent(startActivity, SignupActivity.class);
        startActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_signup);
    }
}
