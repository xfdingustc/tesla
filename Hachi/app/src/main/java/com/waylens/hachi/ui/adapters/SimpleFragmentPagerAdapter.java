package com.waylens.hachi.ui.adapters;


import android.app.Fragment;
import android.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2015/7/22.
 */
public class SimpleFragmentPagerAdapter extends BaseFragmentStatePagerAdapter {
    private final List<Fragment> mFragmentList = new ArrayList<>();
    private final List<String> mFragmentTitles = new ArrayList<>();

    public SimpleFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void addFragment(Fragment fragment, String title) {
        mFragmentList.add(fragment);
        mFragmentTitles.add(title);
    }

    @Override
    public Fragment getItem(int position) {
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
