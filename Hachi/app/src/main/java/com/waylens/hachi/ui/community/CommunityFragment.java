package com.waylens.hachi.ui.community;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.RequestQueue;
import com.waylens.hachi.R;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.UserProfileActivity;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.ui.community.feed.FeedFragment;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.waylens.hachi.utils.VolleyUtil;

import butterknife.BindView;


/**
 * Created by Xiaofei on 2016/1/22.
 */
public class CommunityFragment extends BaseFragment implements FragmentNavigator{
    private static final String TAG = CommunityFragment.class.getSimpleName();

    private RequestQueue mRequestQueue;

    @BindView(R.id.viewpager)
    ViewPager mViewPager;
    private SimpleFragmentPagerAdapter mFeedPageAdapter;

    @Override
    protected String getRequestTag() {
        return TAG;
    }

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


    @Override
    public void onResume() {
        super.onResume();
        getToolbar().getMenu().clear();
        getToolbar().inflateMenu(R.menu.menu_community);
        if (!SessionManager.getInstance().isLoggedIn()) {
            getToolbar().getMenu().removeItem(R.id.my_account);
        }
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                UserProfileActivity.launch(getActivity(), SessionManager.getInstance().getUserId(), getToolbar());
                return false;
            }
        });
    }

    @Override
    public boolean onInterceptBackPressed() {
        Fragment fragment = mFeedPageAdapter.getItem(mViewPager.getCurrentItem());
        if (fragment instanceof FragmentNavigator) {
            return ((FragmentNavigator)fragment).onInterceptBackPressed();
        }
        return false;
    }


    private void setupViewPager() {
        mFeedPageAdapter = new FeedPageAdapter(getChildFragmentManager());
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_MY_FEED), getString(R.string.my_feed));
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_LATEST), getString(R.string.latest));
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(FeedFragment.FEED_TAG_STAFF_PICKS), getString(R.string
            .staff_picks));
        mViewPager.setAdapter(mFeedPageAdapter);



        //mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        getTablayout().setupWithViewPager(mViewPager);

        if (SessionManager.getInstance().isLoggedIn()) {
            mViewPager.setCurrentItem(0);
        } else {
            mViewPager.setCurrentItem(4);
        }

    }

    public void notifyDateChanged() {
        if (mFeedPageAdapter != null) {
            mFeedPageAdapter.notifyDataSetChanged();
        }
    }
}
