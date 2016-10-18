package com.waylens.hachi.utils;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Xiaofei on 2016/10/18.
 */

public class TouchHelper {
    public static class TouchEater implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    }
}
