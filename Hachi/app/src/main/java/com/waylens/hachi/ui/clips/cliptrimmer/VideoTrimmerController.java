package com.waylens.hachi.ui.clips.cliptrimmer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.utils.ViewUtils;


class VideoTrimmerController extends View {
    private static final String TAG = VideoTrimmerController.class.getSimpleName();
    private static final float MAX_VALUE = 1000.0f;

    int mThumbWidth;
    private int mBorderWidth;
    private int mProgressBarWidth = 24;

    private Rect mRectLeft;
    private Rect mRectRight;
    private Rect mRectBorder;
    private Rect mRectLeftEx;
    private Rect mRectRightEx;
    private RectF mRectProgress;

    private Paint mPaint;
    private Paint mPaintEx;
//    private Paint mPaintProgress;
    private Bitmap mBitmapLeftArrow;
    private Bitmap mBitmapRightArrow;
    private int mColor;
    private int mLeftStartPos;
    private int mRightStartPos;

    private boolean isDraggingLeft;
    private boolean isDraggingRight;
    private boolean isDraggingThumb;
    private boolean isDraggingProgressBar;

    private int mTouchDistLeft;
    private int mTouchDistRight;
    private int mTouchDistProgress;

    private int mProgressLeft;

    private int mTotalViewLength;

    VideoTrimmer.OnTrimmerChangeListener mChangeListener;
    VideoTrimmer mVideoTrimmer;

    private double mRangeValueLeft = 0;
    private double mRangeValueRight = MAX_VALUE;


//    private boolean showProgress = true;

    long mStart;
    long mEnd;
    long mProgress;

    public VideoTrimmerController(Context context, int thumbWidth, int boardWidth, int progressBarWidth) {
        super(context);
        mThumbWidth = thumbWidth;
        mBorderWidth = boardWidth;
        mProgressBarWidth = progressBarWidth;
        init();
    }

    public VideoTrimmerController(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VideoTrimmerController(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoTrimmerController(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        if (mThumbWidth == 0) {
            mThumbWidth = ViewUtils.dp2px(VideoTrimmer.DEFAULT_THUMB_WIDTH_DP);
            mBorderWidth = ViewUtils.dp2px(VideoTrimmer.DEFAULT_BORDER_WIDTH_DP);
            mProgressBarWidth = ViewUtils.dp2px(VideoTrimmer.DEFAULT_PROGRESS_BAR_WIDTH_DP);
        }

        mColor = Color.rgb(0x7a, 0xd5, 0x02);
//        mColor = Color.rgb(0xff, 0x57, 0x22);
        mPaint = new Paint();
        mPaint.setColor(mColor);
        mRectLeft = new Rect();
        mRectRight = new Rect();
        mRectLeftEx = new Rect();
        mRectBorder = new Rect();
        mRectRightEx = new Rect();
        mRectProgress = new RectF();

        mBitmapLeftArrow = BitmapFactory.decodeResource(getResources(), R.drawable.video_handle_left);
        mBitmapRightArrow = BitmapFactory.decodeResource(getResources(), R.drawable.video_handle_right);
        mPaintEx = new Paint();
        mPaintEx.setColor(Color.argb(0x80, 0xFF, 0xFF, 0xFF));
//        mPaintProgress = new Paint();
//        mPaintProgress.setColor(Color.WHITE);
        mProgressLeft = mLeftStartPos + mThumbWidth;
    }

    void setOnChangeListener(VideoTrimmer.OnTrimmerChangeListener listener, VideoTrimmer trimmer) {
        mChangeListener = listener;
        mVideoTrimmer = trimmer;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mTotalViewLength = w - mThumbWidth * 2 - mProgressBarWidth;
        positionLeft();
        positionRight();
        positionProgress();
        //mRightStartPos = w - mThumbWidth;
        //mRectLeft.set(mLeftStartPos, 0, mLeftStartPos + mThumbWidth, h);
        //mRectLeftEx.set(0, 0, mLeftStartPos, h);
        //mRectRight.set(mRightStartPos, 0, mRightStartPos + mThumbWidth, h);
        //mRectRightEx.set(mRectRight.right, 0, w, h);
        //mRectProgress.set(mProgressLeft, 0, mProgressLeft + mProgressBarWidth, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawThumb(canvas);
    }

    void drawThumb(Canvas canvas) {
        int height = getHeight();
        canvas.drawRect(mRectLeftEx, mPaintEx);

        canvas.drawBitmap(mBitmapLeftArrow, null, mRectLeft, null);

        canvas.drawBitmap(mBitmapRightArrow, null, mRectRight, null);
        canvas.drawRect(mRectRightEx, mPaintEx);

        mRectBorder.set(mRectLeft.right, 0, mRectRight.left, mBorderWidth);

        canvas.drawRect(mRectBorder, mPaint);

        mRectBorder.set(mRectLeft.right, height - mBorderWidth, mRectRight.left, height);
        canvas.drawRect(mRectBorder, mPaint);

//        if (showProgress && !isDraggingThumb) {
//            canvas.drawRoundRect(mRectProgress, 8, 8, mPaintProgress);
//        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDraggingThumb = evalPressedThumb(event.getX());
                isDraggingProgressBar = isDraggingProgress(event.getX());
                if (isDraggingThumb) {
                    setPressed(true);
                    invalidate();
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                } else if (isDraggingProgressBar) {
                    mTouchDistProgress = mProgressBarWidth / 2; //(int) event.getX() - mProgressLeft;
                    mProgressLeft = (int) event.getX() - mTouchDistProgress;
                    setPressed(true);
                    invalidate();
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                } else {
                    super.onTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                trackTouchEvent(event);
                break;
            case MotionEvent.ACTION_UP:
                if (isDraggingThumb || isDraggingProgressBar) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        return true;
    }

    void onStartTrackingTouch() {
        if (mChangeListener != null) {
            VideoTrimmer.DraggingFlag flag = getFlag();
            mChangeListener.onStartTrackingTouch(mVideoTrimmer, flag);
        }
    }

    VideoTrimmer.DraggingFlag getFlag() {
        VideoTrimmer.DraggingFlag flag;
        if (isDraggingLeft) {
            flag = VideoTrimmer.DraggingFlag.LEFT;
        } else if (isDraggingRight) {
            flag = VideoTrimmer.DraggingFlag.RIGHT;
        } else if (isDraggingProgressBar) {
            flag = VideoTrimmer.DraggingFlag.PROGRESS;
        } else {
            flag = VideoTrimmer.DraggingFlag.UNKNOWN;
        }
        return flag;
    }

    void onStopTrackingTouch() {
        isDraggingLeft = false;
        isDraggingRight = false;
        isDraggingThumb = false;
        mTouchDistLeft = 0;
        mTouchDistRight = 0;
        invalidate();
        if (mChangeListener != null) {
            mChangeListener.onStopTrackingTouch(mVideoTrimmer);
        }
    }

    void setProgress(long value) {
        if (value < mRangeValueLeft || value > mRangeValueRight) {
            return;
        }
        mProgress = value;

        positionProgress();

        invalidate();
    }

    private void positionProgress() {
        double normalizedValue = mProgress - mRangeValueLeft;
        mProgressLeft = (int) (normalizedValue / getRange() * mTotalViewLength) + mThumbWidth;

        if (mProgressLeft > (mRectRight.left - mProgressBarWidth)) {
            mProgressLeft = mRectRight.left - mProgressBarWidth;
        }

        if (mProgressLeft < mRectLeft.right) {
            mProgressLeft = mRectLeft.right;
        }

        mRectProgress.set(mProgressLeft, 0, mProgressLeft + mProgressBarWidth, getHeight());
    }

    void setInitRangeValues(long min, long max) {
        if (min < 0 || max < 0 || min >= max) {
            return;
        }
        mRangeValueLeft = min;
        mRangeValueRight = max;
        mStart = min;
        mEnd = max;
        mProgress = min;
    }

    double getMinValue() {
        return mRangeValueLeft;
    }

    double getMaxValue() {
        return mRangeValueRight;
    }

    double getRange() {
        return mRangeValueRight - mRangeValueLeft;
    }

    void setLeftValue(long value) {
        if (value < mRangeValueLeft || value > mRangeValueRight) {
            return;
        }
        mStart = value;

        positionLeft();

        if (mProgress < mStart) {
            setProgress(mStart);
        } else {
            invalidate();
        }
    }

    private void positionLeft() {
        double normalizedValue = mStart - mRangeValueLeft;
        mLeftStartPos = (int) Math.round(normalizedValue / getRange() * mTotalViewLength);
        int height = getHeight();
        mRectLeft.set(mLeftStartPos, 0, mLeftStartPos + mThumbWidth, height);
        mRectLeftEx.set(0, 0, mLeftStartPos, height);
    }

    void setRightValue(long value) {
        if (value < mRangeValueLeft || value > mRangeValueRight) {
            return;
        }
        mEnd = value;

        positionRight();

        if (mProgress > mEnd) {
            setProgress(mEnd);
        } else {
            invalidate();
        }
    }

    private void positionRight() {
        double normalizedValue = mEnd - mRangeValueLeft;
        mRightStartPos = (int) Math.round((normalizedValue / getRange() * mTotalViewLength) + mProgressBarWidth + mThumbWidth);
        int height = getHeight();
        mRectRight.set(mRightStartPos, 0, mRightStartPos + mThumbWidth, height);
        mRectRightEx.set(mRectRight.right, 0, getWidth(), height);
    }

    void trackTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int height = getHeight();

        if (isDraggingLeft
            && x >= mTouchDistLeft
            && x < (mRightStartPos - mProgressBarWidth - mThumbWidth + mTouchDistLeft)) {
            mLeftStartPos = x - mTouchDistLeft;
            if (mProgressLeft < (mLeftStartPos + mThumbWidth)) {
                mProgressLeft = mLeftStartPos + mThumbWidth;
                mRectProgress.set(mProgressLeft, 0, mProgressLeft + mProgressBarWidth, height);
            }
            mRectLeft.set(mLeftStartPos, 0, mLeftStartPos + mThumbWidth, height);
            mRectLeftEx.set(0, 0, mLeftStartPos, height);
            invalidate();
        }
        if (isDraggingRight
            && x <= (getWidth() - mThumbWidth + mTouchDistRight)
            && x > (mLeftStartPos + mThumbWidth + mProgressBarWidth + mTouchDistRight)) {
            mRightStartPos = x - mTouchDistRight;
            if (mProgressLeft > (mRightStartPos - mProgressBarWidth)) {
                mProgressLeft = mRightStartPos - mProgressBarWidth;
                mRectProgress.set(mProgressLeft, 0, mProgressLeft + mProgressBarWidth, height);
            }
            mRectRight.set(mRightStartPos, 0, mRightStartPos + mThumbWidth, height);
            mRectRightEx.set(mRectRight.right, 0, getWidth(), height);
            invalidate();
        }

        if (isDraggingProgressBar
            && x >= (mLeftStartPos + mThumbWidth + mTouchDistProgress)
            && x <= (mRightStartPos - mProgressBarWidth + mTouchDistProgress)) {
            mProgressLeft = x - mTouchDistProgress;
            mRectProgress.set(mProgressLeft, 0, mProgressLeft + mProgressBarWidth, height);
            invalidate();
        }
        calculateValues();
    }

    private void calculateValues() {
        if (mTotalViewLength == 0) {
            return;
        }
        double scale = getRange() / mTotalViewLength;
        mStart = (long) ((mRectLeft.right - mThumbWidth) * scale + mRangeValueLeft);
        mEnd = (long) ((mRectRight.left - mProgressBarWidth - mThumbWidth) * scale + mRangeValueLeft);
        mProgress = (long) ((mRectProgress.left - mThumbWidth) * scale + mRangeValueLeft);

        if (mStart < mRangeValueLeft) {
            mStart = (long) mRangeValueLeft;
        }
        if (mEnd > mRangeValueRight) {
            mEnd = (long) mRangeValueRight;
        }

        if (mProgress < mRangeValueLeft) {
            mProgress = (long) mRangeValueLeft;
        }
        if (mProgress > mRangeValueRight) {
            mProgress = (long) mRangeValueRight;
        }

        if (mChangeListener != null) {
            mChangeListener.onProgressChanged(mVideoTrimmer, getFlag(), mStart, mEnd, mProgress);
        }
    }

    private boolean evalPressedThumb(float x) {
        isDraggingLeft = isInThumbRange(mRectLeft, x);
        if (isDraggingLeft) {
            mTouchDistLeft = (int) x - mRectLeft.left;
        }
        isDraggingRight = isInThumbRange(mRectRight, x);
        if (isDraggingRight) {
            mTouchDistRight = (int) x - mRectRight.left;
        }
        return isDraggingLeft || isDraggingRight;
    }

    boolean isDraggingProgress(float x) {
        return x > mRectLeft.right && x < mRectRight.left;
    }

    boolean isInThumbRange(Rect rect, float x) {
        return x > rect.left && x < rect.right;
    }


//    @Override
//    public int updateProgress() {
//        int timeOffset;
//        try {
//            timeOffset = mPlayer.getCurrentPosition();
//        } catch (IllegalStateException e) {
//            Log.e("test", "", e);
//            return 0;
//        }
//        setProgress(timeOffset + mStart);
//        return 0;
//    }
//
//    @Override
//    public boolean isInProgress() {
//        try {
//            return !isDraggingThumb && !isDraggingProgressBar && mPlayer != null && mPlayer.isPlaying();
//        } catch (Exception e) {
//            Log.e("test", "", e);
//            return false;
//        }
//    }
}
