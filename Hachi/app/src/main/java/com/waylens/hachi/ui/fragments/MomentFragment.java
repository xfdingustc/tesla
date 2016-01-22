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
public class MomentFragment extends BaseFragment {
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
        View view = createFragmentView(inflater, container, R.layout.fragment_moment,
            savedInstanceState);
        mTabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
        setupViewPager();
        return view;
    }

    private void setupViewPager() {
        SimpleFragmentPagerAdapter adapter = new SimpleFragmentPagerAdapter
            (getActivity().getFragmentManager());

        adapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_MY_FEED), getString(R.string.my_feed));
        adapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_ME), getString(R
            .string.me));
        adapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_LIKES), getString(R.string
            .likes));
        adapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_STAFF_PICKS), getString(R.string
            .staff_picks));
        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }
}
