package com.waylens.hachi.ui.welcome;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.activities.MainActivity;
import com.waylens.hachi.ui.welcome.FirstInstallActivity;
import com.waylens.hachi.utils.PreferenceUtils;

/**
 * Created by Xiaofei on 2016/3/18.
 */
public class OvertureActivity extends BaseActivity {

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
        setContentView(R.layout.activity_overture);
        hideStatusBar();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                redirectTo();
            }
        }, 2000);
    }



    private boolean isUpdated() {
        int oldVersionCode = PreferenceUtils.getInt(PreferenceUtils.VERSION_CODE, 0);

        int newVersionCode;

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            newVersionCode = pi.versionCode;

            if (newVersionCode > oldVersionCode) {
                return true;
            } else {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;


    }


    private void redirectTo() {
        Intent intent = new Intent();
//        boolean enterSetup = VdtCameraManager.getManager().isConnected();
        if (isUpdated()) {
            intent.setClass(this, FirstInstallActivity.class);
        } else {
            intent.setClass(this, MainActivity.class);
        }
        startActivity(intent);
        //overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
        finish();
    }


}
