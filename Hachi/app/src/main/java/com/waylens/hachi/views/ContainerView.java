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


        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            int alignment = params.mAlignment;
            switch (alignment) {
                case LayoutParams.BOTTOM_LEFT:
                    params.mLeft = 0;
                    params.mRight = child.getMeasuredWidth();
                    params.mBottom = height;
                    params.mTop = height - child.getMeasuredHeight();
                    break;
                case LayoutParams.CENTER:
                    params.mLeft = (width - child.getMeasuredWidth()) / 2;
                    params.mTop = (height - child.getMeasuredHeight()) / 2 - params.mBottomMargin;
                    params.mRight = params.mLeft + child.getMeasuredWidth();
                    params.mBottom = params.mTop + child.getMeasuredHeight();
                    break;
                case LayoutParams.TOP_LEFT:
                    params.mLeft = 0;
                    params.mTop = 0;
                    params.mRight = params.mLeft + child.getMeasuredWidth();
                    params.mBottom = params.mTop + child.getMeasuredHeight();
                    break;
                case LayoutParams.TOP_CENTER:
                    params.mLeft = (width - child.getMeasuredWidth()) / 2;
                    params.mTop = params.mTopMargin;
                    params.mRight = params.mLeft + child.getMeasuredWidth();
                    params.mBottom = params.mTop + child.getMeasuredHeight();
                    break;
                case LayoutParams.BOTTOM_CENTER:
                    params.mLeft = (width - child.getMeasuredWidth()) / 2;
                    params.mRight = params.mLeft + child.getMeasuredWidth();
                    params.mBottom = height - params.mBottomMargin;
                    params.mTop = params.mBottom - child.getMeasuredHeight();
                    break;
                case LayoutParams.TOP_RIGHT:
                    params.mRight = width - params.mRightMargin;
                    params.mLeft = params.mRight - child.getMeasuredWidth();
                    params.mTop = params.mTopMargin;
                    params.mBottom = params.mTop + child.getMeasuredHeight();
                    break;
                case LayoutParams.CENTER_RIGHT:
                    params.mRight = width - params.mRightMargin;
                    params.mLeft = params.mRight - child.getMeasuredWidth();
                    params.mTop = (height - child.getMeasuredHeight()) / 2 + params.mTopMargin;
                    params.mBottom = params.mTop + child.getMeasuredHeight();
                    break;
                case LayoutParams.CENTER_LEFT:
                    params.mLeft = params.mLeftMargin;
                    params.mRight = params.mLeft + child.getMeasuredWidth();
                    params.mTop = (height - child.getMeasuredHeight()) / 2 + params.mTopMargin;
                    params.mBottom = params.mTop + child.getMeasuredHeight();
                    break;
                case LayoutParams.BOTTOM_RIGHT:
                    params.mRight = width - params.mRightMargin;
                    params.mLeft = params.mRight - child.getMeasuredWidth();
                    params.mBottom = height - params.mBottomMargin;
                    params.mTop = params.mBottom - child.getMeasuredHeight();
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
        private int mTopMargin, mBottomMargin, mLeftMargin, mRightMargin;

        public LayoutParams(final int topMargin, final int bottomMargin, final int leftMargin,
                            final int rightMargin, final int alignment) {
            super(0, 9);
            this.mAlignment = alignment;
            this.mTopMargin = topMargin;
            this.mBottomMargin = bottomMargin;
            this.mLeftMargin = leftMargin;
            this.mRightMargin = rightMargin;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
