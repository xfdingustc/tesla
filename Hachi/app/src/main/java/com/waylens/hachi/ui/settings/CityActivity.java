package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ViewSwitcher;

import com.android.volley.Response;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.adapters.SimpleCityAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/5/18.
 */
public class CityActivity extends BaseActivity {

    private static final String TAG = CityActivity.class.getSimpleName();
    List<String> cityNameList = new ArrayList<>();
    List<City> cityList = new ArrayList<>();
    public static int cityLimit = 3000;
    private SimpleCityAdapter mAdapter;
    private String mCode;
    private String mName;

    public static void launch(Activity activity, String code, String name) {
        Intent intent = new Intent(activity, CityActivity.class);
        intent.putExtra("code", code);
        intent.putExtra("name", name);
        activity.startActivity(intent);
    }

    @BindView(R.id.vs)
    ViewSwitcher mVs;

    @BindView(R.id.rv_city_list)
    RecyclerView mRvCityList;

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
//                mAdapter.getFilter().filter(newText);
//                return true;
//            }
//        });
    }

    @Override
    protected void init() {
        super.init();
        Intent intent = getIntent();
        mCode = intent.getStringExtra("code");
        mName = intent.getStringExtra("name");
        initViews();
    }



    private void initViews() {
        setContentView(R.layout.activity_city);
        mRvCityList.setLayoutManager(new LinearLayoutManager(this));
        setupToolbar();
        getCityList();
    }




    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getToolbar().setTitle(mName);
//        getToolbar().inflateMenu(R.menu.menu_search);
//        mSearchView.setMenuItem(getToolbar().getMenu().findItem(R.id.action_search));
    }


    private void getCityList() {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_CITY + "?cc=" + mCode + "&limit=" + cityLimit)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mVs.showNext();
                    renderCityList(response);
                }
            })
            .build();
        mRequestQueue.add(request);
    }

    private void renderCityList(JSONObject response) {

        try {
            JSONArray cityArray = response.getJSONArray("cities");
            for (int i = 0; i < cityArray.length(); i++) {
                JSONObject oneCity = cityArray.getJSONObject(i);
                City city = new City();
                city.id = oneCity.getLong("id");
                city.name = oneCity.getString("name");
                cityList.add(city);
                cityNameList.add(city.name);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mAdapter = new SimpleCityAdapter(cityList, new SimpleCityAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                City city = mAdapter.getCity(position);
                if (city != null)
                    changeUserCity(city);
            }
        });

        mRvCityList.setAdapter(mAdapter);
    }

    private void changeUserCity(City city) {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_USER_PROFILE)
            .postBody("cityID", city.id)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Logger.t(TAG).json(response.toString());
                    finish();
                }
            })

            .build();
        mRequestQueue.add(request);
    }

    public static class City {
        public long id;
        public String name;
    }
}
