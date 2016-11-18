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
    private boolean mNeedDelay = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

    }


    @Override
    protected void init() {
        super.init();
        mNeedDelay = getIntent().getBooleanExtra("need_delay", true);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_overture);
        if (mNeedDelay) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    redirectTo();
                }
            }, 2000);
        } else {
            redirectTo();
        }
    }

    private boolean isFirstInstall() {
        int oldVersionCode = PreferenceUtils.getInt(PreferenceUtils.VERSION_CODE, 0);
        return oldVersionCode == 0;
    }


    private boolean isUpdated() {
        int oldVersionCode = PreferenceUtils.getInt(PreferenceUtils.VERSION_CODE, 0);

        int newVersionCode;

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            newVersionCode = pi.versionCode;

            return newVersionCode > oldVersionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean hasBigChange() {
        int newVersionCode;

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            newVersionCode = pi.versionCode;

            return newVersionCode % 10 == 0;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }


    private void redirectTo() {
        if (isFirstInstall() || (isUpdated() && hasBigChange())) {
            FirstInstallActivity.launch(this);
        } else {
            MainActivity.launch(this);
        }

    }


}
