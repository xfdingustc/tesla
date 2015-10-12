package com.waylens.hachi.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.utils.ViewUtils;


public class GaugeView extends View {

    public static final float DIAMETER = 328.0f;

    public static final int SIZE = 328;
    public static final float TOP = 0.0f;
    public static final float LEFT = 0.0f;
    public static final float RIGHT = 1.0f;
    public static final float BOTTOM = 1.0f;
    public static final float CENTER = 0.5f;
    public static final boolean SHOW_OUTER_BORDER = true;
    public static final boolean SHOW_NEEDLE = true;
    public static final boolean SHOW_SCALE = false;
    public static final boolean SHOW_RANGES = true;
    public static final boolean SHOW_TEXT = false;

    public static final float SECTION_ONE = 24;
    public static final float SECTION_TWO = 27;
    public static final float NUMBER_POSITION = 6;
    public static final float DIVISION_LENGTH = 8;
    public static final float RPM_TOP = 92;
    public static final float MPH_TOP = 230;

    public static final float SCALE_START_VALUE = 0.0f;
    public static final float SCALE_END_VALUE = 10.0f;
    public static final float SCALE_START_ANGLE = 30.0f;
    public static final int SCALE_DIVISIONS = 10;
    public static final int SCALE_SUBDIVISIONS = 1;

    public static final int TEXT_SHADOW_COLOR = Color.argb(100, 0, 0, 0);
    public static final int TEXT_VALUE_COLOR = Color.WHITE;
    public static final int TEXT_UNIT_COLOR = Color.WHITE;
    public static final float TEXT_VALUE_SIZE = 0.3f;
    public static final float TEXT_UNIT_SIZE = 0.1f;

    public static final float TEXT_SCALE = 2.25f;

    // *--------------------------------------------------------------------- *//
    // Customizable properties
    // *--------------------------------------------------------------------- *//

    private boolean mShowOuterBorder;
    private boolean mShowScale;
    private boolean mShowRanges;
    private boolean mShowNeedle;
    private boolean mShowText;

    private float mOutArcTop;
    private float mInnerArcTop;
    private float mRPMTop;
    private float mMPHTop;
    private float mDivisionLength;

    private float mNumberPosition;
    private float mScaleStartValue;
    private float mScaleEndValue;
    private float mScaleStartAngle;
    private float mScaleEndAngle;

    private int mDivisions;
    private int mSubdivisions;

    private RectF mOuterBorderRect;
    private RectF mFaceRect;
    private RectF mRPMRect;
    private RectF mMPHRect;
    private RectF[] mNumberRects;
    private RectF mOuterArcRect;
    private RectF mInnerArcRect;
    private RectF mNeedleRect;

    private Bitmap mBackground;
    private Paint mBackgroundPaint;
    private Paint mOuterBorderPaint;
    private Paint mFacePaint;
    private Paint mFacePaintLight;
    private Paint mTextValuePaint;
    private Paint mTextUnitPaint;
    private Paint mScalePaint;
    private Paint mScalePaintDark;

    private String mTextValue;
    private String mTextUnit;
    private int mTextValueColor;
    private int mTextUnitColor;
    private int mTextShadowColor;
    private float mTextValueSize;
    private float mTextUnitSize;

    // *--------------------------------------------------------------------- *//

    private float mScaleRotation;
    private float mDivisionValue;
    private float mSubdivisionValue;
    private float mSubdivisionAngle;

    private float mTargetValue;
    private float mCurrentValue;

    private float mNeedleVelocity;
    private float mNeedleAcceleration;
    private long mNeedleLastMoved = -1;
    private boolean mNeedleInitialized;
    private Bitmap[] mBitmapNumbers;
    private Bitmap[] mBitmapNumbersDark;
    private Bitmap mBitmapRPM;
    private Bitmap mBitmapNeedle;
    private Bitmap mBitmapMPH;
    private Bitmap mOuterCircle;
    private Bitmap mInnerCircle;
    private float mSpeed;

    public GaugeView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        readAttrs(context, attrs, defStyle);
        init();
    }

    public GaugeView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GaugeView(final Context context) {
        this(context, null, 0);
    }

    private void readAttrs(final Context context, final AttributeSet attrs, final int defStyle) {
        Resources resources = getResources();

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GaugeView, defStyle, 0);
        mShowOuterBorder = a.getBoolean(R.styleable.GaugeView_showOuterBorder, SHOW_OUTER_BORDER);
        mShowNeedle = a.getBoolean(R.styleable.GaugeView_showNeedle, SHOW_NEEDLE);
        mShowScale = a.getBoolean(R.styleable.GaugeView_showScale, SHOW_SCALE);
        mShowRanges = a.getBoolean(R.styleable.GaugeView_showRanges, SHOW_RANGES);
        mShowText = a.getBoolean(R.styleable.GaugeView_showText, SHOW_TEXT);

        float outerBorderWidth = mShowOuterBorder ? a.getFloat(R.styleable.GaugeView_outerBorderWidth, SECTION_ONE) : 0.0f;
        mOutArcTop = outerBorderWidth / DIAMETER;
        mInnerArcTop = SECTION_TWO / DIAMETER;
        mRPMTop = RPM_TOP / DIAMETER;
        mMPHTop = MPH_TOP / DIAMETER;

        mDivisionLength = DIVISION_LENGTH / DIAMETER;

        mNumberPosition = (mShowScale || mShowRanges) ? a.getFloat(R.styleable.GaugeView_scalePosition, NUMBER_POSITION) : 0.0f;
        mNumberPosition = mNumberPosition / DIAMETER;

        mScaleStartValue = a.getFloat(R.styleable.GaugeView_scaleStartValue, SCALE_START_VALUE);
        mScaleEndValue = a.getFloat(R.styleable.GaugeView_scaleEndValue, SCALE_END_VALUE);
        mScaleStartAngle = a.getFloat(R.styleable.GaugeView_scaleStartAngle, SCALE_START_ANGLE);
        mScaleEndAngle = a.getFloat(R.styleable.GaugeView_scaleEndAngle, 360.0f - mScaleStartAngle);

        mDivisions = a.getInteger(R.styleable.GaugeView_divisions, SCALE_DIVISIONS);
        mSubdivisions = a.getInteger(R.styleable.GaugeView_subdivisions, SCALE_SUBDIVISIONS);

        if (mShowRanges) {
            mTextShadowColor = a.getColor(R.styleable.GaugeView_textShadowColor, TEXT_SHADOW_COLOR);
        }

        if (mShowText) {
            final int textValueId = a.getResourceId(R.styleable.GaugeView_textValue, 0);
            final String textValue = a.getString(R.styleable.GaugeView_textValue);
            mTextValue = (0 < textValueId) ? context.getString(textValueId) : (null != textValue) ? textValue : "";

            final int textUnitId = a.getResourceId(R.styleable.GaugeView_textUnit, 0);
            final String textUnit = a.getString(R.styleable.GaugeView_textUnit);
            mTextUnit = (0 < textUnitId) ? context.getString(textUnitId) : (null != textUnit) ? textUnit : "";
            mTextValueColor = a.getColor(R.styleable.GaugeView_textValueColor, TEXT_VALUE_COLOR);
            mTextUnitColor = a.getColor(R.styleable.GaugeView_textUnitColor, TEXT_UNIT_COLOR);
            mTextShadowColor = a.getColor(R.styleable.GaugeView_textShadowColor, TEXT_SHADOW_COLOR);

            mTextValueSize = a.getFloat(R.styleable.GaugeView_textValueSize, TEXT_VALUE_SIZE);
            mTextUnitSize = a.getFloat(R.styleable.GaugeView_textUnitSize, TEXT_UNIT_SIZE);
        }

        TypedArray numberResIds = resources.obtainTypedArray(R.array.gauge_numbers);
        mBitmapNumbers = new Bitmap[numberResIds.length()];
        for (int i = 0; i < mBitmapNumbers.length; i++) {
            mBitmapNumbers[i] = BitmapFactory.decodeResource(resources, numberResIds.getResourceId(i, -1));
        }
        numberResIds.recycle();

        numberResIds = resources.obtainTypedArray(R.array.gauge_numbersDark);
        mBitmapNumbersDark = new Bitmap[numberResIds.length()];
        for (int i = 0; i < mBitmapNumbersDark.length; i++) {
            mBitmapNumbersDark[i] = BitmapFactory.decodeResource(resources, numberResIds.getResourceId(i, -1));
        }
        numberResIds.recycle();

        mBitmapRPM = BitmapFactory.decodeResource(resources, R.drawable.speed_label_rpm_1000);
        mBitmapNeedle = BitmapFactory.decodeResource(resources, R.drawable.speed_needle);
        mBitmapMPH = BitmapFactory.decodeResource(resources, R.drawable.speed_label_mph);
        a.recycle();

        mOuterCircle = BitmapFactory.decodeResource(resources, R.drawable.speed_outer_circle_background);
        mInnerCircle = BitmapFactory.decodeResource(resources, R.drawable.speed_inner_circle_background);
    }

    @TargetApi(11)
    private void init() {
        // TODO Why isn't this working with HA layer?
        // The needle is not displayed although the onDraw() is being triggered by invalidate()
        // calls.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        initDrawingRects();
        initDrawingTools();

        // Compute the scale properties
        if (mShowRanges) {
            initScale();
        }
    }

    public void initDrawingRects() {
        // The drawing area is a rectangle of width 1 and height 1,
        // where (0,0) is the top left corner of the canvas.
        // Note that on Canvas X axis points to right, while the Y axis points downwards.
        mOuterBorderRect = new RectF(LEFT, TOP, RIGHT, BOTTOM);

        mOuterArcRect = new RectF(mOuterBorderRect.left + mOutArcTop, mOuterBorderRect.top + mOutArcTop,
                mOuterBorderRect.right - mOutArcTop, mOuterBorderRect.bottom - mOutArcTop);

        mInnerArcRect = new RectF(mOuterArcRect.left + mInnerArcTop, mOuterArcRect.top + mInnerArcTop,
                mOuterArcRect.right - mInnerArcTop, mOuterArcRect.bottom - mInnerArcTop);

        mFaceRect = new RectF(mOuterArcRect.left + mInnerArcTop / 2, mOuterArcRect.top + mInnerArcTop / 2,
                mOuterArcRect.right - mInnerArcTop / 2, mOuterArcRect.bottom - mInnerArcTop / 2);

        float height = mBitmapRPM.getHeight() / DIAMETER;
        float width = mBitmapRPM.getWidth() / DIAMETER;
        mRPMRect = new RectF(0.5f - width / 2, mRPMTop, 0.5f + width / 2, mRPMTop + height);

        height = mBitmapMPH.getHeight() / DIAMETER;
        width = mBitmapMPH.getWidth() / DIAMETER;
        mMPHRect = new RectF(0.5f - width / 2, mMPHTop, 0.5f + width / 2, mMPHTop + height);

        width = mBitmapNeedle.getWidth() / DIAMETER;
        mNeedleRect = new RectF(0.5f - width / 2, mOuterArcRect.top, 0.5f + width / 2, 0.5f);

        mNumberRects = new RectF[mBitmapNumbers.length];
        for (int i = 0; i < mBitmapNumbers.length; i++) {
            height = mBitmapNumbers[i].getHeight() / DIAMETER;
            width = mBitmapNumbers[i].getWidth() / DIAMETER;
            mNumberRects[i] = new RectF(0.5f - width / 2, mOuterArcRect.top + mNumberPosition, 0.5f + width / 2, mOuterArcRect.top + mNumberPosition + height);
        }

    }

    private void initDrawingTools() {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setFilterBitmap(true);

        if (mShowOuterBorder) {
            mOuterBorderPaint = getDefaultOuterBorderPaint();
        }
        if (mShowText) {
            mTextValuePaint = getDefaultTextValuePaint();
            mTextUnitPaint = getDefaultTextUnitPaint();
        }

        mFacePaint = getDefaultFacePaint();
        mFacePaintLight = getDefaultFacePaintLight();
        mScalePaint = getRangePaint();
        mScalePaintDark = getRangePaintDark();
    }

    private Paint getDefaultOuterBorderPaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //paint.setStyle(Paint.Style.FILL);
        //paint.setColor(Color.argb(100, 76, 76, 76));
        return paint;
    }

    public Paint getDefaultFacePaint() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(120, 0, 0, 0));
        //paint.setColor(Color.BLUE);
        return paint;
    }

    public Paint getDefaultFacePaintLight() {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //paint.setShader(new RadialGradient(0.5f, 0.5f, mFaceRect.width() / 2, new int[]{Color.rgb(50, 132, 206), Color.rgb(36, 89, 162),
        //        Color.rgb(27, 59, 131)}, new float[]dr{0.5f, 0.96f, 0.99f}, Shader.TileMode.MIRROR));
        paint.setColor(Color.argb(150, 30, 30, 30));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mInnerArcTop);
        //paint.setColor(Color.BLUE);
        return paint;
    }

    public Paint getDefaultTextValuePaint() {
        final Paint paint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setColor(mTextValueColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(0.005f);
        paint.setTextSize(mTextValueSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.SANS_SERIF);
        paint.setShadowLayer(0.01f, 0.002f, 0.002f, mTextShadowColor);
        return paint;
    }

    public Paint getDefaultTextUnitPaint() {
        final Paint paint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setColor(mTextUnitColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(0.005f);
        paint.setTextSize(mTextUnitSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setShadowLayer(0.01f, 0.002f, 0.002f, mTextShadowColor);
        return paint;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state) {
        final Bundle bundle = (Bundle) state;
        final Parcelable superState = bundle.getParcelable("superState");
        super.onRestoreInstanceState(superState);

        mNeedleInitialized = bundle.getBoolean("needleInitialized");
        mNeedleVelocity = bundle.getFloat("needleVelocity");
        mNeedleAcceleration = bundle.getFloat("needleAcceleration");
        mNeedleLastMoved = bundle.getLong("needleLastMoved");
        mCurrentValue = bundle.getFloat("currentValue");
        mTargetValue = bundle.getFloat("targetValue");
    }

    private void initScale() {
        mScaleRotation = (mScaleStartAngle + 180) % 360;
        mDivisionValue = (mScaleEndValue - mScaleStartValue) / mDivisions;
        mSubdivisionValue = mDivisionValue / mSubdivisions;
        mSubdivisionAngle = (mScaleEndAngle - mScaleStartAngle) / (mDivisions * mSubdivisions);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        final Bundle state = new Bundle();
        state.putParcelable("superState", superState);
        state.putBoolean("needleInitialized", mNeedleInitialized);
        state.putFloat("needleVelocity", mNeedleVelocity);
        state.putFloat("needleAcceleration", mNeedleAcceleration);
        state.putLong("needleLastMoved", mNeedleLastMoved);
        state.putFloat("currentValue", mCurrentValue);
        state.putFloat("targetValue", mTargetValue);
        return state;
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
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        drawGauge();
    }

    private void drawGauge() {
        if (null != mBackground) {
            // Let go of the old background
            mBackground.recycle();
        }
        // Create a new background according to the new width and height
        mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(mBackground);
        final float scale = Math.min(getWidth(), getHeight());
        canvas.scale(scale, scale);
        canvas.translate((scale == getHeight()) ? ((getWidth() - scale) / 2) / scale : 0
                , (scale == getWidth()) ? ((getHeight() - scale) / 2) / scale : 0);

        drawRim(canvas);
        drawFace(canvas);

        if (mShowRanges) {
            drawScale(canvas);
        }

        canvas.drawBitmap(mBitmapRPM, null, mRPMRect, mBackgroundPaint);
        canvas.drawBitmap(mBitmapMPH, null, mMPHRect, mBackgroundPaint);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        drawBackground(canvas);

        final float scale = Math.min(getWidth(), getHeight());
        canvas.scale(scale, scale);
        canvas.translate((scale == getHeight()) ? ((getWidth() - scale) / 2) / scale : 0
                , (scale == getWidth()) ? ((getHeight() - scale) / 2) / scale : 0);

        drawFace(canvas);
        drawScale(canvas);

        canvas.drawBitmap(mBitmapRPM, null, mRPMRect, mBackgroundPaint);
        canvas.drawBitmap(mBitmapMPH, null, mMPHRect, mBackgroundPaint);
        drawSpeed(canvas);
        if (mShowNeedle) {
            drawNeedle(canvas);
        }
        computeCurrentValue();
    }

    private void drawSpeed(Canvas canvas) {
        int[] digits = ViewUtils.getDigits(mSpeed);
        float scale = getWidth() / DIAMETER;
        float top = 188 * scale;
        float bottom = top + mBitmapNumbers[0].getHeight() * scale * TEXT_SCALE;
        float left = (getWidth() - getTotalWidth(digits) * TEXT_SCALE) / 2 * scale;
        float right;
        RectF rectF = new RectF();
        for (int digit : digits) {
            right = left + mBitmapNumbers[digit].getWidth() * scale * TEXT_SCALE;
            rectF.set(left / DIAMETER, top / DIAMETER, right / DIAMETER, bottom / DIAMETER);
            canvas.drawBitmap(mBitmapNumbers[digit], null, rectF, null);
            left = right;
        }

    }

    private float getTotalWidth(int[] digits) {
        float totalWidth = 0;
        for (int digit : digits) {
            totalWidth += mBitmapNumbers[digit].getWidth();
        }
        return totalWidth;
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
    }

    private void drawBackground(final Canvas canvas) {
        if (null != mBackground) {
            canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);
        }
    }

    private void drawRim(final Canvas canvas) {
        canvas.drawBitmap(mOuterCircle, null, mOuterBorderRect, mOuterBorderPaint);
    }

    private void drawFace(final Canvas canvas) {
        float startAngle = 115;
        float totalAngle = 310;
        float angleOffset = 205;
        canvas.drawBitmap(mInnerCircle, null, mOuterArcRect, mFacePaint);

        final float angle = getAngleForValue(mCurrentValue);
        mFacePaintLight.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        float sweepAngle = angle - angleOffset;
        if (sweepAngle < 0) {
            sweepAngle += 360;
        }
        canvas.drawArc(mFaceRect, startAngle, sweepAngle, false, mFacePaintLight);

        canvas.drawArc(mOuterArcRect, startAngle, sweepAngle, false, mScalePaint);
        canvas.drawArc(mInnerArcRect, startAngle, sweepAngle, false, mScalePaint);

        canvas.drawArc(mOuterArcRect, startAngle + sweepAngle, totalAngle - sweepAngle, false, mScalePaintDark);
        canvas.drawArc(mInnerArcRect, startAngle + sweepAngle, totalAngle - sweepAngle, false, mScalePaintDark);


        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.rotate(155, 0.5f, 0.5f);
        canvas.drawLine(0.5f, mOutArcTop, 0.5f, mOutArcTop + mInnerArcTop, mScalePaint);
        canvas.rotate(50, 0.5f, 0.5f);
        canvas.drawLine(0.5f, mOutArcTop, 0.5f, mOutArcTop + mInnerArcTop, mScalePaint);
        canvas.restore();
    }

    private void drawScale(final Canvas canvas) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        // On canvas, North is 0 degrees, East is 90 degrees, South is 180 etc.
        // We start the scale somewhere South-West so we need to first rotate the canvas.
        canvas.rotate(mScaleRotation, 0.5f, 0.5f);

        final int totalTicks = mDivisions * mSubdivisions + 1;
        for (int i = 0; i < totalTicks; i++) {
            final float y1 = mInnerArcRect.top;
            final float y2 = y1 + mDivisionLength; // height of division
            final float y3 = y1 + 0.015f; // height of subdivision

            final float value = getValueForTick(i);
            float mod = value % mDivisionValue;
            if ((Math.abs(mod - 0) < 0.001) || (Math.abs(mod - mDivisionValue) < 0.001)) {
                if (value <= mCurrentValue) {
                    canvas.drawBitmap(mBitmapNumbers[i], null, mNumberRects[i], mScalePaint);
                    canvas.drawLine(0.5f, y1, 0.5f, y2, mScalePaint);
                } else {
                    canvas.drawBitmap(mBitmapNumbersDark[i], null, mNumberRects[i], mScalePaint);
                    canvas.drawLine(0.5f, y1, 0.5f, y2, mScalePaintDark);
                }
            } else {
                // Draw a subdivision tick
                canvas.drawLine(0.5f, y1, 0.5f, y3, mScalePaint);
            }
            canvas.rotate(mSubdivisionAngle, 0.5f, 0.5f);
        }
        canvas.restore();
    }

    private String valueString(final float value) {
        return String.format("%d", (int) value);
    }

    private float getValueForTick(final int tick) {
        return tick * (mDivisionValue / mSubdivisions);
    }

    private Paint getRangePaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.005f);
        paint.setTextSize(0.05f);
        paint.setTypeface(Typeface.SANS_SERIF);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setShadowLayer(0.005f, 0.002f, 0.002f, mTextShadowColor);
        return paint;
    }

    private Paint getRangePaintDark() {
        Paint paint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.005f);
        paint.setTextSize(0.05f);
        paint.setTypeface(Typeface.SANS_SERIF);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setShadowLayer(0.005f, 0.002f, 0.002f, mTextShadowColor);
        return paint;
    }

    private void drawNeedle(final Canvas canvas) {
        if (mNeedleInitialized) {
            final float angle = getAngleForValue(mCurrentValue);
            // Logger.log.info(String.format("value=%f -> angle=%f", mCurrentValue, angle));
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.rotate(angle, 0.5f, 0.5f);

            canvas.drawBitmap(mBitmapNeedle, null, mNeedleRect, mBackgroundPaint);
            canvas.restore();
        }
    }

    private float getAngleForValue(final float value) {
        return (mScaleRotation + ((value - mScaleStartValue) / mSubdivisionValue) * mSubdivisionAngle) % 360;
    }

    private void computeCurrentValue() {
        // Logger.log.warn(String.format("velocity=%f, acceleration=%f", mNeedleVelocity,
        // mNeedleAcceleration));

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

    public void setTargetValue(final float value) {
        if (mShowScale || mShowRanges) {
            if (value < mScaleStartValue) {
                mTargetValue = mScaleStartValue;
            } else if (value > mScaleEndValue) {
                mTargetValue = mScaleEndValue;
            } else {
                mTargetValue = value;
            }
        } else {
            mTargetValue = value;
        }
        mNeedleInitialized = true;
        invalidate();
    }

}

