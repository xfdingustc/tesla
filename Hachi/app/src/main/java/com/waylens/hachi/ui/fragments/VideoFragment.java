package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
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

    @Bind(R.id.clipListViewPager)
    ViewPager mViewPager;

    private SimpleFragmentPagerAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_video,
                savedInstanceState);
        setupViewPager();
        return view;
    }


    @Override
    public void onDestroyView() {
        mViewPager.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void setupToolbar() {
        mToolbar.setTitle(R.string.video);
        super.setupToolbar();
    }

    private void setupViewPager() {
        mAdapter = new SimpleFragmentPagerAdapter(getChildFragmentManager());
        mAdapter.addFragment(BookmarkFragment.newInstance(Clip.TYPE_MARKED), getString(R.string
                .bookmark));
        mAdapter.addFragment(AllFootageFragment.newInstance(), getString(R.string
            .all_footage));
        mViewPager.setAdapter(mAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                BaseFragment fragment = mAdapter.getItem(position);
                fragment.onFragmentFocused(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public boolean onInterceptBackPressed() {
        BookmarkFragment fragment = (BookmarkFragment) mAdapter.getItem(mViewPager.getCurrentItem());
        return fragment.onInterceptBackPressed();
    }
}
