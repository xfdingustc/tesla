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

import butterknife.Bind;

/**
 * Created by Xiaofei on 2016/2/17.
 */
public class BookmarkFragment2 extends BaseFragment {
    private static final String TAG = BookmarkFragment2.class.getSimpleName();
    private TabLayout mTabLayout;

    @Bind(R.id.clipListViewPager)
    ViewPager mViewPager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_bookmark2,
            savedInstanceState);
        mTabLayout = (TabLayout) getActivity().findViewById(R.id.tabs);
        setupViewPager();
        return view;
    }

    private void setupViewPager() {
        SimpleFragmentPagerAdapter adapter = new SimpleFragmentPagerAdapter(getActivity()
            .getFragmentManager());
        adapter.addFragment(new ClipListFragment(), getString(R.string.bookmark));
        adapter.addFragment(new ClipListFragment(), getString(R.string.all));

        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);

    }



    @Override
    public ViewPager getViewPager() {
        return mViewPager;
    }
}
