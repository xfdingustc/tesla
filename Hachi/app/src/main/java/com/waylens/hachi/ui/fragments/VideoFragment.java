package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.vdb.Clip;

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/2/17.
 */
public class VideoFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = VideoFragment.class.getSimpleName();
    private TabLayout mTabLayout;

    @Bind(R.id.clipListViewPager)
    ViewPager mViewPager;

    private SimpleFragmentPagerAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_bookmark2,
                savedInstanceState);
        mTabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
        setupViewPager();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        setTitle(R.string.video);
    }

    @Override
    public void onDestroyView() {
        mViewPager.setAdapter(null);
        super.onDestroyView();
    }

    private void setupViewPager() {
        mAdapter = new SimpleFragmentPagerAdapter(getChildFragmentManager());
        mAdapter.addFragment(ClipListFragment.newInstance(Clip.TYPE_MARKED), getString(R.string
                .bookmark));
        mAdapter.addFragment(ClipListFragment.newInstance(Clip.TYPE_BUFFERED), getString(R.string.all));
        mViewPager.setAdapter(mAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public ViewPager getViewPager() {
        return mViewPager;
    }

    @Override
    public boolean onInterceptBackPressed() {
        ClipListFragment fragment = (ClipListFragment) mAdapter.getItem(mViewPager.getCurrentItem());
        return fragment.onInterceptBackPressed();
    }
}
