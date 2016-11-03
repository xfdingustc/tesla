package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ViewSwitcher;

import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiApi;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.City;
import com.waylens.hachi.rest.body.UserProfileBody;
import com.waylens.hachi.rest.response.CityList;
import com.waylens.hachi.rest.response.SimpleBoolResponse;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.adapters.SimpleCityAdapter;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import java.util.List;

import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/5/18.
 */
public class CityActivity extends BaseActivity {
    private static final String TAG = CityActivity.class.getSimpleName();

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
    }


    private void getCityList() {
        HachiApi hachiApi = HachiService.createHachiApiService();
        hachiApi.getCityListRx(mCode, cityLimit)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<CityList>() {
                @Override
                public void onNext(CityList cityList) {
                    mVs.showNext();
                    renderCityList(cityList.cities);
                }
            });

    }

    private void renderCityList(List<City> cityList) {

        mAdapter = new SimpleCityAdapter(cityList, new SimpleCityAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                City city = mAdapter.getCity(position);
                if (city != null) {
                    changeUserCity(city);
                }
            }
        });

        mRvCityList.setAdapter(mAdapter);
    }


    private void changeUserCity(City city) {
        HachiApi hachiApi = HachiService.createHachiApiService();
        UserProfileBody userProfileBody = new UserProfileBody();
        userProfileBody.cityID = (int) city.id;
        hachiApi.changeProfileRx(userProfileBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<SimpleBoolResponse>() {
                @Override
                public void onNext(SimpleBoolResponse simpleBoolResponse) {
                    finish();
                }
            });
    }


}
