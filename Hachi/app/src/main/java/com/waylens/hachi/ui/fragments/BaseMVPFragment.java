package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public abstract class BaseMVPFragment extends BaseFragment {


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  super.createFragmentView(inflater, container, getContentViewLayoutId(), savedInstanceState);
        init();
        return view;
    }



    protected abstract void init();

    protected abstract int getContentViewLayoutId();
}
