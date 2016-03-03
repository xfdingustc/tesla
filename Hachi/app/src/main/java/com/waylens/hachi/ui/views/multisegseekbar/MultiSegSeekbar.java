package com.waylens.hachi.ui.views.multisegseekbar;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;

import java.util.List;

/**
 * Created by Xiaofei on 2016/2/29.
 */
public class MultiSegSeekbar extends View {
    private static final String TAG = MultiSegSeekbar.class.getSimpleName();
    private static final int DEFAULT_DIVIDER_WIDTH_DP = 4;
    private static final int DEFAULT_BAR_HEIGHT = 4;

    private int mActiveColor;
    private int mInactiveColor;


    private int mDividerWidth = 0;
    private int mBarHeight = 0;
    private List<Clip> mClipList;

    private ThumbView mThumb;
    private Bar mBar;

    private int mMax = 100;
    private int mProgress;

    private float mBarPaddingBottom;
    private float mCircleSize;
    private int mCircleColor;
    private int mDefaultWidth = 500;
    private int mDefaultHeight = 150;

    private static final int DEFAULT_BAR_COLOR = Color.LTGRAY;
    private static final float DEFAULT_BAR_WEIGHT_PX = 2;

    private static final float DEFAULT_BAR_PADDING_BOTTOM_DP = 24;

    private float mBarWeight = DEFAULT_BAR_WEIGHT_PX;

    private int mBarColor = DEFAULT_BAR_COLOR;

    private OnMultiSegSeekBarChangeListener mListener;


    public MultiSegSeekbar(Context context) {
        super(context);
        initAttributes(context, null, 0);
    }

    public MultiSegSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs, 0);

    }

    public MultiSegSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs, defStyleAttr);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MultiSegSeekbar(Context context, AttributeSet attrs, int defStyleAttr, int
            defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttributes(context, attrs, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;

        final int measureWidthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int measureHeightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int measureHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (measureWidthMode == MeasureSpec.AT_MOST || measureWidthMode == MeasureSpec.EXACTLY) {
            width = measureWidth;
        } else {
            width = mDefaultWidth;
        }

        if (measureHeightMode == MeasureSpec.AT_MOST) {
            height = Math.min(mDefaultHeight, measureHeight);
        } else if (measureHeight == MeasureSpec.EXACTLY) {
            height = measureHeight;
        } else {
            height = mDefaultHeight;
        }

        setMeasuredDimension(width, height);
    }

    public void setActiveClip(int position) {
        // reset progress:
        setProgress(0);
        mBar.setActiveIndex(position);
        invalidate();
    }

    private void initAttributes(Context context, AttributeSet attrs, final int defStyle) {
        Resources resources = getResources();
        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiSegSeekbar, defStyle, 0);

            mActiveColor = a.getColor(R.styleable.MultiSegSeekbar_segActiveColor, Color.WHITE);
            mInactiveColor = a.getColor(R.styleable.MultiSegSeekbar_segInactiveColor, Color
                    .GRAY);
            mDividerWidth = a.getDimensionPixelSize(R.styleable.MultiSegSeekbar_dividerWidth, ViewUtils
                    .dp2px(DEFAULT_DIVIDER_WIDTH_DP, resources));
            mBarHeight = a.getDimensionPixelSize(R.styleable.MultiSegSeekbar_barMinHeight,
                    ViewUtils.dp2px(DEFAULT_BAR_HEIGHT, resources));
            mBarPaddingBottom = a.getDimension(R.styleable.MultiSegSeekbar_barPaddingBottom,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            DEFAULT_BAR_PADDING_BOTTOM_DP, getResources().getDisplayMetrics()));

            mCircleSize = a.getDimension(R.styleable.MultiSegSeekbar_circleSize, 24);
            mCircleColor = a.getColor(R.styleable.MultiSegSeekbar_circleColor,
                    0xff3f51b5);
            a.recycle();
        }

    }

    public void setOnMultiSegSeekbarChangListener(OnMultiSegSeekBarChangeListener listener) {
        this.mListener = listener;
    }

    public int getMax() {
        return mMax;
    }

    public int getProgress() {
        return mProgress;
    }


    public void notifyDateSetChanged() {
        // recalcuate current progress;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Context context = getContext();

        float yPos = h - mBarPaddingBottom;

        float marginLeft = mCircleSize;
        float barLength = w - (2 * marginLeft);

        mBar = new Bar(context, marginLeft, yPos, barLength, mBarWeight, mBarColor, mDividerWidth, mActiveColor, mInactiveColor, mClipList);

        mThumb = new ThumbView(context);
        mThumb.init(context, yPos, mCircleSize, mCircleColor);

        mThumb.setX(0);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mClipList == null || mClipList.isEmpty()) {
            return;
        }

        mBar.draw(canvas);
        mThumb.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Logger.t(TAG).d("DDDD");
                onActionDown(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                onActionUp(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                onActionMove(event.getX());
                return true;
            default:
                return false;
        }
    }




    private void onActionDown(float x, float y) {
        if (!mThumb.isPressed() && mThumb.isInTargetZone(x, y)) {
            mThumb.press();
            if (mListener != null) {
                mListener.onStartTrackingTouch(this);
            }
        }
    }

    private void onActionUp(float x, float y) {
        if (mThumb.isPressed()) {
            mThumb.release();
            if (mListener != null) {
                mListener.onStopTrackingTouch(this);
            }
        }
    }

    private void onActionMove(float x) {
        if (x < mBar.getLeftX() || x > mBar.getRightX()) {

        } else {
            mThumb.setX(x);
            if (mListener != null) {
                mProgress = (int)(x * mMax / mBar.getWidth() );
                mListener.onProgressChanged(this, mProgress);
            }
            invalidate();
        }
    }


    public void setClipList(List<Clip> clipList) {
        this.mClipList = clipList;
        invalidate();
    }

    public void setProgress(int progress) {
        mProgress = progress;
        float thumbX = mProgress * mBar.getWidth() / mMax;
        mThumb.setX(thumbX);
        invalidate();
    }

    public interface OnMultiSegSeekBarChangeListener {
        void onStartTrackingTouch(MultiSegSeekbar seekBar);

        void onProgressChanged(MultiSegSeekbar seekBar, int progress);

        void onStopTrackingTouch(MultiSegSeekbar seekBar);

    }
}
