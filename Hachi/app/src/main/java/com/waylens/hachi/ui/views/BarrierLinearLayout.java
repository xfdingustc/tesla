package com.waylens.hachi.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Created by Richard on 1/6/16.
 */
public class BarrierLinearLayout extends LinearLayout {

    private boolean mBarrierEnabled;

    public BarrierLinearLayout(Context context) {
        this(context, null, 0);
    }

    public BarrierLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarrierLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBarrierEnabled = true;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BarrierLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mBarrierEnabled = true;
    }

    public void enableBarrier(boolean barrierEnabled) {
        mBarrierEnabled = barrierEnabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mBarrierEnabled || super.onTouchEvent(event);
    }
}
