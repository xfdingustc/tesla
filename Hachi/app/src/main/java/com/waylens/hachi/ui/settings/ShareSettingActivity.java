package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;

/**
 * Created by lshw on 2016/9/13.
 */
public class ShareSettingActivity extends BaseActivity {

    public static final String TAG = ShareSettingActivity.class.getSimpleName();

    public static final String LOCATION_CHECKED = "locationChecked";
    public static final String VEHICLE_CHECKED =  "vehicleChecked";
    public static final String VEHICLE_MAKER = "vehicleMaker";
    public static final String VEHICLE_MODEL = "vehicleModel";
    public static final String VEHICLE_YEAR = "vehicleYear";

    private String location;

    private String vehicleMaker;

    private String vehicleModel;

    private int vehicleYear;

    public boolean isLocationChecked = true;

    public boolean isVehicleChecked = true;

    public void setVehicleMaker(String vehicleMaker) {
        this.vehicleMaker = vehicleMaker;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public void setVehicleYear(int vehicleYear) {
        this.vehicleYear = vehicleYear;
    }

    public static void launch(Activity activity, String location, String maker, String model, int year, int requestCode) {
        Intent intent = new Intent(activity, ShareSettingActivity.class);
        intent.putExtra("location", location);
        intent.putExtra("vehicleMaker", maker);
        intent.putExtra("vehicleModel", model);
        intent.putExtra("vehicleYear", year);
        activity.startActivityForResult(intent, requestCode);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        location = intent.getStringExtra("location");
        vehicleMaker = intent.getStringExtra("vehicleMaker");
        vehicleModel = intent.getStringExtra("vehicleModel");
        vehicleYear = intent.getIntExtra("vehicleYear", -1);
        init();
    }


    @Override
    protected void init() {
        super.init();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_share_setting);
        setupToolbar();
        ShareSettingFragment fragment = ShareSettingFragment.newInstance(location, vehicleMaker, vehicleModel, vehicleYear);
        getFragmentManager().beginTransaction().replace(R.id.share_setting, fragment).commit();

    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle("Setting");
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                intent.putExtra(LOCATION_CHECKED, isLocationChecked);
                intent.putExtra(VEHICLE_CHECKED, isVehicleChecked);
                intent.putExtra(VEHICLE_MAKER, vehicleMaker);
                intent.putExtra(VEHICLE_MODEL, vehicleModel);
                intent.putExtra(VEHICLE_YEAR, vehicleYear);
                Logger.t(TAG).d("maker:" + vehicleMaker + vehicleModel + vehicleYear);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }


    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        intent.putExtra(LOCATION_CHECKED, isLocationChecked);
        intent.putExtra(VEHICLE_CHECKED, isVehicleChecked);
        intent.putExtra(VEHICLE_MAKER, vehicleMaker);
        intent.putExtra(VEHICLE_MODEL, vehicleModel);
        intent.putExtra(VEHICLE_YEAR, vehicleYear);
        Logger.t(TAG).d("maker:" + vehicleMaker + vehicleModel + vehicleYear);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
