package com.waylens.hachi.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.mapbox.mapboxsdk.views.MapView;

/**
 * FrameLayout - Drag container
 * Created by Richard on 10/30/15.
 */
public class DragLayout extends FrameLayout {

    private final ViewDragHelper mDragHelper;

    private View mDragView;

    OnViewDragListener mDragListener;

    public DragLayout(Context context) {
        this(context, null, 0);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelperCallback());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelperCallback());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
            return false;
        }
        View toCapture = mDragHelper.findTopChildUnder((int) ev.getX(), (int)ev.getY());
        return toCapture instanceof MapView || mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDragHelper.getCapturedView() == null
                && event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
        }
        mDragHelper.processTouchEvent(event);
        return true;
    }

    public void setOnViewDragListener(OnViewDragListener listener) {
        mDragListener = listener;
    }

    class ViewDragHelperCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            mDragView = child;
            return child instanceof GaugeView || child instanceof MapView;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final int leftBound = getPaddingLeft();
            final int rightBound = getWidth() - mDragView.getWidth();
            return Math.min(Math.max(left, leftBound), rightBound);
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int topBound = getPaddingTop();
            final int bottomBound = getHeight() - mDragView.getHeight();
            return Math.min(Math.max(top, topBound), bottomBound);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            Log.e("test", "Position l:" + releasedChild.getLeft() + "; t: " + releasedChild.getTop());
            mDragView.setX(releasedChild.getX());
            mDragView.setY(releasedChild.getY());
            super.onViewReleased(releasedChild, xvel, yvel);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (mDragListener == null) {
                return;
            }
            if (state == ViewDragHelper.STATE_DRAGGING) {
                mDragListener.onStartDragging();
            } else if (state == ViewDragHelper.STATE_IDLE) {
                mDragListener.onStopDragging();
            }
        }
    }

    public interface OnViewDragListener {
        void onStartDragging();

        void onStopDragging();
    }

}
