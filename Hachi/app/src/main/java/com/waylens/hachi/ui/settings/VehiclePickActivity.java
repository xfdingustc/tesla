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
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.adapters.SimpleCommonAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

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

    List<Maker> mMakerList = new ArrayList<>();
    List<Model> mModelList = new ArrayList<>();
    List<ModelYear> mYearList = new ArrayList<>();

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
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_MAKER)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mViewAnimator.setDisplayedChild(1);
                    renderMakerList(response);
                }
            })
            .build();
        mRequestQueue.add(request);
    }

    private void renderMakerList(JSONObject response) {

        try {
            JSONArray country = response.getJSONArray("makers");
            for (int i = 0; i < country.length(); i++) {
                JSONObject object = country.getJSONObject(i);
                Maker oneMaker = new Maker();
                oneMaker.makerID = object.getLong("makerID");
                oneMaker.makerName = object.getString("makerName");
                mMakerList.add(oneMaker);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mMakerAdapter = new MakerAdapter(mMakerList, new MakerAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                Logger.t(TAG).d("on item clicked: " + position);
                Maker maker = mMakerAdapter.getItem(position);
                if (maker != null) {
                    vehicleMaker = maker.getName();
                    getModelList(maker.makerID);
                }
            }
        });
        mRvContentList.setAdapter(mMakerAdapter);
    }

    private void getModelList(long makerID) {
        mViewAnimator.setDisplayedChild(0);
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_MODEL + "?maker=" + makerID)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mViewAnimator.setDisplayedChild(1);
                    renderModelList(response);
                    mCurrentStep = STEP_MODEL;
//                    if (mSearchView != null) {
//                        mSearchView.closeSearch();
//                    }
                }
            })
            .build();
        mRequestQueue.add(request);
    }

    private void renderModelList(JSONObject response) {
        try {
            JSONArray country = response.getJSONArray("models");
            for (int i = 0; i < country.length(); i++) {
                JSONObject object = country.getJSONObject(i);
                Model oneModel = new Model();
                oneModel.modelID = object.getLong("modelID");
                oneModel.modelName = object.getString("modelName");
                mModelList.add(oneModel);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mModelAdapter = new ModelAdapter(mModelList, new ModelAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                Logger.t(TAG).d("on item clicked: " + position);
                Model model = mModelAdapter.getItem(position);
                if (model != null) {
                    vehicleModel = model.getName();
                    getYearList(model.modelID);
                }
            }
        });
        mRvContentList.setAdapter(mModelAdapter);
    }

    private void getYearList(long model) {
        mViewAnimator.setDisplayedChild(0);
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_MODEL_YEAR + "?model=" + model)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mViewAnimator.setDisplayedChild(1);
                    renderYearList(response);
                    mCurrentStep = STEP_YEAR;
//                    if (mSearchView != null) {
//                        mSearchView.closeSearch();
//                    }
                }
            })
            .build();
        mRequestQueue.add(request);
    }

    private void renderYearList(JSONObject response) {
        try {
            JSONArray country = response.getJSONArray("years");
            for (int i = 0; i < country.length(); i++) {
                JSONObject object = country.getJSONObject(i);
                ModelYear oneModelYear = new ModelYear();
                oneModelYear.modelYearID = object.getLong("modelYearID");
                oneModelYear.year = object.getInt("year");
                mYearList.add(oneModelYear);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mYearAdapter = new ModelYearAdapter(mYearList, new ModelYearAdapter.OnListItemClickListener() {
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
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_VEHICLE)
            .postBody("modelYearID", modelYearID)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).json(response.toString());
                    Intent intent = getIntent();
                    intent.putExtra(VEHICLE_MAKER, vehicleMaker);
                    intent.putExtra(VEHICLE_MODEL, vehicleModel);
                    intent.putExtra(VEHICLE_YEAR, vehicleYear);
                    setResult(RESULT_OK, intent);
                    Logger.t(TAG).d("set result");
                    Logger.t(TAG).d(vehicleMaker + vehicleModel + vehicleYear);
                    finish();
                }
            })
            .errorListener(new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Logger.t(TAG).d(error.toString());
                    Intent intent = getIntent();
                    intent.putExtra(VEHICLE_MAKER, vehicleMaker);
                    intent.putExtra(VEHICLE_MODEL, vehicleModel);
                    intent.putExtra(VEHICLE_YEAR, vehicleYear);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            })
            .build();
        mRequestQueue.add(request);

    }


    public class Maker {
        public long makerID;
        public String makerName;

        public String getName() {
            return makerName;
        }
    }

    public class Model {
        public long modelID;
        public String modelName;

        public String getName() {
            return modelName;
        }
    }

    public class ModelYear {
        public long modelYearID;
        public int year;

        public String getName() {
            return String.valueOf(year);
        }
    }

    public class MakerAdapter extends SimpleCommonAdapter<Maker> {

        public MakerAdapter(List<Maker> itemList, OnListItemClickListener listener) {
            super(itemList, listener);
        }

        @Override
        public String getName(Maker maker) {
            return maker.getName();
        }
    }

    public class ModelAdapter extends SimpleCommonAdapter<Model> {

        public ModelAdapter(List<Model> itemList, OnListItemClickListener listener) {
            super(itemList, listener);
        }

        @Override
        public String getName(Model model) {
            return model.getName();
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
