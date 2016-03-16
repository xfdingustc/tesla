package com.waylens.hachi.ui.adapters;

import android.app.FragmentManager;

import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FeedFragment;
import com.waylens.hachi.ui.fragments.SignUpEntryFragment;

/**
 * Created by Richard on 3/14/16.
 */
public class FeedPageAdapter extends SimpleFragmentPagerAdapter {

    public FeedPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public BaseFragment getItem(int position) {
        FeedFragment fragment = (FeedFragment) super.getItem(position);
        if (fragment.isLoginRequired() && !SessionManager.getInstance().isLoggedIn()) {
            return new SignUpEntryFragment();
        } else {
            return fragment;
        }
    }
}
