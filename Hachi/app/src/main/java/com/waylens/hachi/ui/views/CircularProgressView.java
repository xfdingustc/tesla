package com.waylens.hachi.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Xiaofei on 2015/5/25.
 */
public class CircularProgressView extends View {
    private static final String TAG = "CircularProgressView";
    private int mProgressStokeSize = 10;
    private Paint mProgressPaint;
    private Paint mDoneBgPaint;
    private Paint mCheckmarkPaint;
    private Paint mMaskPaint;

    public static final int STATE_NOT_STARTED = 0;
    public static final int STATE_PROGRESS_STARTED = 1;
    public static final int STATE_DONE_STARTED = 2;
    public static final int STATE_FINISHED = 3;

    private int mState = STATE_NOT_STARTED;

    private Bitmap mTempBitmap;
    private Canvas mTempCanvas;
    private RectF mProgressBounds;
    private float mCurrentProgress = 0;


    public CircularProgressView(Context context) {
        this(context, null);
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setupProgressPaint();
        setupDonePaints();

    }


    private void setupProgressPaint() {
        mProgressPaint = new Paint();
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setColor(Color.WHITE);
        mProgressPaint.setStrokeWidth(mProgressStokeSize);

    }

    private void setupDonePaints() {
        mDoneBgPaint = new Paint();
        mDoneBgPaint.setAntiAlias(true);
        mDoneBgPaint.setStyle(Paint.Style.FILL);
        mDoneBgPaint.setColor(0xff39cb72);
        mCheckmarkPaint = new Paint();

        mMaskPaint = new Paint();
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateProgressBounds();
        resetTempCanvas();
    }

    private void updateProgressBounds() {
        mProgressBounds = new RectF(mProgressStokeSize, mProgressStokeSize, getWidth() -
            mProgressStokeSize, getWidth() - mProgressStokeSize);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mState == STATE_PROGRESS_STARTED) {
            drawArForCurrentProgress();
        }

        canvas.drawBitmap(mTempBitmap, 0, 0, null);
    }


    private void changeState(int state) {
        if (this.mState == state) {
            return;
        }


        mTempBitmap.recycle();
        resetTempCanvas();

        Log.i(TAG, "state is changed from " + mState + " to " + state);
        this.mState = state;
        if (state == STATE_PROGRESS_STARTED) {

        } else if (state == STATE_DONE_STARTED) {

        }
    }

    public void finished() {
        changeState(STATE_DONE_STARTED);
    }

    private void resetTempCanvas() {
        mTempBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mTempCanvas = new Canvas(mTempBitmap);
    }

    public void setCurrentProgress(float currentProgress) {
        if (mState == STATE_NOT_STARTED && currentProgress != 0) {
            Log.i(TAG, "Change state!!!!!");
            changeState(STATE_PROGRESS_STARTED);
        }
        this.mCurrentProgress = currentProgress;
        postInvalidate();
    }


    private void drawArForCurrentProgress() {
        Log.i(TAG, "Draw arc: " + mCurrentProgress);
        mTempCanvas.drawArc(mProgressBounds, -90f, 360 * mCurrentProgress / 100, false, mProgressPaint);
    }
}
