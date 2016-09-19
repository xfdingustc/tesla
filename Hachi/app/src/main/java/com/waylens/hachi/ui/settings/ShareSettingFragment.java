package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

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

    static ShareSettingFragment newInstance(String location, String maker, String model, int year) {
        ShareSettingFragment fragment = new ShareSettingFragment();
        Bundle bundle = new Bundle();
        bundle.putString("location", location);
        bundle.putString("vehicleMaker", maker);
        bundle.putString("vehicleModel", model);
        bundle.putInt("vehicleYear", year);
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

        Map<String, String> gaugeSettingMap = GaugeSettingManager.getManager().getGaugeSettingMap();
        if (!gaugeSettingMap.get("showGps").equals("")) {
            mDrivingLocation.setEnabled(false);
        }
        mDrivingLocation.setSummary(mShareLocation);
        mVehicleMaker.setSummary(mShareMaker);
        mVehicleModel.setSummary(mShareModel);
        if (mShareYear > 0) {
            mVehicleYear.setSummary(String.valueOf(mShareYear));
        }
        mDrivingLocation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                ((ShareSettingActivity)getActivity()).isLocationChecked = mDrivingLocation.isChecked();
                return true;
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

        if (mShareMaker == null) {
            mVehicleMaker.setEnabled(true);
            mVehicleMaker.setSummary("Click to select one.");
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
                if (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED) {
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
