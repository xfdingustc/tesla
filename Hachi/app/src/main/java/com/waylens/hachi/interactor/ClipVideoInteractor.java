package com.waylens.hachi.interactor;

import com.waylens.hachi.ui.fragments.BaseFragment;

import java.util.List;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public interface ClipVideoInteractor {
    List<BaseFragment> getPagerFragments();

    List<Integer> getFragmentTitlesRes();
}
