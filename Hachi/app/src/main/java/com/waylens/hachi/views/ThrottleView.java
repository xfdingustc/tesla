package com.waylens.hachi.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;

import com.waylens.hachi.R;

/**
 * Throttle View
 * <p>
 * Created by liangyx on 7/14/15.
 */
public class ThrottleView extends View {


    private static final int SIZE = 324;
    private static final int METER_TOP = 16;
    private static final int METER_LEFT = 48;
    private static final int REF_HEIGHT = 324;
    private static final int REF_WIDTH = 165;
    private static final int LABEL_TOP = 10;
    private static final int LABEL_SPACE = 33;

    private static final int LABEL_THROTTLE_LEFT = 20;
    private static final int LABEL_THROTTLE_TOP = 270;

    private Bitmap mBackground;
    private Bitmap mBgImage;
    private Bitmap mBaseMeter;
    private Bitmap mLabelThrottle;

    private Bitmap mBitmapZero;
    private Bitmap[] mLabels;
    private SparseBooleanArray mLabelTags;

    private Paint mBackgroundPaint;


    public ThrottleView(Context context) {
        this(context, null, 0);
    }

    public ThrottleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThrottleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public ThrottleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        Resources resources = getResources();
        mBgImage = BitmapFactory.decodeResource(resources, R.drawable.throttle_background_panel);
        mBaseMeter = BitmapFactory.decodeResource(resources, R.drawable.throttle_meter_base);
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setFilterBitmap(true);

        mLabelThrottle = BitmapFactory.decodeResource(resources, R.drawable.throttle_label_throttle);

        mBitmapZero = BitmapFactory.decodeResource(resources, R.drawable.numbers_0_16px_opa100);

        mLabels = new Bitmap[8];
        mLabels[0] = BitmapFactory.decodeResource(resources, R.drawable.throttle_label_thr);
        mLabels[1] = BitmapFactory.decodeResource(resources, R.drawable.numbers_8_16px_opa100);
        mLabels[2] = BitmapFactory.decodeResource(resources, R.drawable.numbers_6_16px_opa100);
        mLabels[3] = BitmapFactory.decodeResource(resources, R.drawable.numbers_4_16px_opa100);
        mLabels[4] = BitmapFactory.decodeResource(resources, R.drawable.numbers_2_16px_opa100);
        mLabels[5] = BitmapFactory.decodeResource(resources, R.drawable.numbers_0_16px_opa100);
        mLabels[6] = BitmapFactory.decodeResource(resources, R.drawable.numbers_5_16px_opa100);
        mLabels[7] = BitmapFactory.decodeResource(resources, R.drawable.throttle_label_brk);
        mLabelTags = new SparseBooleanArray(8);
        mLabelTags.put(1, true);
        mLabelTags.put(2, true);
        mLabelTags.put(3, true);
        mLabelTags.put(4, true);
        mLabelTags.put(6, true);
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
        drawThrottle();
    }

    private void drawThrottle() {
        if (null != mBackground) {
            mBackground.recycle();
        }
        mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mBackground);
        float hScale = getHeight() / REF_HEIGHT;
        float wScale = getWidth() / REF_WIDTH;
        canvas.drawBitmap(mBgImage, null, new Rect(0, 0, getWidth(), getHeight()), mBackgroundPaint);

        float left = METER_LEFT * wScale;
        float top = METER_TOP * hScale;
        canvas.drawBitmap(mBaseMeter, null,
                new RectF(left, top, left + mBaseMeter.getWidth() * wScale, top + mBaseMeter.getHeight() * hScale),
                mBackgroundPaint);

        top = LABEL_TOP;
        int labelWidth = METER_LEFT - 3;
        for (int i = 0; i < mLabels.length; i++) {
            if (mLabelTags.get(i)) {
                left = (labelWidth - mLabels[i].getWidth() - mBitmapZero.getWidth()) * wScale;
                canvas.drawBitmap(mLabels[i], null,
                        new RectF(left, top * hScale, left + mLabels[i].getWidth() * wScale, top + mLabels[i].getHeight() * hScale),
                        mBackgroundPaint);
                float leftZero = left + mLabels[i].getWidth() * wScale;
                canvas.drawBitmap(mBitmapZero, null,
                        new RectF(leftZero, top * hScale, leftZero + mBitmapZero.getWidth() * wScale, top + mBitmapZero.getHeight() * hScale),
                        mBackgroundPaint);
            } else {
                left = (labelWidth - mLabels[i].getWidth()) * wScale;
                canvas.drawBitmap(mLabels[i], null,
                        new RectF(left, top * hScale, left + mLabels[i].getWidth() * wScale, top + mLabels[i].getHeight() * hScale),
                        mBackgroundPaint);
            }
            top = top + LABEL_SPACE;
        }

        left = LABEL_THROTTLE_LEFT * wScale;
        top = LABEL_THROTTLE_TOP * hScale;
        canvas.drawBitmap(mLabelThrottle, null,
                new RectF(left, top, left + mLabelThrottle.getWidth() * wScale, top + mLabelThrottle.getHeight() * hScale),
                mBackgroundPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (null != mBackground) {
            canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);
        }
    }

    float getWidthPX(int width) {
        if (getWidth() == 0) {
            return width;
        }
        return width * (getWidth() / REF_WIDTH);
    }

    float getHeightPX(int height) {
        if (getHeight() == 0) {
            return height;
        }
        return height * (getHeight() / REF_HEIGHT);
    }
}

