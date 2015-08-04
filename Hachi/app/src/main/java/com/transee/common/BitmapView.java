package com.transee.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class BitmapView extends View {

    public interface Callback {
        public void onDown();

        public void onSingleTapUp();

        public void onDoubleClick();
    }

    private Bitmap mBitmap; // can be null
    private BitmapCanvas mBitmapCanvas;
    private GestureDetector mGesture;
    private Callback mCallback;

    public BitmapView(Context context) {
        super(context);
        initView();
    }

    public BitmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public BitmapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        mBitmapCanvas = new BitmapCanvas(this) {
            @Override
            public void invalidate() {
                postInvalidate();
            }

            @Override
            public void invalidateRect(int left, int top, int right, int bottom) {
                postInvalidate(left, top, right, bottom);
            }
        };
        mGesture = new GestureDetector(getContext(), new MyGestureListener());
    }

    // API
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void setBackgroundColor(int color) {
        mBitmapCanvas.setBackgroundColor(color);
    }

    // API
    public void setBitmap(Bitmap bitmap) {
        /*
		Don't call Bitmap.recycle() here, because other views
		might hold this Bitmap.
		if (mBitmap != null) {
			mBitmap.recycle();
		}*/

        mBitmap = bitmap;
        if (bitmap == null) {
            // TODO
            mBitmapCanvas.cancelAnimation();
        }
        invalidate();
    }

    // API
    public final void setAnchorRect(int leftMargin, int topMargin, int rightMargin, int bottomMargin, float bestZoom) {
        mBitmapCanvas.setAnchorRect(leftMargin, topMargin, rightMargin, bottomMargin, bestZoom);
    }

    // API
    public final Bitmap getBitmap() {
        return mBitmap;
    }

    // API
    public final void getBitmapRect(int bmWidth, int bmHeight, Rect rect) {
        mBitmapCanvas.getBitmapRect(bmWidth, bmHeight, rect);
    }

    // API
    public final Rect getBitmapRect() {
        return mBitmapCanvas.getBitmapRect();
    }

    // API
    public final void setUserRect(boolean bUserRect, Rect rect, int alpha) {
        mBitmapCanvas.setUserRect(bUserRect, rect, alpha);
    }

    // API
    public final void setThumbnailScale(int thumbnailScale) {
        mBitmapCanvas.setThumbnailScale(thumbnailScale);
    }

    // API
    public final void getIdealSize(Rect rect) {
        int width = getWidth();
        int height = getHeight();
        if (mBitmap == null) {
            // asume the bitmap is 16:9
            BitmapCanvas.calcPropRect(width, width * 9 / 16, width, height, rect);
        } else {
            BitmapCanvas.calcPropRect(mBitmap.getWidth(), mBitmap.getHeight(), width, height, rect);
        }
        rect.offset(-rect.left, -rect.top);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mBitmapCanvas.updateBitmapRect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mBitmapCanvas.drawBitmap(canvas, mBitmap, false, null);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Log.d(TAG, "dispatchTouchEvent");
        boolean handled = super.dispatchTouchEvent(ev);
        handled |= mBitmapCanvas.dispatchTouchEvent(ev, handled, mGesture);
        return handled;
    }

    private boolean onDown(MotionEvent e) {
        mBitmapCanvas.onDown(e);
        if (mCallback != null) {
            mCallback.onDown();
        }
        return true;
    }

    private boolean onSingleTapUp(MotionEvent e) {
        if (mBitmapCanvas.isDoubleClick(e)) {
            mBitmapCanvas.scale(e, false);
            if (mCallback != null) {
                mCallback.onDoubleClick();
            }
        } else {
            if (mCallback != null) {
                mCallback.onSingleTapUp();
            }
        }
        return true;
    }

    private boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mBitmapCanvas.scroll(e1, e2, distanceX, distanceY);
        return true;
    }

    private boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        mBitmapCanvas.fling(e1, e2, velocityX, velocityY);
        return true;
    }

    private class MyGestureListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return BitmapView.this.onDown(e);
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // Log.d(TAG, "onShowPress");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return BitmapView.this.onSingleTapUp(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return BitmapView.this.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return BitmapView.this.onFling(e1, e2, velocityX, velocityY);
        }

    }

}
