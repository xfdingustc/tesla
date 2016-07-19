package com.waylens.hachi.ui.fragments;

import android.os.Bundle;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public abstract class BaseLazyFragment extends BaseMVPFragment {
    private boolean isPrepared;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initPrepare();
    }

    private synchronized void initPrepare() {
        if (!isPrepared) {
            onFirstUserVisible();
            isPrepared = true;
        }
    }


    protected abstract void onFirstUserVisible();

    protected abstract boolean isBindEventBusHere();
}
