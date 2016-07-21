package com.waylens.hachi.ui.adapters;


import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;

import com.waylens.hachi.ui.fragments.BaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/7/22.
 */
public class SimpleFragmentPagerAdapter extends FragmentStatePagerAdapter {
    private final List<BaseFragment> mFragmentList = new ArrayList<>();
    private final List<String> mFragmentTitles = new ArrayList<>();

    public SimpleFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void addFragment(BaseFragment fragment) {
        addFragment(fragment, "");
    }

    public void addFragment(BaseFragment fragment, String title) {
        mFragmentList.add(fragment);
        mFragmentTitles.add(title);
    }

    @Override
    public BaseFragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitles.get(position);
    }
}
