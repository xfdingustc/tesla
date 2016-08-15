package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

/**
 * Created by Xiaofei on 2016/8/15.
 */
public class ProfileSettingActivity extends BaseActivity {

    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, ProfileSettingActivity.class);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setting);
        setupToolbar();

        Fragment fragment = new ProfileSettingPreferenceFragment();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.profile_setting);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
