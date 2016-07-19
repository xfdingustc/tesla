package com.waylens.hachi.ui.fragments;

import android.os.Bundle;
import android.view.View;

import com.waylens.hachi.loading.VaryViewHelperController;
import com.waylens.hachi.view.base.BaseView;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public abstract class BaseLazyFragment extends BaseMVPFragment implements BaseView {
    private boolean isPrepared;

    private VaryViewHelperController mVaryViewHelpercontroller = null;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (getLoadingTargetView() != null) {
            mVaryViewHelpercontroller = new VaryViewHelperController(getLoadingTargetView());
        }
    }

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

    protected abstract View getLoadingTargetView();


    @Override
    public void showLoading(String msg) {
        toggleShowLoading(true, null);
    }

    @Override
    public void hideLoading() {
        toggleShowLoading(false, null);
    }


    @Override
    public void showCameraDisconnect() {
        toggleShowCameraDisconnect(true);
    }


    @Override
    public void hideCameraDisconnect() {
        toggleShowCameraDisconnect(false);
    }

    protected void toggleShowLoading(boolean toggle, String msg) {
        if (mVaryViewHelpercontroller == null) {
            throw new IllegalArgumentException("You must return a right target view for loading");
        }

        if (toggle) {
            mVaryViewHelpercontroller.showLoading(msg);
        } else {
            mVaryViewHelpercontroller.restore();
        }
    }


    protected void toggleShowCameraDisconnect(boolean toggle) {
        if (mVaryViewHelpercontroller == null) {
            throw new IllegalArgumentException("You must return a right target view for loading");
        }

        if (toggle) {
            mVaryViewHelpercontroller.showCameraDisconnected();
        } else {
            mVaryViewHelpercontroller.restore();
        }
    }
}
