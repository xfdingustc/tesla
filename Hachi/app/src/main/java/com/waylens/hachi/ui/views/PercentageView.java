package com.waylens.hachi.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2015/12/28.
 */
public class PercentageView extends View {

    private int mDefaultColor;
    private int mCategoryOneColor;
    private int mCategoryTwoColor;
    private int mCategoryThreeColor;

    private Paint mBackgroundPaint = new Paint();


    private int mCategoryOne = 0;
    private int mCategoryTwo = 0;
    private int mCategoryThree = 0;

    public PercentageView(Context context) {
        this(context, null);
    }

    public PercentageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PercentageView);
        mDefaultColor = a.getColor(R.styleable.PercentageView_defaultColor, 0xD7D7D7);
        mCategoryOneColor = a.getColor(R.styleable.PercentageView_categoryOneColor, 0xFF000000);
        mCategoryTwoColor = a.getColor(R.styleable.PercentageView_categoryTwoColor, 0xFF000000);
        mCategoryThreeColor = a.getColor(R.styleable.PercentageView_categoryThreeColor, 0xFF000000);

        mBackgroundPaint.setColor(mDefaultColor);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mDefaultColor);

        Paint paint = new Paint();
        Rect rect = canvas.getClipBounds();

        int width1 =  (mCategoryOne * rect.width()) / (mCategoryOne + mCategoryTwo +
            mCategoryThree);
        Rect rect1 = new Rect(rect.left, rect.top, rect.left + width1, rect.bottom);
        paint.setColor(mCategoryOneColor);
        canvas.drawRect(rect1, paint);


    }
}
