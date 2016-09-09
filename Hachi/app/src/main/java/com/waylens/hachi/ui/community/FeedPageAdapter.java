package com.waylens.hachi.ui.community;

import android.app.FragmentManager;

import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.community.feed.FeedFragment;
import com.waylens.hachi.ui.authorization.SignUpEntryFragment;

/**
 * Created by Richard on 3/14/16.
 */
public class FeedPageAdapter extends SimpleFragmentPagerAdapter {

    boolean mOriginalLoginStatus;

    public FeedPageAdapter(FragmentManager fm) {
        super(fm);
        mOriginalLoginStatus = SessionManager.getInstance().isLoggedIn();
    }

    @Override
    public BaseFragment getItem(int position) {
        BaseFragment fragment = (BaseFragment) super.getItem(position);
            return fragment;
/*        if (fragment.isLoginRequired() && !SessionManager.getInstance().isLoggedIn()) {
            return new SignUpEntryFragment();
        } else {
            return fragment;
        }*/
    }

    @Override
    public int getItemPosition(Object object) {
/*      if (!mOriginalLoginStatus && SessionManager.getInstance().isLoggedIn()) {
            return POSITION_NONE;
        }*/
        return super.getItemPosition(object);
    }
}