package com.waylens.hachi.ui.clips.player.multisegseekbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;

import com.waylens.hachi.R;

/**
 * Created by Xiaofei on 2016/3/1.
 */
class ThumbView extends View {
    private Resources mRes;
    private Paint mCirclePaint;
    private Paint mOutCirclePaint;
    private float mY;
    private float mX;
    private boolean mIsPressed = false;
    private float mCircleRadiusPx;

    private float mTargetRadiusPx;

    private static final float MINIMUM_TARGET_RADIUS_DP = 48;
    private float mLeftMin;
    private float mRightMax;


    public ThumbView(Context context) {
        super(context);
    }


    public void init(Context context, float y, float circleRadius, int circleColor, float left, float right) {
        mRes = context.getResources();

        mCircleRadiusPx = circleRadius;

        mLeftMin = left;
        mRightMax = right;

        mCirclePaint = new Paint();
        mCirclePaint.setColor(circleColor);
        mCirclePaint.setAntiAlias(true);

        mOutCirclePaint = new Paint();
//        mOutCirclePaint.setColor(getResources().getColor(R.color.material_grey_700));
        mOutCirclePaint.setColor(Color.DKGRAY);
        mOutCirclePaint.setAntiAlias(true);

        int targetRadius = (int) Math.max(MINIMUM_TARGET_RADIUS_DP, mCircleRadiusPx);
        mTargetRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, targetRadius, mRes.getDisplayMetrics());
        mY = y;
    }

    @Override
    public void setX(float x) {
        mX = x;
        mX = Math.max(mLeftMin, mX);
        mX = Math.min(mRightMax, mX);
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
        canvas.drawCircle(mX, mY, mCircleRadiusPx * 2, mOutCirclePaint);
        canvas.drawCircle(mX, mY, mCircleRadiusPx, mCirclePaint);
        super.draw(canvas);
    }

    public boolean isInTargetZone(float x, float y) {
        return (Math.abs(x - mX) <= mTargetRadiusPx && Math.abs(y - mY) <= mTargetRadiusPx);
    }

    public void press() {
        mIsPressed = true;

    }
}
