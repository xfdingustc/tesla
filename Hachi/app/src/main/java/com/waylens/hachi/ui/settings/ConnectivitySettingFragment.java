package com.waylens.hachi.ui.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.waylens.hachi.R;


/**
 * Created by Xiaofei on 2016/5/11.
 */
public class ConnectivitySettingFragment extends PreferenceFragment {

    private Preference mBt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_connectivity_setting);
        initPreference();
    }

    private void initPreference() {
        initBtPreference();
    }

    private void initBtPreference() {
        mBt = findPreference("bluetooth");
        mBt.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                BluetoothSettingActivity.launch(getActivity());
                return true;
            }
        });
    }
}
