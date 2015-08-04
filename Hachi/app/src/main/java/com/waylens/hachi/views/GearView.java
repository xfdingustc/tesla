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
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.R;
/**
 * GearView implementation
 *
 * Created by liangyx on 7/13/15.
 */
public class GearView extends View {

    public static final int SIZE = 324;

    public static final float REF_HEIGHT = 324.0f;
    public static final float REF_WIDTH = 158.0f;

    private static final int TOP_DASH_OFFSET = 25;
    private static final int SPACE = 35;
    private static final int LABEL_TOP = 296;
    private static final int LABEL_LEFT = 36;


    private static final int MAX_GEAR = 6;
    private static final int MIN_GEAR = 0;

    private Bitmap mBackground;
    private Bitmap mBgImage;
    private Bitmap mGearLabel;
    private Bitmap[] mDashes;
    private Bitmap[] mNumbers;
    private Bitmap mCurrentGearIcon;
    private Bitmap mCurrentGearBox;
    private Paint mBackgroundPaint;

    private int mTargetValue;

    public GearView(Context context) {
        this(context, null, 0);
    }

    public GearView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GearView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public GearView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setFilterBitmap(true);
        mBgImage = BitmapFactory.decodeResource(getResources(), R.drawable.gear_background_panel);

        Resources resources = getResources();
        if (!isInEditMode()) {
            TypedArray resIds = resources.obtainTypedArray(R.array.gear_dashes);
            mDashes = new Bitmap[resIds.length()];
            for (int i = 0; i < mDashes.length; i++) {
                mDashes[i] = BitmapFactory.decodeResource(resources, resIds.getResourceId(i, -1));
            }
            resIds.recycle();

            resIds = resources.obtainTypedArray(R.array.gear_numbers);
            mNumbers = new Bitmap[resIds.length()];
            for (int i = 0; i < mNumbers.length; i++) {
                mNumbers[i] = BitmapFactory.decodeResource(resources, resIds.getResourceId(i, -1));
            }
            resIds.recycle();
        }

        mGearLabel = BitmapFactory.decodeResource(resources, R.drawable.gear_gear_label);
        mCurrentGearIcon = BitmapFactory.decodeResource(resources, R.drawable.gear_current_gear_icon);
        mCurrentGearBox = BitmapFactory.decodeResource(resources, R.drawable.gear_current_gear_highlight_box);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        // Loggable.log.debug(String.format("widthMeasureSpec=%s, heightMeasureSpec=%s",
        // View.MeasureSpec.toString(widthMeasureSpec),
        // View.MeasureSpec.toString(heightMeasureSpec)));

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
        drawGear();
    }

    private void drawGear() {
        if (mBackground != null) {
            mBackground.recycle();
        }

        mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBackground);
        drawBgImage(canvas);
        float hScale = getHeight() / REF_HEIGHT;
        float wScale = getWidth() / REF_WIDTH;

        canvas.drawBitmap(mGearLabel, LABEL_LEFT * wScale, LABEL_TOP * hScale, mBackgroundPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (null != mBackground) {
            canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);
        }
        drawGears(canvas);
    }

    private void drawBgImage(Canvas canvas) {
        canvas.drawBitmap(mBgImage, null, new Rect(0, 0, getWidth(), getHeight()), mBackgroundPaint);
    }

    private void drawGears(Canvas canvas) {
        if (mDashes == null) {
            return;
        }
        float hScale = getHeight() / REF_HEIGHT;
        float wScale = getWidth() / REF_WIDTH;
        float top = TOP_DASH_OFFSET * hScale;
        float left;
        float numberLeft = getWidth() - 31 * wScale;
        for (int i = 0; i < mDashes.length; i++) {
            //Log.e("test", "w: " + i + " : " + mDashes[i].getWidth());
            left = getWidth() - 44 * wScale - mDashes[i].getWidth();
            if ((mDashes.length - 1) - i == mTargetValue) {
                canvas.drawBitmap(mCurrentGearBox, getWidth() - mCurrentGearBox.getWidth(), top - (SPACE - 18) * hScale, mBackgroundPaint);
                canvas.drawBitmap(mCurrentGearIcon, getWidth() - 73 * wScale, top - (SPACE - 23) * hScale, mBackgroundPaint);
            } else {
                canvas.drawBitmap(mDashes[i], left, top, mBackgroundPaint);
            }

            canvas.drawBitmap(mNumbers[i], numberLeft, getAlignedTop(mDashes[i], top, mNumbers[i]), mBackgroundPaint);
            top = top + SPACE * hScale + mDashes[i].getHeight();
        }
    }

    /**
     * Valid value range [0..6]
     * 1 - 6 means gears
     * 0 means R
     *
     * @param targetValue target value
     */
    public void setTargetValue(int targetValue) {
        if (targetValue > MAX_GEAR || targetValue < MIN_GEAR) {
            return;
        }
        if (mTargetValue != targetValue) {
            mTargetValue = targetValue;
            invalidate();
        }

    }

    private float getAlignedTop(Bitmap dash, float dashTop, Bitmap label) {
        float centerVertical = dashTop + getHeightPX(dash.getHeight()) / 2;

        return centerVertical - getHeightPX(label.getHeight()) / 2;
    }

    /**
     * This function is used to convert bitmap size to absolution
     * px size
     *
     * @param width width
     * @return px
     */
    float getWidthPX(int width) {
        if (getWidth() == 0) {
            return width;
        }
        return width * (getWidth() / REF_WIDTH);
    }

    /**
     * This function is used to convert bitmap size to absolution
     * px size
     *
     * @param height height
     * @return px
     */
    float getHeightPX(int height) {
        if (getHeight() == 0) {
            return height;
        }
        return height * (getHeight() / REF_HEIGHT);
    }
}
