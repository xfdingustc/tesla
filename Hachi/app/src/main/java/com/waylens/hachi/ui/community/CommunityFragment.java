package com.waylens.hachi.ui.community;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.RequestQueue;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.FeedPageAdapter;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FeedFragment;
import com.waylens.hachi.utils.VolleyUtil;

import butterknife.BindView;


/**
 * Created by Xiaofei on 2016/1/22.
 */
public class CommunityFragment extends BaseFragment {

    RequestQueue mRequestQueue;

    @BindView(R.id.viewpager)
    ViewPager mViewPager;
    private SimpleFragmentPagerAdapter mFeedPageAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestQueue = VolleyUtil.newVolleyRequestQueue(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_moment, savedInstanceState);

        setupViewPager();
        return view;
    }

    @Override
    public void setupToolbar() {
        getToolbar().setTitle(R.string.moments);
        super.setupToolbar();
    }


    private void setupViewPager() {
        mFeedPageAdapter = new FeedPageAdapter(getChildFragmentManager());
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_MY_FEED), getString(R.string.my_feed));
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_ME), getString(R
            .string.me));
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_LIKES), getString(R.string
            .like));
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_STAFF_PICKS), getString(R.string
            .staff_picks));
        //adapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_ALL), getString(R.string
        //        .all));
        mViewPager.setAdapter(mFeedPageAdapter);

        //mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        getTablayout().setupWithViewPager(mViewPager);

        if (SessionManager.getInstance().isLoggedIn()) {
            mViewPager.setCurrentItem(0);
        } else {
            mViewPager.setCurrentItem(3);
        }

    }

    public void notifyDateChanged() {
        if (mFeedPageAdapter != null) {
            mFeedPageAdapter.notifyDataSetChanged();
        }
    }

}
