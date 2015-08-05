package com.waylens.hachi.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import com.waylens.hachi.R;

import butterknife.OnClick;

/**
 * Created by Xiaofei on 2015/8/5.
 */
public class LoginActivity extends BaseActivity {

    @OnClick(R.id.btnHaveNoAccount)
    public void onBtnHaveNoAccountClicked() {
        SignupActivity.launch(this);
        finish();
    }

    public static void launch(Activity startActivity) {
        Intent intent = new Intent(startActivity, LoginActivity.class);
        startActivity.startActivity(intent);
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
        setContentView(R.layout.activity_login);

    }
}
