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
class MarkView extends View {
    Paint mPaintBackground;
    Rect mRect;
    Paint mPaintMark;
    int mMarkWidth;

    public MarkView(Context context, int markWidth) {
        super(context);
        mPaintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintBackground.setColor(Color.argb(0x80, 0xFF, 0xFF, 0xFF));
        mPaintMark = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintMark.setColor(Color.rgb(0x1e, 0x88, 0xe5));
        mRect = new Rect();
        mMarkWidth = markWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mRect.set(0, 0, getWidth(), getHeight());
        canvas.drawRect(mRect, mPaintBackground);
        mRect.set((getWidth() - mMarkWidth) / 2, 0, (getWidth() + mMarkWidth) / 2, getHeight());
        canvas.drawRect(mRect, mPaintMark);
    }
}
