package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/2/18.
 */
public class ClipListFragment extends BaseFragment {

    public static ClipListFragment newInstance(int tag) {
        ClipListFragment fragment = new ClipListFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_clip_list,
            savedInstanceState);
        return view;
    }
}
