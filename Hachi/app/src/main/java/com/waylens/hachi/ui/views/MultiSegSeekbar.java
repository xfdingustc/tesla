package com.waylens.hachi.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.utils.ViewUtils;
import com.waylens.hachi.vdb.Clip;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.util.List;

/**
 * Created by Xiaofei on 2016/2/29.
 */
public class MultiSegSeekbar extends View {
    private static final int DEFAULT_DIVIDER_WIDTH_DP = 4;
    private static final int DEFAULT_BAR_HEIGHT = 4;

    private int mActiveColor;
    private int mInactiveColor;

    private Paint mActivePaint = new Paint();
    private Paint mInactivePaint = new Paint();

    private int mDividerWidth = 0;
    private int mBarHeight = 0;
    private int mActiveIndex = 0;
    private List<Clip> mClipList;

    private ThumbView mThumb;
    private int mBarPaddingBottom;
    private float mCircleSize;
    private int mCircleColor;


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

    public void setActiveClip(int position) {
        mActiveIndex = position;
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

            mCircleSize = a.getDimension(R.styleable.MultiSegSeekbar_circleSize, 15);
            mCircleColor = a.getColor(R.styleable.MultiSegSeekbar_circleColor,
                    0xff3f51b5);
            a.recycle();
        }

        mActivePaint.setColor(mActiveColor);
        mInactivePaint.setColor(mInactiveColor);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Context context = getContext();

        float yPos = h - mBarPaddingBottom;

        mThumb = new ThumbView(context);
        mThumb.init(context, yPos, mCircleSize, mCircleColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mClipList == null || mClipList.isEmpty()) {
            return;
        }
        int top = (canvas.getHeight() - mBarHeight) / 2;

        long totalClipTimeMs = 0;
        for (Clip clip : mClipList) {
            totalClipTimeMs += clip.editInfo.getSelectedLength();
        }

        float widthScale = ((float)(canvas.getWidth() - (mClipList.size() - 1) * mDividerWidth))
            / totalClipTimeMs;


        int left = 0;

        for (int i = 0; i < mClipList.size(); i++) {
            Clip clip = mClipList.get(i);
            Rect rect = new Rect();
            rect.top = top;
            rect.bottom = top + mBarHeight;
            rect.left = left;
            rect.right = rect.left + (int)(widthScale * clip.editInfo.getSelectedLength());

            if (i == mActiveIndex) {
                canvas.drawRect(rect, mActivePaint);
            } else {
                canvas.drawRect(rect, mInactivePaint);
            }
            left = rect.right + mDividerWidth;
        }
        mThumb.draw(canvas);
    }


    public void setClipList(List<Clip> clipList) {
        this.mClipList = clipList;
        invalidate();
    }
}
