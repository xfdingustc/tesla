package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.utils.PreferenceUtils;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/5/3.
 */
public class GeneralSettingActivity extends BaseActivity {
    private static final String TAG = GeneralSettingActivity.class.getSimpleName();
    @BindView(R.id.light_theme)
    Switch btnLightTheme;

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
        btnLightTheme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    Logger.t(TAG).d("set to light");
                    getApplication().setTheme(R.style.LightTheme);
                    PreferenceUtils.putString(PreferenceUtils.APP_THEME, "light");

                } else {
                    Logger.t(TAG).d("set to dark");
                    getApplication().setTheme(R.style.DarkTheme);
                    PreferenceUtils.putString(PreferenceUtils.APP_THEME, "dark");
                }
            }
        });


        GeneralSettingFragment fragment = new GeneralSettingFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
    }
}
