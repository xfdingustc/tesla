package com.waylens.hachi.ui.welcome;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.fragments.BaseFragment;

/**
 * Created by Xiaofei on 2016/7/5.
 */
public class Welcome1Fragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_welcome1, savedInstanceState);
        return view;
    }
}
