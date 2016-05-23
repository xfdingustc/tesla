package com.waylens.hachi.ui.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.ui.activities.MainActivity;

/**
 * Created by Xiaofei on 2016/5/23.
 */
public class WifiSettingFragment extends PreferenceFragment {
    private static final String TAG = WifiSettingFragment.class.getSimpleName();
    private VdtCamera mVdtCamera;
    private Preference mWifiMode;
    private Preference mChooseWifi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_wifi_setting);
        init();
    }

    private void init() {
        mVdtCamera = VdtCameraManager.getManager().getCurrentCamera();
        if (mVdtCamera == null) {
            return;
        }

        initWifiModePreference();
        initChangeWifiPreference();


    }



    private void initWifiModePreference() {
        mWifiMode = findPreference("mode");
        int wifiMode = 0;
        switch (mVdtCamera.getWifiMode()) {
            case VdtCamera.WIFI_MODE_AP:
                mWifiMode.setSummary(R.string.access_point);
                wifiMode = 0;
                break;
            case VdtCamera.WIFI_MODE_CLIENT:
                mWifiMode.setSummary(R.string.client);
                wifiMode = 1;
                break;
            case VdtCamera.WIFI_MODE_OFF:
                mWifiMode.setSummary(R.string.off);
                wifiMode = 2;
                break;

        }

        final int wifiModeIndex = wifiMode;
        mWifiMode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.change_wifi_mode)
                    .items(R.array.wifi_mode)
                    .itemsCallbackSingleChoice(wifiModeIndex, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                            return false;
                        }
                    })
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            int selectIndex = dialog.getSelectedIndex();
                            if (selectIndex == wifiModeIndex) {
                                return;
                            }

                            showChangeWifiModeAlertDialog(selectIndex);
                        }


                    })
                    .show();


                return false;
            }
        });
    }

    private void initChangeWifiPreference() {
        mChooseWifi = findPreference("wifi_list");
        mChooseWifi.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ChooseWifiActivity.launch(getActivity());
                return true;
            }
        });
    }


    private void showChangeWifiModeAlertDialog(final int selectIndex) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
            .title(R.string.change_wifi_mode)
            .content(R.string.change_wifi_mode_alert)
            .positiveText(android.R.string.ok)
            .negativeText(android.R.string.cancel)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    doChangeWifiMode(selectIndex);
                }
            })
            .show();
    }

    private void doChangeWifiMode(int selectIndex) {
        Logger.t(TAG).d("change wifi mode to " + selectIndex);
        mVdtCamera.setWifiMode(selectIndex);
        MainActivity.launch(getActivity());
    }


}
