package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.Vehicle;
import com.waylens.hachi.rest.body.VehicleListResponse;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.settings.adapters.CarListAdapter;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by lshw on 2016/9/13.
 */
public class ShareSettingActivity extends BaseActivity implements CarListAdapter.OnCarItemClickedListener{

    public static final String TAG = ShareSettingActivity.class.getSimpleName();

    public static final String LOCATION_CHECKED = "locationChecked";
    public static final String VEHICLE_CHECKED =  "vehicleChecked";
    public static final String VEHICLE_MAKER = "vehicleMaker";
    public static final String VEHICLE_MODEL = "vehicleModel";
    public static final String VEHICLE_YEAR = "vehicleYear";

    public static final String EXTRA_VEHICLE = "extraVehicle";
    public static final int REQUEST_PICKCAR = 0x1001;

    private String location;

    private String vehicleMaker;

    private String vehicleModel;

    private int vehicleYear;

    private Vehicle selectedVehicle;

    @BindView(R.id.rv_car_list)
    RecyclerView rvCarList;

    @OnClick(R.id.layout_add_car)
    public void onAddCar() {
        VehiclePickActivity.launch(ShareSettingActivity.this, REQUEST_PICKCAR);
    }

    private CarListAdapter mAdapter;

    private SessionManager mSessionManager;

    public static void launch(Activity activity, Vehicle vehicle,int requestCode) {
        Intent intent = new Intent(activity, ShareSettingActivity.class);
        intent.putExtra(EXTRA_VEHICLE, vehicle);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        selectedVehicle = (Vehicle) intent.getSerializableExtra(EXTRA_VEHICLE);
        init();
    }

    @Override
    protected void init() {
        super.init();
        mSessionManager = SessionManager.getInstance();
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_share_setting);
        mAdapter = new CarListAdapter(this, this);
        rvCarList.setLayoutManager(new LinearLayoutManager(this));
        rvCarList.setAdapter(mAdapter);
        mAdapter.setCarList(mSessionManager.getVehicles(), selectedVehicle);
        updateVehicle();
        setupToolbar();
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle("Back");
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                Logger.t(TAG).d("maker:" + vehicleMaker + vehicleModel + vehicleYear);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICKCAR:
                if (resultCode == Activity.RESULT_OK) {
                    String maker = data.getStringExtra(VehiclePickActivity.VEHICLE_MAKER);
                    String model = data.getStringExtra(VehiclePickActivity.VEHICLE_MODEL);
                    int year = data.getIntExtra(VehiclePickActivity.VEHICLE_YEAR, 0);
                    long modelYearId = data.getLongExtra(VehiclePickActivity.VEHICLE_MODEL_YEAR_ID, -1);
                    if (maker != null && model != null && year != 0 && modelYearId != -1) {
                        Vehicle newVehicle = new Vehicle();
                        newVehicle.maker = maker;
                        newVehicle.model = model;
                        newVehicle.year = year;
                        newVehicle.modelYearID = modelYearId;
                        mSessionManager.addVehicle(newVehicle);
                        mAdapter.setCarList(mSessionManager.getVehicles(), selectedVehicle);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void updateVehicle() {
        Logger.t(TAG).d("update Vehicle!");

        HachiService.createHachiApiService().getUserVehicleListRx(SessionManager.getInstance().getUserId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleSubscribe<VehicleListResponse>() {
                    @Override
                    public void onNext(VehicleListResponse vehicleListResponse) {
                        List<Vehicle> localVehicles = mSessionManager.getVehicles();
                        if (localVehicles == null || localVehicles.size() != vehicleListResponse.vehicles.size() || !localVehicles.containsAll(vehicleListResponse.vehicles)) {
                            mSessionManager.updateVehicles(vehicleListResponse.vehicles);
                            mAdapter.setCarList(vehicleListResponse.vehicles, selectedVehicle);
                        }
                    }
                });
    }


    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onItemClicked(Vehicle item) {
        mAdapter.notifyDataSetChanged();
        Intent intent = getIntent();
        intent.putExtra(EXTRA_VEHICLE, item);
        setResult(RESULT_OK, intent);
        finish();
    }
}
