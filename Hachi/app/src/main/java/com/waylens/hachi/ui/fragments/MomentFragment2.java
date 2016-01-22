package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.RequestQueue;
import com.waylens.hachi.R;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.utils.VolleyUtil;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/1/22.
 */
public class MomentFragment2 extends BaseFragment {
    private TabLayout mTabLayout;

    RequestQueue mRequestQueue;

    @Bind(R.id.viewpager)
    ViewPager mViewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_moment2,
            savedInstanceState);
        mTabLayout = (TabLayout)getActivity().findViewById(R.id.tabs);
        setupViewPager();
        return view;
    }

    private void setupViewPager() {
        SimpleFragmentPagerAdapter adapter = new SimpleFragmentPagerAdapter
            (getActivity().getFragmentManager());
        adapter.addFragment(new MomentFragment(), getString(R.string.my_feed));
        adapter.addFragment(new MomentFragment(), getString(R.string.me));
        adapter.addFragment(new MomentFragment(), getString(R.string.likes));
        adapter.addFragment(new MomentFragment(), getString(R.string.staff_picks));
        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }
}