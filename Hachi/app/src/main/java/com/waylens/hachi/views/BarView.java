package com.waylens.hachi.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.utils.ViewUtils;

/**
 * Bar view for Whp, PSI, and baro
 * <p>
 * Created by liangyx on 7/14/15.
 */
public class BarView extends View {

    private static final int WHP = 0;
    private static final int PSI = 1;
    private static final int BARO = 2;

    private static final int SIZE = 236;
    private static final int REF_HEIGHT = 101;
    private static final int REF_WIDTH = 236;
    private static final int BAR_TOP = 10;
    private static final int BAR_LEFT = 16;
    private static final int LABEL_TOP = 12;
    private static final int LABEL_LEFT = 88;
    private static final int NUMBER_LEFT = 20;

    private static final int BOTTOM_LABEL_LEFT = 24;
    private static final int BOTTOM_LABEL_TOP = 85;
    private static final int BOTTOM_LABEL_RIGHT = 218;

    private static final int TOTAL_BARS = 18;

    private Bitmap mBackground;
    private Bitmap mBgImage;
    private Bitmap[] mBars;
    private Bitmap mLabel;
    private Bitmap[] mStartNumbers;
    private Bitmap[] mEndNumbers;
    private Bitmap[] mDigitBitmaps;

    private Paint mBackgroundPaint;
    private Paint mBottomLabelPaint;

    private int mType;
    private int mFractionDigits;
    private float mStartValue;
    private float mEndValue;
    private float mTargetValue;
    private float mCurrentValue;

    private long mNeedleLastMoved = -1;
    private float mNeedleVelocity;
    private float mNeedleAcceleration;

    public BarView(Context context) {
        this(context, null, 0);
    }

    public BarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readAttrs(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public BarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void readAttrs(final Context context, final AttributeSet attrs, final int defStyle) {
        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.BarView, defStyle, 0);
        int resourceId = attributes.getResourceId(R.styleable.BarView_labelSrc, -1);
        mLabel = BitmapFactory.decodeResource(getResources(), resourceId);
        mType = attributes.getInt(R.styleable.BarView_barType, -1);
        attributes.recycle();
    }

    private void init() {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setFilterBitmap(true);

        Resources resources = getResources();
        mBgImage = BitmapFactory.decodeResource(resources, R.drawable.bar_background);

        TypedArray barIds = resources.obtainTypedArray(R.array.bars_img);
        mBars = new Bitmap[barIds.length()];
        for (int i = 0; i < mBars.length; i++) {
            mBars[i] = BitmapFactory.decodeResource(resources, barIds.getResourceId(i, -1));
        }
        barIds.recycle();

        TypedArray resIds = resources.obtainTypedArray(R.array.numbers_22px_opa100);
        mDigitBitmaps = new Bitmap[resIds.length()];
        for (int i = 0; i < mDigitBitmaps.length; i++) {
            mDigitBitmaps[i] = BitmapFactory.decodeResource(resources, resIds.getResourceId(i, -1));
        }
        resIds.recycle();

        switch (mType) {
            case WHP:
                mStartNumbers = new Bitmap[3];
                mStartNumbers[0] = BitmapFactory.decodeResource(getResources(), R.drawable.numbers_1_16px_opa100);
                mStartNumbers[1] = BitmapFactory.decodeResource(getResources(), R.drawable.numbers_0_16px_opa100);
                mStartNumbers[2] = mStartNumbers[1];
                mEndNumbers = new Bitmap[4];
                mEndNumbers[0] = mStartNumbers[0];
                mEndNumbers[1] = mStartNumbers[1];
                mEndNumbers[2] = mStartNumbers[1];
                mEndNumbers[3] = mStartNumbers[1];
                mStartValue = 100;
                mEndValue = 1000;
                mFractionDigits = 0;
                break;
            case PSI:
                mStartNumbers = new Bitmap[3];
                mStartNumbers[0] = BitmapFactory.decodeResource(getResources(), R.drawable.minus_16px);
                mStartNumbers[1] = BitmapFactory.decodeResource(getResources(), R.drawable.numbers_3_16px_opa100);
                mStartNumbers[2] = BitmapFactory.decodeResource(getResources(), R.drawable.numbers_0_16px_opa100);
                mEndNumbers = new Bitmap[2];
                mEndNumbers[0] = mStartNumbers[1];
                mEndNumbers[1] = BitmapFactory.decodeResource(getResources(), R.drawable.numbers_5_16px_opa100);
                mStartValue = -30;
                mEndValue = 35;
                mFractionDigits = 0;
                break;
            case BARO:
                mStartNumbers = new Bitmap[2];
                mStartNumbers[0] = BitmapFactory.decodeResource(getResources(), R.drawable.numbers_2_16px_opa100);
                mStartNumbers[1] = BitmapFactory.decodeResource(getResources(), R.drawable.numbers_6_16px_opa100);
                mEndNumbers = new Bitmap[2];
                mEndNumbers[0] = BitmapFactory.decodeResource(getResources(), R.drawable.numbers_3_16px_opa100);
                mEndNumbers[1] = mStartNumbers[0];
                mStartValue = 26;
                mEndValue = 32;
                mFractionDigits = 1;
                break;
            default:
                break;
        }

        mBottomLabelPaint = new Paint();
        mBottomLabelPaint.setColorFilter(new ColorMatrixColorFilter(
                new float[]{
                        0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0,
                        1, 1, 1, 1, 0,
                }));
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int bgHeight = mBgImage.getHeight();
        int bgWidth = mBgImage.getWidth();

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
        drawBackground();
    }

    private void drawBackground() {
        if (null != mBackground) {
            mBackground.recycle();
        }
        mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mBackground);
        canvas.drawBitmap(mBgImage, null, new Rect(0, 0, getWidth(), getHeight()), mBackgroundPaint);

        float hScale = getHeight() / REF_HEIGHT;
        float wScale = getWidth() / REF_WIDTH;
        float left = LABEL_LEFT * wScale;
        float top = LABEL_TOP * hScale;
        if (mLabel != null) {
            RectF rectF = new RectF(left, top, left + mLabel.getWidth() * wScale, top + mLabel.getHeight() * hScale);
            canvas.drawBitmap(mLabel, null, rectF, mBackgroundPaint);
        }
    }

    private void drawBottomLabel(Canvas canvas) {
        float hScale = getHeight() / REF_HEIGHT;
        float wScale = getWidth() / REF_WIDTH;
        float left = BOTTOM_LABEL_LEFT * wScale;
        float top;
        for (Bitmap bitmap : mStartNumbers) {
            top = BOTTOM_LABEL_TOP - bitmap.getHeight() * hScale / 2;
            float right = left + bitmap.getWidth() * wScale;
            RectF rectF = new RectF(left, top, right, top + bitmap.getHeight() * hScale);
            canvas.drawBitmap(bitmap, null, rectF, mBottomLabelPaint);
            left = right;
        }

        left = BOTTOM_LABEL_RIGHT;
        for (int i = mEndNumbers.length - 1; i >= 0; i--) {
            left = (left - mEndNumbers[i].getWidth()) * wScale;
            top = BOTTOM_LABEL_TOP - mEndNumbers[i].getHeight() * hScale / 2;
            float right = left + mEndNumbers[i].getWidth() * wScale;
            RectF rectF = new RectF(left, top, right, top + mEndNumbers[i].getHeight() * hScale);
            canvas.drawBitmap(mEndNumbers[i], null, rectF, mBottomLabelPaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (null != mBackground) {
            canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);
        }
        drawBar(canvas);
        drawBottomLabel(canvas);
        computeCurrentValue();
    }

    private void drawBar(Canvas canvas) {
        float hScale = getHeight() / REF_HEIGHT;
        float wScale = getWidth() / REF_WIDTH;

        float left = BAR_LEFT * wScale;
        float top = BAR_TOP * hScale;

        int index = valueToIndex(mCurrentValue);
        RectF mBarRectF = new RectF(left, top, left + mBars[index].getWidth() * wScale, top + mBars[index].getHeight() * hScale);
        canvas.drawBitmap(mBars[index], null, mBarRectF, mBackgroundPaint);

        int[] digits = ViewUtils.getDigits(mTargetValue, mFractionDigits);
        left = NUMBER_LEFT * wScale;
        float right;
        float bottom;
        for (int digit : digits) {
            top = LABEL_TOP * hScale;
            right = left + mDigitBitmaps[digit].getWidth() * wScale;
            if (digit == ViewUtils.INDEX_DOT) {
                bottom = top + mDigitBitmaps[0].getHeight() * hScale;
                top = bottom - mDigitBitmaps[digit].getHeight() * hScale;
            } else if (digit == ViewUtils.INDEX_MINUS) {
                top = top + (mDigitBitmaps[0].getHeight() - mDigitBitmaps[digit].getHeight()) * hScale / 2;
                bottom = top + mDigitBitmaps[digit].getHeight() * hScale;
            } else {
                bottom = top + mDigitBitmaps[digit].getHeight() * hScale;
            }
            mBarRectF.set(left,
                    top,
                    left + mDigitBitmaps[digit].getWidth() * wScale,
                    bottom);
            canvas.drawBitmap(mDigitBitmaps[digit], null, mBarRectF, null);
            left = right;
        }
    }

    private int valueToIndex(float value) {
        int index = 0;
        if (value < mStartValue) {
            index = 0;
        } else {
            index = (int) ((value - mStartValue) * TOTAL_BARS / (mEndValue - mStartValue)) + 1;
            if (index > (TOTAL_BARS + 1)) {
                index = TOTAL_BARS + 1;
            }
        }
        return index;
    }

    public void setTargetValue(float targetValue) {
        if (targetValue > mEndValue
                || targetValue < mStartValue
                || targetValue == mTargetValue) {
            return;
        }
        mTargetValue = targetValue;
        //Log.e("test", "mTargetIndex: [" + mTargetValue + "]:" + valueToIndex(mTargetValue) + "   =========================");
        computeCurrentValue();

    }

    private void computeCurrentValue() {
        if (!(Math.abs(mCurrentValue - mTargetValue) > 0.01f)) {
            return;
        }

        if (-1 != mNeedleLastMoved) {
            final float time = (System.currentTimeMillis() - mNeedleLastMoved) / 1000.0f;
            final float direction = Math.signum(mNeedleVelocity);
            if (Math.abs(mNeedleVelocity) < 90.0f) {
                mNeedleAcceleration = 5.0f * (mTargetValue - mCurrentValue);
            } else {
                mNeedleAcceleration = 0.0f;
            }

            mNeedleAcceleration = 5.0f * (mTargetValue - mCurrentValue);
            mCurrentValue += mNeedleVelocity * time;
            mNeedleVelocity += mNeedleAcceleration * time;

            if ((mTargetValue - mCurrentValue) * direction < 0.01f * direction) {
                mCurrentValue = mTargetValue;
                mNeedleVelocity = 0.0f;
                mNeedleAcceleration = 0.0f;
                mNeedleLastMoved = -1L;
            } else {
                mNeedleLastMoved = System.currentTimeMillis();
            }

            invalidate();

        } else {
            mNeedleLastMoved = System.currentTimeMillis();
            computeCurrentValue();
        }
    }
}
