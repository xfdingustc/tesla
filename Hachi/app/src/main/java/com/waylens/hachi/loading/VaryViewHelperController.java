package com.waylens.hachi.loading;

import android.support.annotation.LayoutRes;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;


/**
 * Created by Xiaofei on 2016/7/19.
 */
public class VaryViewHelperController {
    private IVaryViewHelper mHelper;

    public VaryViewHelperController(View view) {
        this(new VaryViewHelper(view));
    }

    public VaryViewHelperController(IVaryViewHelper helper) {
        this.mHelper = helper;
    }


    public void showError(String errorMsg, View.OnClickListener onClickListener) {

    }


    public void showEmpty(@LayoutRes int layoutResid,  String emptyMsg, View.OnClickListener onClickListener) {
        Logger.t("test").d("show empty");
        View layout = mHelper.inflate(layoutResid);
        mHelper.showLayout(layout);
    }


    public void showLoading(String msg) {
        View layout = mHelper.inflate(R.layout.loading);
        mHelper.showLayout(layout);
    }

    public void restore() {
        mHelper.restoreView();
    }

    public void showCameraDisconnected() {
        View layout = mHelper.inflate(R.layout.fragment_camera_disconnected);
        mHelper.showLayout(layout);
    }
}
