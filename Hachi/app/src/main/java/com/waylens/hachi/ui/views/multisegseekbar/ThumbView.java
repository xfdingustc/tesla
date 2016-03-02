package com.waylens.hachi.ui.views.multisegseekbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Xiaofei on 2016/3/1.
 */
class ThumbView extends View {
    private Resources mRes;
    private Paint mCirclePaint;
    private float mY;
    private float mX;
    private boolean mIsPressed;
    private float mCircleRadiusPx;

    public ThumbView(Context context) {
        super(context);
    }


    public void init(Context context, float y, float circleRadius, int circleColor) {
        mRes = context.getResources();

        mCircleRadiusPx = circleRadius;

        mCirclePaint = new Paint();
        mCirclePaint.setColor(circleColor);
        mCirclePaint.setAntiAlias(true);

        mY = y;
    }

    @Override
    public void setX(float x) {
        mX = x;
    }

    @Override
    public float getX() {
        return mX;
    }

    @Override
    public boolean isPressed() {
        return mIsPressed;
    }


    public void release() {
        mIsPressed = false;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(mX, mY, mCircleRadiusPx, mCirclePaint);
        super.draw(canvas);
    }
}
