package com.waylens.hachi.ui.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/5/13.
 */
public class WaylensCloudFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_waylens_cloud);
    }
}
