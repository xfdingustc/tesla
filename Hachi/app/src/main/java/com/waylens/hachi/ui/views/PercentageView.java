package com.waylens.hachi.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2015/12/28.
 */
public class PercentageView extends View {
    private static final String TAG = PercentageView.class.getSimpleName();

    private int mDefaultColor;
    private int mCategoryOneColor;
    private int mCategoryTwoColor;
    private int mCategoryThreeColor;

    private int mProgress;
    private int mMax;

    private Paint mBackgroundPaint = new Paint();

    private boolean mFolded;
    private int mFoldedHeight;



    public PercentageView(Context context) {
        this(context, null);
    }

    public PercentageView(Context context, AttributeSet attrs) {
        super(context, attrs);


        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PercentageView);
        mFolded = a.getBoolean(R.styleable.PercentageView_folded, false);
        mFoldedHeight = a.getDimensionPixelSize(R.styleable.PercentageView_foldedHeight, 12);
        mDefaultColor = a.getColor(R.styleable.PercentageView_defaultColor, 0xD7D7D7);
        mCategoryOneColor = a.getColor(R.styleable.PercentageView_categoryOneColor, 0xFF000000);
        mCategoryTwoColor = a.getColor(R.styleable.PercentageView_categoryTwoColor, 0xFF000000);
        mCategoryThreeColor = a.getColor(R.styleable.PercentageView_categoryThreeColor, 0xFF000000);

        mBackgroundPaint.setColor(mDefaultColor);

        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mFolded) {
            setMeasuredDimension(width, mFoldedHeight);
        } else {
            setMeasuredDimension(width, height);
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mDefaultColor);

        Paint paint = new Paint();
        Rect rect = canvas.getClipBounds();

        if (mMax != 0) {
//            Logger.t(TAG).d("Progress: " + mProgress + " Max: " + mMax);
            int width1 = (int)(((float)mProgress /mMax) * rect.width());
            Rect rect1 = new Rect(rect.left, rect.top, rect.left + width1, rect.bottom);
//            Logger.t(TAG).d("Rect: " + rect1.toString());
            paint.setColor(mCategoryOneColor);
            canvas.drawRect(rect1, paint);
        }


    }


    public void setFolded(boolean folded) {
        mFolded = folded;
        requestLayout();
    }



    public synchronized void setProgress(int progress) {
        mProgress = progress;
        invalidate();
    }

    public synchronized void setMax(int max) {
        mMax = max;

    }
}
