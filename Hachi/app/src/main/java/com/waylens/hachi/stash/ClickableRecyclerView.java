package com.waylens.hachi.stash;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Richard on 11/30/15.
 */
public class ClickableRecyclerView extends RecyclerView {

    public ClickableRecyclerView(Context context) {
        super(context);
    }

    public ClickableRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClickableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        final int action = MotionEventCompat.getActionMasked(e);
        if (action == MotionEvent.ACTION_UP) {
            performClick();
        }
        return true;
    }
}
