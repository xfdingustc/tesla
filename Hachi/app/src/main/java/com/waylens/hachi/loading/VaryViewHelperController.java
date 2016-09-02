package com.waylens.hachi.loading;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;


/**
 * Created by Xiaofei on 2016/7/19.
 */
public class VaryViewHelperController {
    private final Activity mActivity;
    private IVaryViewHelper mHelper;

    public VaryViewHelperController(Activity activity, View view) {

        this(activity, new VaryViewHelper(view));
    }

    private VaryViewHelperController(Activity activity, IVaryViewHelper helper) {
        this.mActivity = activity;
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
        TextView buyWaylensEnter = (TextView)layout.findViewById(R.id.buy_waylens_camera);
        buyWaylensEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri contentUri = Uri.parse("http://www.waylens.com/");
                intent.setData(contentUri);
                mActivity.startActivity(intent);
            }
        });
        mHelper.showLayout(layout);
    }
}
