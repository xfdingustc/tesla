package com.waylens.hachi.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.orhanobut.logger.Logger;

/**
 * Created by Xiaofei on 2015/9/9.
 */
public class ContainerView extends ViewGroup {
    private static final String TAG = ContainerView.class.getSimpleName();
    public ContainerView(Context context) {
        super(context);
    }

    public ContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);


        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            PanelView.LayoutParams params = (PanelView.LayoutParams) child.getLayoutParams();
            int alignment = params.mAlignment;
            switch (alignment) {
                case PanelView.LayoutParams.BOTTOM_LEFT:
                    params.mLeft = 0;
                    params.mRight = child.getMeasuredWidth();
                    params.mBottom = getMeasuredHeight();
                    params.mTop = getMeasuredHeight() - child.getMeasuredHeight();
                    break;
            }
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();


        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            PanelView.LayoutParams params = (PanelView.LayoutParams) child.getLayoutParams();
            //Logger.t(TAG).d("l: " + params.mLeft + " r: " + params.mRight + " t: " + params.mTop
            //    + " b: " + params.mBottom);
            child.layout(params.mLeft, params.mTop, params.mRight, params.mBottom);
        }
    }

    public static class LayoutParams extends ViewGroup.LayoutParams implements ContainerLayouts {

        public int mAlignment;
        private int mLeft, mTop, mRight, mBottom;

        public LayoutParams(final int width, final int height, final int alignment) {
            super(width, height);
            this.mAlignment = alignment;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
