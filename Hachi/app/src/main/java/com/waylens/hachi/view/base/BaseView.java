package com.waylens.hachi.view.base;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public interface BaseView {
    void showLoading(String msg);

    void hideLoading();

    void showEmpty();

    void showError(String msg);

    void showCameraDisconnect();

    void hideCameraDisconnect();
}
