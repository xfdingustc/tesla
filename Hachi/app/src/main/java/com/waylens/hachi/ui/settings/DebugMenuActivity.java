package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.utils.DebugHelper;
import com.waylens.hachi.utils.PreferenceUtils;

import butterknife.BindArray;
import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/12/8.
 */

public class DebugMenuActivity extends BaseActivity {
    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, DebugMenuActivity.class);
        activity.startActivity(intent);
    }

    @BindView(R.id.switch_show_moment_source)
    Switch switchShowMomentSource;

    @BindView(R.id.server_97)
    RadioButton server97;

    @BindView(R.id.server_singapore)
    RadioButton serverSingapore;

    @BindView(R.id.server_aws)
    RadioButton serverAws;

    @BindView(R.id.host_server_group)
    RadioGroup hostServerGroup;

    @BindArray(R.array.server_list)
    String[] serverList;

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
        setContentView(R.layout.activity_debug_menu);
        switchShowMomentSource.setChecked(DebugHelper.showMomentSource());
        switchShowMomentSource.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                DebugHelper.setShowMomentSource(b);
            }
        });



        String server = PreferenceUtils.getString("server", null);
        if (!TextUtils.isEmpty(server)) {
            if (server.equals(serverList[0])) {
                server97.setChecked(true);
            } else if (server.equals(serverList[1])) {
                serverSingapore.setChecked(true);
            } else {
                serverAws.setChecked(true);
            }
        } else {
            serverAws.setChecked(true);
        }

        hostServerGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int index = 2;
                switch (radioGroup.getCheckedRadioButtonId()) {
                    case R.id.server_97:
                        index = 0;
                        break;
                    case R.id.server_singapore:
                        index = 1;
                        break;
                    case R.id.server_aws:
                        index = 2;
                        break;

                }
                PreferenceUtils.putString("server", serverList[index]);
            }
        });


    }
}
