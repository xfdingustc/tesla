package com.waylens.hachi.ui.liveview.camerapreview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.waylens.hachi.utils.Utils;

abstract public class BitmapCanvas {

    abstract public void invalidate();

    abstract public void invalidateRect(int left, int top, int right, int bottom);

    public static final int DBL_CLICK_LENGTH = 400; // ms
    public static final int ZOOM_DURATION = 250; // ms
    public static final double MAX_ZOOM_FACTOR = 8.0f;

    public static final int THUMB_STATE_IDLE = 0; // no thumbnail
    public static final int THUMB_STATE_SHOW = 1; // is showing
    public static final int THUMB_STATE_KEEP = 2; // prepare to fading
    public static final int THUMB_STATE_FADING = 3; // fading

    public static final int THUMB_KEEP_TIME = 200;
    public static final int THUMB_FADING_TIME = 300;

    private final View mView;

    private int mBackgroundColor;
    private Paint mPaint = new Paint();
    private Paint mThumbnailPaint;
    private int mThumbnailScale = 3; // 1/3 width, 1/3 height

    private int mViewWidth;
    private int mViewHeight;
    private int mBitmapWidth;
    private int mBitmapHeight;

    private boolean mbNeedRecalc;
    private boolean mbUserRect;
    private int mUserAlpha;
    private Rect mUserBitmapRect = new Rect(); // user specified
    private Rect mBitmapRect = new Rect(); // bitmap
    private Rect mThumbRect = new Rect(); // thumbnail
    private Rect mDrawRect = new Rect(); // temp use
    private double mCurrZoom = 1.0f;
    private int mOffsetX;
    private int mOffsetY;

    private double mBestZoom = -1.0f;


    private int mAnchorTopThumbnail;

    private ZoomInfo mZoomInfo;

    private Scroller mScroller;

    private int mThumbState;
    private long mThumbStartTime;

    private Rect mStateRect = new Rect();



    static class ZoomInfo {
        double mStartZoom;
        double mTargetZoom;
        long mStartZoomTime;
        int mStartOffsetX;
        int mStartOffsetY;
        int mEndOffsetX;
        int mEndOffsetY;
        int mAnchorX;
        int mAnchorY;
        boolean mbTest; // test target area
    }


    public BitmapCanvas(View view) {
        mView = view;
        mBackgroundColor = Color.BLACK;

        mPaint.setFilterBitmap(true);
        mPaint.setColor(Color.BLACK);
    }




    // API
    public void drawBitmap(Canvas canvas, Bitmap bitmap, Drawable[] states) {
        if (bitmap == null) {
            if (canvas != null) {
                canvas.drawColor(mBackgroundColor);
            }
            return;
        }

        // if bitmap size changed
        if (mBitmapWidth != bitmap.getWidth() || mBitmapHeight != bitmap.getHeight()) {
            mBitmapWidth = bitmap.getWidth();
            mBitmapHeight = bitmap.getHeight();
            mbNeedRecalc = true;
        }

        // if view size changed
        if (mViewWidth != mView.getWidth() || mViewHeight != mView.getHeight()) {
            mViewWidth = mView.getWidth();
            mViewHeight = mView.getHeight();
            mbNeedRecalc = true;
        }

        if (mbNeedRecalc) {
            mbNeedRecalc = calcBitmapRect(bitmap.getWidth(), bitmap.getHeight(), mBitmapRect);
        }

        Rect rect = mbUserRect ? mUserBitmapRect : mBitmapRect;
        int colorSave = mPaint.getColor();
        if (mbUserRect) {
            mPaint.setColor((mUserAlpha << 24) | (colorSave & 0x00FFFFFF));
        }
        drawMargins(rect, canvas);
        if (mbUserRect) {
            mPaint.setColor(colorSave);
        }

        canvas.drawBitmap(bitmap, null, rect, mPaint);


        // draw thumbnail
        if (mThumbState == THUMB_STATE_SHOW) {
            drawThumbnail(canvas, bitmap, 255);
        } else if (mThumbState == THUMB_STATE_KEEP) {
            drawThumbnail(canvas, bitmap, 255);
            // KEEP -> FADING
            long currTime = SystemClock.uptimeMillis();
            int elapsed = (int) (currTime - mThumbStartTime);
            if (elapsed >= THUMB_KEEP_TIME) {
                mThumbState = THUMB_STATE_FADING;
                mThumbStartTime = currTime;
            }
        } else if (mThumbState == THUMB_STATE_FADING) {
            // FADING -> IDLE
            int elapsed = (int) (SystemClock.uptimeMillis() - mThumbStartTime);
            if (elapsed >= THUMB_FADING_TIME) {
                mThumbState = THUMB_STATE_IDLE;
            } else {
                int alpha = 255 - 255 * elapsed / THUMB_FADING_TIME;
                drawThumbnail(canvas, bitmap, alpha);
            }
        }

        if (states != null) {
            drawStates(rect, canvas, states);
        }

        // TODO - need to draw in STATE_KEEP
        if (mThumbState == THUMB_STATE_KEEP || mThumbState == THUMB_STATE_FADING) {
            int d = 2; // line width...
            invalidateRect(mThumbRect.left - d, mThumbRect.top - d, mThumbRect.right + d, mThumbRect.bottom + d);
        } else if (mZoomInfo != null || mScroller != null) {
            invalidate();
        }
    }


    // API
    public void scale(MotionEvent e, boolean bLongPress) {

        if ((mBitmapWidth | mBitmapHeight) == 0) {
            return;
        }

        int width = mView.getWidth() ;
        int height = mView.getHeight() ;
        if (width <= 0 || height <= 0) {
            return;
        }

        double bestZoom;
        if (mBestZoom > 0.0f) {
            bestZoom = mBestZoom;
        } else {
            int a = width * mBitmapHeight;
            int b = height * mBitmapWidth;
            bestZoom = a >= b ? (double) a / (double) b : (double) b / (double) a;
        }

        double targetZoom = mCurrZoom != bestZoom ? bestZoom : 1.0f;

        if (mZoomInfo == null) {
            mZoomInfo = new ZoomInfo();
        }

        mZoomInfo.mStartZoomTime = SystemClock.uptimeMillis();
        mZoomInfo.mStartOffsetX = mOffsetX;
        mZoomInfo.mStartOffsetY = mOffsetY;
        mZoomInfo.mStartZoom = mCurrZoom;
        mZoomInfo.mTargetZoom = targetZoom;
        mZoomInfo.mAnchorX = (int) ((e.getX() - mOffsetX) / mCurrZoom);
        mZoomInfo.mAnchorY = (int) ((e.getY() - mOffsetY) / mCurrZoom);

        double savedZoom = mCurrZoom;
        int savedOffsetX = mOffsetX;
        int savedOffsetY = mOffsetY;

        // TODO
        mZoomInfo.mbTest = true;
        calcBitmapRect(mBitmapWidth, mBitmapHeight, mBitmapRect);
        mZoomInfo.mbTest = false;
        mZoomInfo.mEndOffsetX = mOffsetX;
        mZoomInfo.mEndOffsetY = mOffsetY;

        mCurrZoom = savedZoom;
        mOffsetX = savedOffsetX;
        mOffsetY = savedOffsetY;
        mbNeedRecalc = true;

        mThumbState = THUMB_STATE_SHOW;
        mbNeedRecalc = true;
        invalidate();
    }

    // API
    public void scroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mOffsetX -= (int) distanceX;
        mOffsetY -= (int) distanceY;
        mbNeedRecalc = true;
        invalidate();
    }


    private void drawMargins(Rect rect, Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        if (rect.top > 0) {
            mDrawRect.set(0, 0, width, rect.top);
            canvas.drawRect(mDrawRect, mPaint);
        }
        if (rect.left > 0) {
            mDrawRect.set(0, rect.top, rect.left, rect.bottom);
            canvas.drawRect(mDrawRect, mPaint);
        }
        if (rect.right < width) {
            mDrawRect.set(rect.right, rect.top, width, rect.bottom);
            canvas.drawRect(mDrawRect, mPaint);
        }
        if (rect.bottom < height) {
            mDrawRect.set(0, rect.bottom, width, height);
            canvas.drawRect(mDrawRect, mPaint);
        }
    }

    private void drawStates(Rect rect, Canvas canvas, Drawable[] states) {
        int width = 0;
        int height = 0;
        int n = 0;

        for (int i = 0; i < states.length; i++) {
            Drawable s = states[i];
            if (s != null) {
                width += s.getIntrinsicWidth();
                int h = s.getIntrinsicHeight();
                if (height < h) {
                    height = h;
                }
                n++;
            }
        }

        if (n == 0) {
            return;
        }

        int x = rect.right - width;
        int y = rect.top;
        if (x < 0) {
            x = 0;
        } else if (x + width > mView.getWidth()) {
            x = mView.getWidth() - width;
        }

        if (y < 0) {
            y = 0;
        } else if (y + height > mView.getHeight()) {
            y = mView.getHeight() - height;
        }

        for (int i = 0; i < states.length; i++) {
            Drawable s = states[i];
            if (s != null) {
                int w = s.getIntrinsicWidth();
                mStateRect.set(x, y, x + w, y + s.getIntrinsicHeight());
                s.setBounds(mStateRect);
                s.draw(canvas);
                x += w;
            }
        }
    }

    private void drawThumbnail(Canvas canvas, Bitmap bitmap, int alpha) {
        Rect rect = mBitmapRect;
        int width = mView.getWidth();
        int height = mView.getHeight();

        calcPropRect(bitmap.getWidth(), bitmap.getHeight(), width / mThumbnailScale, height / mThumbnailScale,
            mThumbRect);

        // top,right
        mThumbRect.left += width - mThumbRect.right ;
        mThumbRect.right = width ;
        mThumbRect.bottom -= mThumbRect.top;
        mThumbRect.bottom +=  mAnchorTopThumbnail;
        mThumbRect.top =  mAnchorTopThumbnail;

        int topMargin = (int) (32 * Utils.getDensity(mView.getContext()));
        mThumbRect.offset(-32, topMargin);

        int alphaSaved = mPaint.getAlpha();
        mPaint.setAlpha(alpha);
        canvas.drawBitmap(bitmap, null, mThumbRect, mPaint);
        mPaint.setAlpha(alphaSaved);

        if (mThumbnailPaint == null) {
            mThumbnailPaint = new Paint();
            mThumbnailPaint.setColor(Color.WHITE);
            mThumbnailPaint.setStyle(Style.STROKE);
            mThumbnailPaint.setStrokeWidth(1.0f);
        }

        alphaSaved = mThumbnailPaint.getAlpha();
        mThumbnailPaint.setAlpha(alpha);
/*
        mDrawRect.set(mThumbRect);
		canvas.drawRect(mDrawRect, mThumbnailPaint);
*/
        mDrawRect.set(mThumbRect);
        double scale = mCurrZoom * mThumbnailScale;
        int left = (int) (mDrawRect.left - rect.left / scale);
        if (left < mDrawRect.left)
            left = mDrawRect.left;
        int right = (int) (mDrawRect.left + (width - rect.left) / scale + 0.5);
        if (right > mDrawRect.right)
            right = mDrawRect.right;
        int top = (int) (mDrawRect.top - rect.top / scale);
        if (top < mDrawRect.top)
            top = mDrawRect.top;
        int bottom = (int) (mDrawRect.top + (height - rect.top) / scale + 0.5);
        if (bottom > mDrawRect.bottom)
            bottom = mDrawRect.bottom;

        mDrawRect.set(left, top, right, bottom);
        canvas.drawRect(mDrawRect, mThumbnailPaint);

        mThumbnailPaint.setAlpha(alphaSaved);
    }

    static public void calcPropRect(int bmWidth, int bmHeight, int viewWidth, int viewHeight, Rect rect) {

        int marginX;
        int marginY;
        int width;
        int height;

        if (viewWidth * bmHeight >= viewHeight * bmWidth) {
            // screen is wider
            width = viewWidth;
            height = (int) ((double) viewWidth * bmHeight / bmWidth + 0.5);
            marginX = 0;
            marginY = (viewHeight - height) / 2;
        } else {
            // bitmap is wider
            width = (int) ((double) viewHeight * bmWidth / bmHeight + 0.5);
            height = viewHeight;
            marginX = (viewWidth - width) / 2;
            marginY = 0;
        }

        rect.left = marginX;
        rect.right = marginX + width;
        rect.top = marginY;
        rect.bottom = marginY + height;
    }

    private void startFading() {
        mThumbStartTime = SystemClock.uptimeMillis();
        mThumbState = THUMB_STATE_KEEP;
        invalidate();
    }


    // TODO
    private boolean calcBitmapRect(int bmWidth, int bmHeight, Rect rect) {
        int width = mView.getWidth();
        int height = mView.getHeight();
        boolean bNeedRecalc = false;

        calcPropRect(bmWidth, bmHeight, width, height, rect);

        if (mZoomInfo != null) {
            ZoomInfo zi = mZoomInfo;
            if (zi.mbTest) {
                mCurrZoom = zi.mTargetZoom;
                mOffsetX = (int) (zi.mAnchorX * (zi.mStartZoom - mCurrZoom)) + zi.mStartOffsetX;
                mOffsetY = (int) (zi.mAnchorY * (zi.mStartZoom - mCurrZoom)) + zi.mStartOffsetY;
            } else {
                int elapsed = (int) (SystemClock.uptimeMillis() - zi.mStartZoomTime);
                if (elapsed >= ZOOM_DURATION) {
                    mCurrZoom = zi.mTargetZoom;
                    mOffsetX = zi.mEndOffsetX;
                    mOffsetY = zi.mEndOffsetY;
                    mZoomInfo = null;
                    startFading();
                } else {
                    mCurrZoom = zi.mStartZoom + elapsed * (zi.mTargetZoom - zi.mStartZoom) / ZOOM_DURATION;
                    mOffsetX = zi.mStartOffsetX + elapsed * (zi.mEndOffsetX - zi.mStartOffsetX) / ZOOM_DURATION;
                    mOffsetY = zi.mStartOffsetY + elapsed * (zi.mEndOffsetY - zi.mStartOffsetY) / ZOOM_DURATION;
                    bNeedRecalc = true;
                }
            }
        }

        if (mScroller != null) {
            if (mScroller.computeScrollOffset()) {
                mOffsetX = mScroller.getCurrX();
                mOffsetY = mScroller.getCurrY();
                bNeedRecalc = true;
            } else {
                mScroller = null;
                startFading();
            }
        }

        rect.left = (int) (rect.left * mCurrZoom) + mOffsetX;
        rect.right = (int) (rect.right * mCurrZoom) + mOffsetX;
        rect.top = (int) (rect.top * mCurrZoom) + mOffsetY;
        rect.bottom = (int) (rect.bottom * mCurrZoom) + mOffsetY;

        // adjust x position
        int tmp = rect.width();
        int dx = 0;
        int lenx = width ;
        if (tmp <= lenx) {
            // center in (view - margin)
            dx =(lenx - tmp) / 2 - rect.left;
        } else if (rect.left > 0) {
            dx = 0 - rect.left;
        } else if (rect.right + 0 < width) {
            dx = width - (rect.right );
        }
        rect.left += dx;
        rect.right += dx;
        mOffsetX += dx;

        // adjust y position
        tmp = rect.height();
        int dy = 0;
        int leny = height;
        if (tmp <= leny) {
            dy = (leny - tmp) / 2 - rect.top;
        } else if (rect.top > 0) {
            dy = 0 - rect.top;
        } else if (rect.bottom + 0 < height) {
            dy = height - rect.bottom ;
        }
        rect.top += dy;
        rect.bottom += dy;
        mOffsetY += dy;

        return bNeedRecalc;
    }

}
