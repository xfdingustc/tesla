package com.waylens.hachi.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.waylens.hachi.R;

import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/3/18.
 */
public class StartupActivity extends BaseActivity {

    @OnClick(R.id.btnGetStarted)
    public void onBtnGetStartedClicked() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .customView(R.layout.dialog_get_camera_pemission, true)
            .positiveText(R.string.understand)
            .backgroundColor(Color.WHITE)
            .callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    MenualSetupActivity.launch(StartupActivity.this);
                    finish();
                    super.onPositive(dialog);
                }
            })
            .build();
        dialog.show();
    }

    @OnClick(R.id.skip)
    public void onSkipClicked() {
        MaterialDialog dialog =  new MaterialDialog.Builder(this)
            .content(R.string.skip_confirm)
            .positiveText(R.string.leave)
            .negativeText(android.R.string.cancel)
            .callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    Intent intent = new Intent(StartupActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    super.onPositive(dialog);
                }

                @Override
                public void onNegative(MaterialDialog dialog) {
                    super.onNegative(dialog);
                }
            })
            .build();
        dialog.show();

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
