package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

/**
 * Created by Xiaofei on 2016/5/3.
 */
public class GeneralSettingActivity extends BaseActivity{

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, GeneralSettingActivity.class);
        activity.startActivity(intent);
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
        setContentView(R.layout.activity_general_setting);

        GeneralSettingFragment fragment = new GeneralSettingFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
    }
}
