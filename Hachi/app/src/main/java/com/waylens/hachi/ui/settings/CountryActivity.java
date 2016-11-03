package com.waylens.hachi.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ViewSwitcher;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.HachiService;
import com.waylens.hachi.rest.bean.Country;
import com.waylens.hachi.rest.response.CountryListResponse;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.adapters.SimpleCountryAdapter;
import com.waylens.hachi.utils.rxjava.SimpleSubscribe;

import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/5/18.
 */
public class CountryActivity extends BaseActivity {
    private static final String TAG = CountryActivity.class.getSimpleName();

    private SimpleCountryAdapter mAdapter;

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
    }


    private void getCountryList() {
        HachiService.createHachiApiService().getCountryListRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<CountryListResponse>() {
                @Override
                public void onNext(CountryListResponse countryListResponse) {
                    mVs.showNext();
                    renderCountryList(countryListResponse);
                }
            });
    }

    private void renderCountryList(CountryListResponse response) {
        mAdapter = new SimpleCountryAdapter(response.countries, new SimpleCountryAdapter.OnListItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                Logger.t(TAG).d("on item clicked: " + position);
                Country country = mAdapter.getCountry(position);
                if (country != null) {
                    CityActivity.launch(CountryActivity.this, country.code, country.name);
                    finish();
                }
            }
        });
        mRvCountryList.setAdapter(mAdapter);
    }


}
