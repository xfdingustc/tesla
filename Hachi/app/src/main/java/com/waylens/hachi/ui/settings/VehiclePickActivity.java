package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ViewAnimator;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.Maker;
import com.waylens.hachi.rest.bean.Model;
import com.waylens.hachi.rest.bean.ModelYear;
import com.waylens.hachi.rest.body.AddVehicleBody;
import com.waylens.hachi.rest.response.MakerResponse;
import com.waylens.hachi.rest.response.ModelResponse;
import com.waylens.hachi.rest.response.ModelYearResponse;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.adapters.SimpleCommonAdapter;
import com.waylens.hachi.utils.ServerErrorHelper;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by lshw on 16/7/27.
 */
public class VehiclePickActivity extends BaseActivity {

    public static final String TAG = VehiclePickActivity.class.getSimpleName();
    public static final int STEP_MAKER = 0;
    public static final int STEP_MODEL = 1;
    public static final int STEP_YEAR = 2;

    public static final String VEHICLE_MAKER = "vehicleMaker";
    public static final String VEHICLE_MODEL = "vehicleModel";
    public static final String VEHICLE_YEAR = "vehicleYear";


    private int mCurrentStep = STEP_MAKER;
    private String vehicleMaker;
    private String vehicleModel;
    private int vehicleYear;




    private SimpleCommonAdapter<Maker> mMakerAdapter;
    private SimpleCommonAdapter<Model> mModelAdapter;
    private SimpleCommonAdapter<ModelYear> mYearAdapter;


    public static void launch(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, VehiclePickActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void launch(Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), VehiclePickActivity.class);
        fragment.startActivityForResult(intent, requestCode);
    }

    @BindView(R.id.va)
    ViewAnimator mViewAnimator;

    @BindView(R.id.rv_content_list)
    RecyclerView mRvContentList;

//    @BindView(R.id.search_view)
//    MaterialSearchView mSearchView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();


//        mSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                switch (mCurrentStep) {
//                    case STEP_MAKER:
//                        mMakerAdapter.getFilter().filter(newText);
//                        break;
//                    case STEP_MODEL:
//                        mModelAdapter.getFilter().filter(newText);
//                        break;
//                    case STEP_YEAR:
//                        mYearAdapter.getFilter().filter(newText);
//                        break;
//                }
//                return true;
//            }
//        });
    }

    @Override
    protected void init() {
        super.init();
        initViews();
    }


    private void initViews() {
        setContentView(R.layout.activity_vehicle_picker);
        mRvContentList.setLayoutManager(new LinearLayoutManager(this));
        setupToolbar();
        getMakerList();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.vehicle);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
//        getToolbar().inflateMenu(R.menu.menu_search);
//        mSearchView.setMenuItem(getToolbar().getMenu().findItem(R.id.action_search));
    }


    private void getMakerList() {
        mViewAnimator.setDisplayedChild(0);
        HachiService.createHachiApiService().getAllMarkerRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<MakerResponse>() {
                @Override
                public void onNext(MakerResponse makerResponse) {
                    mViewAnimator.setDisplayedChild(1);
                    renderMakerList(makerResponse);
                }
            });


    }

    private void renderMakerList(MakerResponse response) {
        mMakerAdapter = new MakerAdapter(response.makers, new MakerAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                Logger.t(TAG).d("on item clicked: " + position);
                Maker maker = mMakerAdapter.getItem(position);
                if (maker != null) {
                    vehicleMaker = maker.makerName;
                    getModelList(maker.makerID);
                }
            }
        });
        mRvContentList.setAdapter(mMakerAdapter);
    }

    private void getModelList(long makerID) {
        mViewAnimator.setDisplayedChild(0);
        HachiService.createHachiApiService().getModelByMakerRx(makerID)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<ModelResponse>() {
                @Override
                public void onNext(ModelResponse modelResponse) {
                    mViewAnimator.setDisplayedChild(1);
                    renderModelList(modelResponse);
                    mCurrentStep = STEP_MODEL;
                }
            });
    }

    private void renderModelList(ModelResponse response) {
        mModelAdapter = new ModelAdapter(response.models, new ModelAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                Logger.t(TAG).d("on item clicked: " + position);
                Model model = mModelAdapter.getItem(position);
                if (model != null) {
                    vehicleModel = model.modelName;
                    getYearList(model.modelID);
                }
            }
        });
        mRvContentList.setAdapter(mModelAdapter);
    }

    private void getYearList(long model) {
        mViewAnimator.setDisplayedChild(0);
        HachiService.createHachiApiService().getModelYearRx(model)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<ModelYearResponse>() {
                @Override
                public void onNext(ModelYearResponse modelYearResponse) {
                    mViewAnimator.setDisplayedChild(1);
                    renderYearList(modelYearResponse);
                    mCurrentStep = STEP_YEAR;
                }
            });
    }

    private void renderYearList(ModelYearResponse response) {
        mYearAdapter = new ModelYearAdapter(response.years, new ModelYearAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                Logger.t(TAG).d("on item clicked: " + position);
                ModelYear modelYear = mYearAdapter.getItem(position);
                if (modelYear != null) {
                    addVehicle(modelYear.modelYearID);
                    vehicleYear = modelYear.year;
                }
            }
        });
        mRvContentList.setAdapter(mYearAdapter);
    }

    public void addVehicle(long modelYearID) {
        AddVehicleBody addVehicleBody = new AddVehicleBody();
        addVehicleBody.modelYearID = modelYearID;
        HachiService.createHachiApiService().addUserVehicle(addVehicleBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<SimpleBoolResponse>() {
                @Override
                public void onNext(SimpleBoolResponse simpleBoolResponse) {
                    Intent intent = getIntent();
                    intent.putExtra(VEHICLE_MAKER, vehicleMaker);
                    intent.putExtra(VEHICLE_MODEL, vehicleModel);
                    intent.putExtra(VEHICLE_YEAR, vehicleYear);
                    setResult(RESULT_OK, intent);
                    Logger.t(TAG).d("set result");
                    Logger.t(TAG).d(vehicleMaker + vehicleModel + vehicleYear);
                    finish();
                }

                @Override
                public void onError(Throwable e) {
                    ServerErrorHelper.showErrorMessage(mViewAnimator, e);
                }
            });

    }



    public class MakerAdapter extends SimpleCommonAdapter<Maker> {

        public MakerAdapter(List<Maker> itemList, OnListItemClickListener listener) {
            super(itemList, listener);
        }

        @Override
        public String getName(Maker maker) {
            return maker.makerName;
        }
    }

    public class ModelAdapter extends SimpleCommonAdapter<Model> {

        public ModelAdapter(List<Model> itemList, OnListItemClickListener listener) {
            super(itemList, listener);
        }

        @Override
        public String getName(Model model) {
            return model.modelName;
        }
    }

    public class ModelYearAdapter extends SimpleCommonAdapter<ModelYear> {

        public ModelYearAdapter(List<ModelYear> itemList, OnListItemClickListener listener) {
            super(itemList, listener);
        }

        @Override
        public String getName(ModelYear modelYear) {
            return modelYear.getName();
        }
    }
}
