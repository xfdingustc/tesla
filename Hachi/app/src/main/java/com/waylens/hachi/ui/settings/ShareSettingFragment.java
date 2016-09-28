package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.GaugeSettingManager;

import java.util.Map;

/**
 * Created by lshw on 16/9/13.
 */
public class ShareSettingFragment extends PreferenceFragment{

    public static final String TAG = ShareSettingFragment.class.getSimpleName();

    public static final int REQUEST_PICKCAR = 101;

    private SwitchPreference mDrivingLocation;

    private SwitchPreference mVehicleInfo;

    private Preference mVehicleMaker;

    private Preference mVehicleModel;

    private Preference mVehicleYear;

    private String mShareLocation;

    private String mShareMaker;

    private String mShareModel;

    private int mShareYear;

    private boolean mAutoDetected;

    static ShareSettingFragment newInstance(String location, String maker, String model, int year, boolean autoDetected) {
        ShareSettingFragment fragment = new ShareSettingFragment();
        Bundle bundle = new Bundle();
        bundle.putString("location", location);
        bundle.putString("vehicleMaker", maker);
        bundle.putString("vehicleModel", model);
        bundle.putInt("vehicleYear", year);
        bundle.putBoolean("autoDetected", autoDetected);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mShareLocation = bundle.getString("location");
        mShareMaker = bundle.getString("vehicleMaker");
        mShareModel = bundle.getString("vehicleModel");
        mShareYear = bundle.getInt("vehicleYear");
        mAutoDetected = bundle.getBoolean("autoDetected", false);
        addPreferencesFromResource(R.xml.pref_share_setting);
        initPreference();
    }

    public void initPreference() {
        mDrivingLocation = (SwitchPreference) findPreference("driving_location");
        mVehicleInfo = (SwitchPreference) findPreference("vehicle_info");
        mVehicleMaker = findPreference("vehicle_maker");
        mVehicleModel = findPreference("vehicle_model");
        mVehicleYear = findPreference("vehicle_year");

        mVehicleMaker.setShouldDisableView(true);
        mVehicleModel.setShouldDisableView(true);
        mVehicleYear.setShouldDisableView(true);
        mVehicleMaker.setEnabled(true);
        mVehicleModel.setEnabled(true);
        mVehicleYear.setEnabled(true);

        mDrivingLocation.setChecked(true);
        mVehicleInfo.setChecked(true);

        final Map<String, String> gaugeSettingMap = GaugeSettingManager.getManager().getGaugeSettingMap();
        mDrivingLocation.setSummary(mShareLocation);
        mVehicleMaker.setSummary(mShareMaker);
        mVehicleModel.setSummary(mShareModel);
        if (mShareYear > 0) {
            mVehicleYear.setSummary(String.valueOf(mShareYear));
        }
        mDrivingLocation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (gaugeSettingMap.get("showGps").equals("")) {
                    ((ShareSettingActivity) getActivity()).isLocationChecked = !mDrivingLocation.isChecked();
                    return true;
                } else {
                    Snackbar.make(ShareSettingFragment.this.getView(), getString(R.string.geoTag_setting_notification), Snackbar.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        mVehicleInfo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                ((ShareSettingActivity)getActivity()).isVehicleChecked = !mVehicleInfo.isChecked();
                if (mVehicleInfo.isChecked()) {
                    mVehicleMaker.setEnabled(false);
                    mVehicleModel.setEnabled(false);
                    mVehicleYear.setEnabled(false);
                } else {
                    mVehicleMaker.setEnabled(true);
                    mVehicleModel.setEnabled(true);
                    mVehicleYear.setEnabled(true);
                }
                return true;
            }
        });

        if (!mAutoDetected) {
            mVehicleMaker.setEnabled(true);
            if (mShareMaker == null) {
                mVehicleMaker.setSummary("Click to select one.");
            }
            mVehicleMaker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    VehiclePickActivity.launch(ShareSettingFragment.this, REQUEST_PICKCAR);
                    return true;
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICKCAR:
                if (resultCode == Activity.RESULT_OK) {
                    mShareMaker = data.getStringExtra(VehiclePickActivity.VEHICLE_MAKER);
                    mShareModel = data.getStringExtra(VehiclePickActivity.VEHICLE_MODEL);
                    mShareYear = data.getIntExtra(VehiclePickActivity.VEHICLE_YEAR, 0);
                    ((ShareSettingActivity)getActivity()).setVehicleMaker(mShareMaker);
                    ((ShareSettingActivity)getActivity()).setVehicleModel(mShareModel);
                    ((ShareSettingActivity)getActivity()).setVehicleYear(mShareYear);
                    Logger.t(TAG).d("maker:" + mShareMaker + mShareModel + mShareYear);

                    mVehicleMaker.setSummary(mShareMaker);
                    mVehicleModel.setSummary(mShareModel);
                    mVehicleYear.setSummary(String.valueOf(mShareYear));
                }
                break;
            default:
                break;
        }
    }
}
