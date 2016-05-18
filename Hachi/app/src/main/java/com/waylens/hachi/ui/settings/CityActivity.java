package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.adapters.SimpleStringAdapter;

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
    private String mCode;
    private String mName;

    public static void launch(Activity activity, String code, String name) {
        Intent intent = new Intent(activity, CityActivity.class);
        intent.putExtra("code", code);
        intent.putExtra("name", name);
        activity.startActivity(intent);
    }

    @BindView(R.id.rv_city_list)
    RecyclerView mRvCityList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
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
    }


    private void getCityList() {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_CITY + "?cc=" + mCode)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
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

        SimpleStringAdapter adapter = new SimpleStringAdapter(cityNameList, new SimpleStringAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                changeUserCity(cityList.get(position));
            }
        });

        mRvCityList.setAdapter(adapter);
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

    private static class City {
        long id;
        String name;
    }
}
