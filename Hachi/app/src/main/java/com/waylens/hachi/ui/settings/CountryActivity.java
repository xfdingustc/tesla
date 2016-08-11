package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ViewSwitcher;

import com.android.volley.Response;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.AuthorizedJsonRequest;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.adapters.SimpleCountryAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by Xiaofei on 2016/5/18.
 */
public class CountryActivity extends BaseActivity {
    private static final String TAG = CountryActivity.class.getSimpleName();

    List<String> mCountryNameList = new ArrayList<>();

    List<Country> mCountryList = new ArrayList<>();

    SimpleCountryAdapter mAdapter;
    public static void launch(Activity activity) {
        Intent intent = new Intent(activity, CountryActivity.class);
        activity.startActivity(intent);
    }

    @BindView(R.id.vs)
    ViewSwitcher mVs;

    @BindView(R.id.rv_country_list)
    RecyclerView mRvCountryList;

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
        initViews();
    }



    private void initViews() {
        setContentView(R.layout.activity_country);
        mRvCountryList.setLayoutManager(new LinearLayoutManager(this));
        setupToolbar();
        getCountryList();
    }


    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.country);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
//        getToolbar().inflateMenu(R.menu.menu_search);
//        mSearchView.setMenuItem(getToolbar().getMenu().findItem(R.id.action_search));
    }



    private void getCountryList() {
        AuthorizedJsonRequest request = new AuthorizedJsonRequest.Builder()
            .url(Constants.API_COUNTRY)
            .listner(new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mVs.showNext();
                    renderCountryList(response);
                }
            })
            .build();
        mRequestQueue.add(request);
    }

    private void renderCountryList(JSONObject response) {

        try {
            JSONArray country = response.getJSONArray("countries");
            for (int i = 0; i < country.length(); i++) {
                JSONObject object = country.getJSONObject(i);
                Country oneCountry = new Country();
                oneCountry.code = object.getString("code");
                oneCountry.name = object.getString("name");
                mCountryList.add(oneCountry);
                mCountryNameList.add(oneCountry.name);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mAdapter = new SimpleCountryAdapter(mCountryList, new SimpleCountryAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                Logger.t(TAG).d("on item clicked: " + position);
                Country country = mAdapter.getCountry(position);
                if(country != null) {
                    CityActivity.launch(CountryActivity.this, country.code, country.name);
                    finish();
                }
            }
        });
        mRvCountryList.setAdapter(mAdapter);
    }

    public static class Country {
        public String code;
        public String name;
    }
}
