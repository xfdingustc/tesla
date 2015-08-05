package com.waylens.hachi.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;

import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2015/8/4.
 */
public class AccountFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       return createFragmentView(inflater, container, R.layout.fragment_account, savedInstanceState);
    }
}
