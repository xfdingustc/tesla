package com.waylens.hachi.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.R;

/**
 * GForce View
 * Created by liangyx on 7/15/15.
 */
public class GForceView extends View {

    private static final int SIZE = 328;
    private static final int BAR_SPACE = 4;
    private static final int ARROW_SPACE = 8;
    private static final int UP_TOP_REF = 58;
    private static final int LEFT_LEFT_REF = 70;
    private static final int TOTAL_BARS = 16;

    private static final int LABEL_LEFT_TOP = 36;

    private Bitmap mBackground;
    private Bitmap mOuterCircle;
    private Bitmap mInnerCircle;
    private Bitmap mCentralDot;
    private Bitmap[] mLabelUp;
    private Bitmap[] mLabelDown;
    private Bitmap[] mLabelLeft;
    private Bitmap[] mLabelRight;
    private Bitmap[] mBitmapDigits100;
    private Bitmap[] mBitmapDigits70;

    private Bitmap[] mGForceBars;

    private Paint mBackgroundPaint;

    private MoveData mUpMoveData = new MoveData();
    private MoveData mDownMoveData = new MoveData();
    private MoveData mLeftMoveData = new MoveData();
    private MoveData mRightMoveData = new MoveData();

    public GForceView(Context context) {
        this(context, null, 0);
    }

    public GForceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GForceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public GForceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        Resources resources = getResources();

        TypedArray resIds = resources.obtainTypedArray(R.array.gforce_bars);
        mGForceBars = new Bitmap[resIds.length()];
        for (int i = 0; i < mGForceBars.length; i++) {
            mGForceBars[i] = BitmapFactory.decodeResource(resources, resIds.getResourceId(i, -1));
        }
        resIds.recycle();

        resIds = resources.obtainTypedArray(R.array.numbers_16px_opa100);
        mBitmapDigits100 = new Bitmap[resIds.length()];
        for (int i = 0; i < mBitmapDigits100.length; i++) {
            mBitmapDigits100[i] = BitmapFactory.decodeResource(resources, resIds.getResourceId(i, -1));
        }
        resIds.recycle();

        resIds = resources.obtainTypedArray(R.array.numbers_16px_opa70);
        mBitmapDigits70 = new Bitmap[resIds.length()];
        for (int i = 0; i < mBitmapDigits70.length; i++) {
            mBitmapDigits70[i] = BitmapFactory.decodeResource(resources, resIds.getResourceId(i, -1));
        }
        resIds.recycle();


        mOuterCircle = BitmapFactory.decodeResource(resources, R.drawable.gforce_outer_background_circle);
        mInnerCircle = BitmapFactory.decodeResource(resources, R.drawable.gforce_inner_background_circle);
        mCentralDot = BitmapFactory.decodeResource(resources, R.drawable.gforce_central_dot);

        mLabelUp = new Bitmap[2];
        mLabelUp[0] = BitmapFactory.decodeResource(resources, R.drawable.gforce_label_brake_opa100);
        mLabelUp[1] = BitmapFactory.decodeResource(resources, R.drawable.gforce_label_brake_opa50);

        mLabelDown = new Bitmap[2];
        mLabelDown[0] = BitmapFactory.decodeResource(resources, R.drawable.gforce_label_accel_opa100);
        mLabelDown[1] = BitmapFactory.decodeResource(resources, R.drawable.gforce_label_accel_opa50);

        mLabelLeft = new Bitmap[2];
        mLabelLeft[0] = BitmapFactory.decodeResource(resources, R.drawable.gforce_label_left_opa100);
        mLabelLeft[1] = BitmapFactory.decodeResource(resources, R.drawable.gforce_label_left_opa50);

        mLabelRight = new Bitmap[2];
        mLabelRight[0] = BitmapFactory.decodeResource(resources, R.drawable.gforce_label_right_opa100);
        mLabelRight[1] = BitmapFactory.decodeResource(resources, R.drawable.gforce_label_right_opa50);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setFilterBitmap(true);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int bgHeight = mOuterCircle.getHeight();
        int bgWidth = mOuterCircle.getWidth();

        if (widthSize >= bgWidth) { //use image size
            widthSize = bgWidth;
            if (heightSize > bgHeight) {
                heightSize = bgHeight;
            } else {
                widthSize = heightSize * bgWidth / bgHeight;
            }
        } else {
            int height = widthSize * bgHeight / bgWidth;
            if (height <= heightSize) {
                heightSize = height;
            } else {
                widthSize = heightSize * bgWidth / bgHeight;
            }
        }

        final int chosenWidth = chooseDimension(widthMode, widthSize);
        final int chosenHeight = chooseDimension(heightMode, heightSize);
        setMeasuredDimension(chosenWidth, chosenHeight);
    }

    private int chooseDimension(final int mode, final int size) {
        switch (mode) {
            case View.MeasureSpec.AT_MOST:
            case View.MeasureSpec.EXACTLY:
                return size;
            case View.MeasureSpec.UNSPECIFIED:
            default:
                return getDefaultDimension();
        }
    }

    private int getDefaultDimension() {
        return SIZE;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        drawGForce();
    }

    private void drawGForce() {
        if (null != mBackground) {
            mBackground.recycle();
        }
        mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mBackground);
        //outer circle
        canvas.drawBitmap(mOuterCircle, null, new Rect(0, 0, getWidth(), getHeight()), null);

        //inner circle
        float scale = mOuterCircle.getHeight() / SIZE;
        float space = (mOuterCircle.getHeight() - mInnerCircle.getHeight()) / 2;
        canvas.drawBitmap(mInnerCircle, null,
                new RectF(space, space, (mOuterCircle.getWidth() - space) * scale, (mOuterCircle.getHeight() - space) * scale), null);

        //central dot
        float left = (mOuterCircle.getWidth() - mCentralDot.getWidth()) / 2 * scale;
        float top = (mOuterCircle.getHeight() - mCentralDot.getHeight()) / 2 * scale;
        canvas.drawBitmap(mCentralDot, null, new RectF(left, top, left + mCentralDot.getWidth() * scale, top + mCentralDot.getHeight() * scale), null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (null != mBackground) {
            canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);
        }
        drawBars(canvas);
        computeCurrentValue(mUpMoveData);
        computeCurrentValue(mDownMoveData);
        computeCurrentValue(mLeftMoveData);
        computeCurrentValue(mRightMoveData);
    }

    private int valueToIndex(float value) {
        int index = 0;
        if (value <= 0) {
            index = 0;
        } else {
            index = (int) (value * TOTAL_BARS) + 1;
            if (index > (TOTAL_BARS + 1)) {
                index = TOTAL_BARS + 1;
            }
        }
        return index;
    }

    private void drawBars(Canvas canvas) {
        float scale = mOuterCircle.getHeight() / SIZE;

        //right
        float left = ((mOuterCircle.getWidth() + mCentralDot.getWidth()) / 2 + BAR_SPACE) * scale;
        float top = (mOuterCircle.getHeight() - mGForceBars[0].getHeight()) * scale / 2;
        RectF rectF = new RectF(left, top, left + mGForceBars[0].getWidth() * scale, top + mGForceBars[0].getHeight());
        int index = valueToIndex(mRightMoveData.currentValue);
        canvas.drawBitmap(mGForceBars[index], null, rectF, null);
        canvas.save(Canvas.MATRIX_SAVE_FLAG);

        //down
        index = valueToIndex(mDownMoveData.currentValue);
        //Log.e("test", "Index: " + index);
        canvas.rotate(90, getWidth() / 2, getHeight() / 2);
        canvas.drawBitmap(mGForceBars[index], null, rectF, null);

        //left
        index = valueToIndex(mLeftMoveData.currentValue);
        canvas.rotate(90, getWidth() / 2, getHeight() / 2);
        canvas.drawBitmap(mGForceBars[index], null, rectF, null);

        //up
        index = valueToIndex(mUpMoveData.currentValue);
        canvas.rotate(90, getWidth() / 2, getHeight() / 2);
        canvas.drawBitmap(mGForceBars[index], null, rectF, null);
        canvas.restore();

        left = (mOuterCircle.getWidth() - mLabelUp[0].getWidth()) / 2 * scale;
        top = LABEL_LEFT_TOP * scale;
        float right = left + mLabelUp[0].getWidth() * scale;
        float bottom = top + mLabelUp[0].getHeight() * scale;
        rectF.set(left, top, right, bottom);
        canvas.drawBitmap(mLabelUp[0], null, rectF, null);

        left = (mOuterCircle.getWidth() - mLabelDown[0].getWidth()) / 2 * scale;
        bottom = (mOuterCircle.getHeight() - LABEL_LEFT_TOP) * scale;
        top = bottom - mLabelDown[0].getHeight() * scale;
        right = left + mLabelDown[0].getWidth() * scale;
        rectF.set(left, top, right, bottom);
        canvas.drawBitmap(mLabelDown[0], null, rectF, null);

        left = (LEFT_LEFT_REF - mLabelLeft[0].getWidth()) * scale;
        right = LEFT_LEFT_REF;
        bottom = (mOuterCircle.getHeight() - ARROW_SPACE) / 2 * scale;
        top = bottom - mLabelLeft[0].getHeight() * scale;
        rectF.set(left, top, right, bottom);
        canvas.drawBitmap(mLabelLeft[0], null, rectF, null);

        left = (mOuterCircle.getWidth() - LEFT_LEFT_REF) * scale;
        right = left + mLabelRight[0].getWidth() * scale;
        top = bottom - mLabelRight[0].getHeight() * scale;
        rectF.set(left, top, right, bottom);
        canvas.drawBitmap(mLabelRight[0], null, rectF, null);

        drawUpDownValue(canvas, mUpMoveData.targetValue, mBitmapDigits100, UP_TOP_REF);
        drawUpDownValue(canvas, mDownMoveData.targetValue, mBitmapDigits70, mOuterCircle.getHeight() - UP_TOP_REF);
        drawLeftRightValue(canvas, mLeftMoveData.targetValue, mBitmapDigits70, LEFT_LEFT_REF, true);
        drawLeftRightValue(canvas, mRightMoveData.targetValue, mBitmapDigits100, mOuterCircle.getWidth() - LEFT_LEFT_REF, false);
    }

    private void drawUpDownValue(Canvas canvas, float value, Bitmap[] bitmapDigits, float topRef) {
        float scale = mOuterCircle.getHeight() / SIZE;
        RectF rectF = new RectF();
        int[] digits = ViewUtils.getDigits(value, 2);
        float totalWidth = getTotalWidth(digits);
        float left = (mOuterCircle.getWidth() - totalWidth) / 2 * scale;
        float top = topRef * scale;
        float right;
        for (int digit : digits) {
            if (digit == ViewUtils.INDEX_DOT) {
                right = left + bitmapDigits[digit].getWidth() * scale;
                float bottom = top + bitmapDigits[0].getHeight() / 2 * scale;
                rectF.set(left,
                        bottom - bitmapDigits[digit].getHeight() * scale,
                        right,
                        bottom);
            } else {
                right = left + bitmapDigits[digit].getWidth() * scale;
                rectF.set(left,
                        top - bitmapDigits[digit].getHeight() / 2 * scale,
                        right,
                        top + bitmapDigits[digit].getHeight() / 2 * scale);
            }
            canvas.drawBitmap(bitmapDigits[digit], null, rectF, null);
            left = right;
        }
    }

    private void drawLeftRightValue(Canvas canvas, float value, Bitmap[] bitmapDigits, float leftRef, boolean isLeft) {
        float scale = mOuterCircle.getHeight() / SIZE;
        RectF rectF = new RectF();
        int[] digits = ViewUtils.getDigits(value, 2);
        float totalWidth = getTotalWidth(digits);
        float left;
        if (isLeft) {
            left = (leftRef - totalWidth) * scale;
        } else {
            left = leftRef * scale;
        }
        float top = (mOuterCircle.getHeight() + ARROW_SPACE) / 2 * scale;
        float right;
        for (int digit : digits) {
            if (digit == ViewUtils.INDEX_DOT) {
                right = left + bitmapDigits[digit].getWidth();
                float bottom = top + bitmapDigits[0].getHeight() * scale;
                rectF.set(left,
                        bottom - bitmapDigits[digit].getHeight() * scale,
                        right,
                        bottom);
            } else {
                right = left + bitmapDigits[digit].getWidth();
                rectF.set(left,
                        top,
                        right,
                        top + bitmapDigits[digit].getHeight() * scale);
            }
            canvas.drawBitmap(bitmapDigits[digit], null, rectF, null);
            left = right;
        }
    }

    private float getTotalWidth(int[] digits) {
        float totalWidth = 0;
        for (int digit : digits) {
            totalWidth += mBitmapDigits100[digit].getWidth();
        }
        return totalWidth;
    }

    public void setUpValue(float value) {
        setValue(mUpMoveData, value);
    }

    public void setDownValue(float value) {
        setValue(mDownMoveData, value);
    }

    public void setLeftValue(float value) {
        setValue(mLeftMoveData, value);
    }

    public void setRightValue(float value) {
        setValue(mRightMoveData, value);
    }

    private void setValue(MoveData moveData, float value) {
        if (Math.abs(moveData.targetValue - value) < 0.001f) {
            return;
        }
        moveData.targetValue = value;
        computeCurrentValue(moveData);
    }

    private void computeCurrentValue(MoveData moveData) {
        if (!(Math.abs(moveData.currentValue - moveData.targetValue) > 0.001f)) {
            return;
        }

        if (-1 != moveData.lastMoved) {
            final float time = (System.currentTimeMillis() - moveData.lastMoved) / 1000.0f;
            final float direction = Math.signum(moveData.velocity);
            if (Math.abs(moveData.velocity) < 90.0f) {
                moveData.acceleration = 5.0f * (moveData.targetValue - moveData.currentValue);
            } else {
                moveData.acceleration = 0.0f;
            }

            moveData.acceleration = 5.0f * (moveData.targetValue - moveData.currentValue);
            moveData.currentValue += moveData.velocity * time;
            moveData.velocity += moveData.acceleration * time;

            if ((moveData.targetValue - moveData.currentValue) * direction < 0.001f * direction) {
                moveData.currentValue = moveData.targetValue;
                moveData.velocity = 0.0f;
                moveData.acceleration = 0.0f;
                moveData.lastMoved = -1L;
            } else {
                moveData.lastMoved = System.currentTimeMillis();
            }

            invalidate();

        } else {
            moveData.lastMoved = System.currentTimeMillis();
            computeCurrentValue(moveData);
        }
    }

    static class MoveData {
        public float targetValue;
        public float currentValue;
        public long lastMoved;
        public float velocity;
        public float acceleration;
    }
}
