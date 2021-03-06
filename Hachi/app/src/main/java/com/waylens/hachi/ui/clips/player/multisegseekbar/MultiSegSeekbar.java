package com.waylens.hachi.ui.clips.player.multisegseekbar;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.eventbus.events.ClipSetChangeEvent;
import com.waylens.hachi.eventbus.events.ClipSetPosChangeEvent;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.snipe.vdb.ClipSetPos;
import com.waylens.hachi.utils.ViewUtils;



import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by Xiaofei on 2016/2/29.
 */
public class MultiSegSeekbar extends View {
    private static final String TAG = MultiSegSeekbar.class.getSimpleName();
    private static final int DEFAULT_DIVIDER_WIDTH_DP = 4;
    private static final int DEFAULT_BAR_HEIGHT = 4;

    private int mActiveColor;
    private int mInactiveColor;

    private int mProgressColor;


    private int mDividerWidth = 0;
    private int mBarHeight = 0;
    private int mClipListIndex;

    private ThumbView mThumb;
    private Bar mBar;

    private int mMax = 100;
    private int mProgress;

    private float mBarPaddingBottom;
    private float mCircleSize;
    private int mCircleColor;
    private int mDefaultWidth = 500;
    private int mDefaultHeight = 150;
    private boolean mIsMulti = true;
    private int mCurrentClipIndex = 0;



    private static final int DEFAULT_BAR_COLOR = Color.LTGRAY;

    private static final float DEFAULT_BAR_PADDING_BOTTOM_DP = 18;

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
    public MultiSegSeekbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
//            height = Math.min(mDefaultHeight, measureHeight);
            height = (int)(mCircleSize * 4);
        } else if (measureHeight == MeasureSpec.EXACTLY) {
            height = measureHeight;
        } else {
            height = mDefaultHeight;
        }

        setMeasuredDimension(width, height);
    }

//    @Override
//    public Parcelable onSaveInstanceState() {
//        Bundle bundle = new Bundle();
//        bundle.putParcelable("instanceState", super.onSaveInstanceState());
//        bundle.putSerializable("ClipSetPos", getCurrentClipSetPos());
//        return bundle;
//    }
//
//
//    @Override
//    protected void onRestoreInstanceState(Parcelable state) {
//        if (state instanceof Bundle) {
//            Bundle bundle = (Bundle) state;
//            ClipSetPos clipSetPos = (ClipSetPos)bundle.getSerializable("ClipSetPos");
//            setClipSetPos(clipSetPos);
//            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
//        } else {
//            super.onRestoreInstanceState(state);
//        }
//    }

    public void setMultiStyle(boolean isMulti) {
        mIsMulti = isMulti;
        invalidate();
    }


    public ClipSetPos getCurrentClipSetPos() {
        return mBar.getClipSetPos(mThumb.getX());
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetPosChanged(ClipSetPosChangeEvent event) {
        ClipSetPos clipSetPos = event.getClipSetPos();

        setClipSetPos(clipSetPos);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventClipSetChanged(ClipSetChangeEvent event) {
        if (mClipListIndex == event.getIndex()) {
            ClipSetPos clipSetPos = new ClipSetPos(mCurrentClipIndex, getClipSet().getClip(mCurrentClipIndex).editInfo.selectedStartValue);
            mBar.setClipSetList(getClipSet().getClipList());
            float newX = mBar.setClipSetPos(clipSetPos);
            mThumb.setX(newX);
            invalidate();
        }
    }



    private void initAttributes(Context context, AttributeSet attrs, final int defStyle) {
        if (isInEditMode()) {
            return;
        }
        Resources resources = getResources();
        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiSegSeekbar, defStyle, 0);

            mActiveColor = a.getColor(R.styleable.MultiSegSeekbar_segActiveColor, Color.WHITE);
            mInactiveColor = a.getColor(R.styleable.MultiSegSeekbar_segInactiveColor, Color.GRAY);
            mProgressColor = a.getColor(R.styleable.MultiSegSeekbar_progressColor, getResources().getColor(R.color.style_color_primary));
            mDividerWidth = a.getDimensionPixelSize(R.styleable.MultiSegSeekbar_dividerWidth,
                ViewUtils.dp2px(DEFAULT_DIVIDER_WIDTH_DP));
            mBarHeight = a.getDimensionPixelSize(R.styleable.MultiSegSeekbar_barHeight,
                ViewUtils.dp2px(DEFAULT_BAR_HEIGHT));
            mBarPaddingBottom = a.getDimension(R.styleable.MultiSegSeekbar_barPaddingBottom,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_BAR_PADDING_BOTTOM_DP, getResources().getDisplayMetrics()));

            mCircleSize = a.getDimension(R.styleable.MultiSegSeekbar_circleSize, 24);
            mCircleColor = a.getColor(R.styleable.MultiSegSeekbar_circleColor, 0xff3f51b5);
            a.recycle();
        }

    }

    private void setClipSetPos(ClipSetPos clipSetPos) {
        if (clipSetPos == null) {
            return;
        }
        mCurrentClipIndex = clipSetPos.getClipIndex();
        float newX = mBar.setClipSetPos(clipSetPos);
        mThumb.setX(newX);
        invalidate();
    }

    public void setOnMultiSegSeekbarChangListener(OnMultiSegSeekBarChangeListener listener) {
        this.mListener = listener;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Context context = getContext();

        float yPos = h - mCircleSize * 2;

        float marginLeft = mCircleSize * 2;
        float barLength = w - (2 * marginLeft);

        float oldThumbXOffset = 0;
        if (mThumb != null) {
            oldThumbXOffset = mThumb.getX() - marginLeft;
        }

        if (getClipSet() == null) {
            return;
        }

        mBar = new Bar(context, marginLeft, yPos, barLength, mBarHeight, mBarColor, mDividerWidth,
            mActiveColor, mInactiveColor, mProgressColor, mIsMulti, getClipSet().getClipList());

        mThumb = new ThumbView(context);
        mThumb.init(context, yPos, mCircleSize, mCircleColor, mBar.getLeftX(), mBar.getRightX());

        float newThumbXOffset = 0;
        if (oldw != 0) {
            newThumbXOffset = oldThumbXOffset * w / oldw;
        }


        mThumb.setX(marginLeft + newThumbXOffset);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getClipSet() == null) {
            return;
        }

        mBar.draw(canvas, getClipSet().getClipList(), mThumb.getX());
        mThumb.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
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
            Logger.t(TAG).d("X: " + x + " left: " + mBar.getLeftX() + " right: " + mBar.getRightX());
        } else {
            mThumb.setX(x);
            if (mListener != null) {
                //mProgress = (int)(x * mMax / mBar.getWidth() );
                ClipSetPos clipPos = mBar.getClipSetPos(x);
                mListener.onProgressChanged(this, clipPos);
            }
            invalidate();
        }
    }


    public void setClipList(int clipSetIndex) {
        this.mClipListIndex = clipSetIndex;
        invalidate();
    }


    private ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mClipListIndex);
    }

    public interface OnMultiSegSeekBarChangeListener {
        void onStartTrackingTouch(MultiSegSeekbar seekBar);

        void onProgressChanged(MultiSegSeekbar seekBar, ClipSetPos clipPos);

        void onStopTrackingTouch(MultiSegSeekbar seekBar);

    }
}
