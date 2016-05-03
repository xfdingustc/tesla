package com.waylens.hachi.ui.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.ManualSetupActivity;
import com.waylens.hachi.ui.activities.VersionCheckActivity;

/**
 * Created by Xiaofei on 2016/5/3.
 */
public class SettingPrefFragment extends PreferenceFragment {
    private static final String TAG = SettingPrefFragment.class.getSimpleName();
    private Preference mAccountPref;
    private Preference mGeneralPref;
    private Preference mCameraPref;
    private Preference mAddCameraPref;
    private Preference mHelpPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_setting);
        initPreference();
    }

    private void initPreference() {
        mGeneralPref = findPreference("general");
        mAccountPref = findPreference("account");
        mCameraPref = findPreference("camera");
        mAddCameraPref = findPreference("addCamera");
        mHelpPref = findPreference("help");

//        mGeneralPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                return false;
//            }
//        });

        Logger.t(TAG).d("settTTTTTTTTTTTT");

        mAccountPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Logger.t(TAG).d("on account clicked");
                AccountActivity.launch(getActivity());
                return true;
            }
        });

//        mCameraPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                return false;
//            }
//        });

        mAddCameraPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ManualSetupActivity.launch(getActivity());
                return true;
            }
        });



        mHelpPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                VersionCheckActivity.launch(getActivity());
                return true;
            }
        });
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Logger.t(TAG).d("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
