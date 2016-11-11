package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.dialogs.DialogHelper;
import com.waylens.hachi.utils.DataCleanManager;
import com.waylens.hachi.utils.PreferenceUtils;
import com.waylens.hachi.utils.SettingHelper;
import com.waylens.hachi.utils.StringUtils;
import com.waylens.hachi.utils.ThemeHelper;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Xiaofei on 2016/9/28.
 */

public class SettingActivity extends BaseActivity {
    private static final String TAG = SettingActivity.class.getSimpleName();

    @BindView(R.id.unit)
    TextView tvUnit;

    @BindView(R.id.btn_logout)
    Button btnLogout;

    @BindView(R.id.cache_size)
    TextView cacheSize;

    @OnClick(R.id.ll_unit)
    public void onUnitClicked() {
        DialogHelper.showSwitchUnitDialog(this, new DialogHelper.OnPositiveClickListener() {
            @Override
            public void onPositiveClick() {
                refreshUnits();
            }
        });

    }

    @OnClick(R.id.btn_logout)
    public void onBtnLogoutClicked() {
        DialogHelper.showSignoutConfirmDialog(this, new DialogHelper.OnPositiveClickListener() {
            @Override
            public void onPositiveClick() {
                refreshBtnLogout();
            }
        });
    }

    @OnClick(R.id.ll_cache_clear)
    public void onCacheClearClicked() {
        DataCleanManager.clearAllCache(this);
        refreshCacheSize();
        Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.ll_gauge_setting)
    public void onGaugeSettingClicked() {
        GaugeSettingActivity.launch(this);
    }



    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, SettingActivity.class);
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
        setContentView(R.layout.activity_setting);
        setupToolbar();

        refreshUnits();
        refreshBtnLogout();
        refreshCacheSize();
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.settings);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void refreshUnits() {
        boolean metricUnits = SettingHelper.isMetricUnit();
        if (metricUnits) {
            tvUnit.setText(R.string.metric_units);
        } else {
            tvUnit.setText(R.string.imperial);
        }
    }




    private void refreshBtnLogout() {
        if (SessionManager.getInstance().isLoggedIn()) {
            btnLogout.setVisibility(View.VISIBLE);
        } else {
            btnLogout.setVisibility(View.GONE);
        }

    }

    private void refreshCacheSize() {
        try {
            String cache = DataCleanManager.getTotalCacheSize(this);
            cacheSize.setText(cache);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
