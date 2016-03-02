package com.waylens.hachi.ui.views.multisegseekbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * Created by Xiaofei on 2016/3/1.
 */
class ThumbView extends View {
    private Resources mRes;
    private Paint mCirclePaint;
    private float mY;
    private float mX;
    private boolean mIsPressed = false;
    private float mCircleRadiusPx;
    private float mTargetRadiusPx;

    private static final float MINIMUM_TARGET_RADIUS_DP = 48;

    public ThumbView(Context context) {
        super(context);
    }


    public void init(Context context, float y, float circleRadius, int circleColor) {
        mRes = context.getResources();

        mCircleRadiusPx = circleRadius;

        mCirclePaint = new Paint();
        mCirclePaint.setColor(circleColor);
        mCirclePaint.setAntiAlias(true);

        int targetRadius = (int) Math.max(MINIMUM_TARGET_RADIUS_DP, mCircleRadiusPx);
        mTargetRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                targetRadius,
                mRes.getDisplayMetrics());
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
        canvas.drawCircle(mX + mCircleRadiusPx, mY, mCircleRadiusPx, mCirclePaint);
        super.draw(canvas);
    }

    public boolean isInTargetZone(float x, float y) {
        return (Math.abs(x - mX) <= mTargetRadiusPx && Math.abs(y - mY) <= mTargetRadiusPx);
    }

    public void press() {
        mIsPressed = true;

    }
}
