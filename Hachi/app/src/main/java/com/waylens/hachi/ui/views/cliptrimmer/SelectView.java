package com.waylens.hachi.ui.views.cliptrimmer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * Created by Xiaofei on 2016/3/17.
 */
@SuppressLint("ViewConstructor")
class SelectView extends View {
    private Paint mPaintMark;
    private int mWidth = 1;
    Rect mRect;


    public void setWidth(int width) {
        mWidth = width;
        invalidate();
    }

    public SelectView(Context context) {
        super(context);
        mPaintMark = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintMark.setColor(Color.GREEN);
        mPaintMark.setAlpha(125);
        mRect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int halfScreenPosition = getWidth() / 2;
        if (mWidth >= 0) {
            mRect.set(halfScreenPosition - mWidth, 0, halfScreenPosition , getHeight());
        } else {
            mRect.set(halfScreenPosition, 0, halfScreenPosition - mWidth, getHeight());
        }

        canvas.drawRect(mRect, mPaintMark);
    }
}
