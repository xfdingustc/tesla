package com.waylens.hachi.interactor.impl;

import com.waylens.hachi.R;
import com.waylens.hachi.interactor.ClipVideoInteractor;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.ui.clips.ClipGridListFragment;
import com.waylens.hachi.ui.fragments.BaseFragment;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public class ClipVideoInteractorImpl implements ClipVideoInteractor {
    @Override
    public List<BaseFragment> getPagerFragments() {
        List<BaseFragment> fragments = new ArrayList<>();
        fragments.add(ClipGridListFragment.newInstance(Clip.TYPE_MARKED));
        fragments.add(ClipGridListFragment.newInstance(Clip.TYPE_BUFFERED));
        return fragments;
    }

    @Override
    public List<Integer> getFragmentTitlesRes() {
        List<Integer> titles = new ArrayList<>();
        titles.add(R.string.highlights);
        titles.add(R.string.lable_buffered_video);
        return titles;
    }


}
