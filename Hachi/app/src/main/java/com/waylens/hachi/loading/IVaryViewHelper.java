package com.waylens.hachi.loading;

import android.content.Context;
import android.view.View;

/**
 * Created by Xiaofei on 2016/7/19.
 */
public interface IVaryViewHelper {
    View getCurrentLayout();

    void restoreView();

    void showLayout(View view);

    View inflate(int layoutId);

    Context getContext();

    View getView();
}
