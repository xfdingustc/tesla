package com.waylens.hachi.ui.views.multisegseekbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.waylens.hachi.vdb.Clip;

import java.util.List;

/**
 * Created by Xiaofei on 2016/3/2.
 */
class Bar {
    private final Paint mBarPaint;
    private final float mLeftX;
    private final float mRightX;
    private final float mY;
    private final List<Clip> mClipList;
    private final float mDividerWidth;
    private final float mLength;
    private int mActiveIndex = 0;

    private Paint mActivePaint;
    private Paint mInactivePaint;

    public Bar(Context context, float x, float y, float length, float barWeight, int barColor,
               float dividerWidth, int activeColor, int inActiveColor, List<Clip> clipList) {
        mLeftX = x;
        mRightX = x + length;
        mLength = length;
        mY = y;
        mDividerWidth = dividerWidth;
        this.mClipList = clipList;

        mBarPaint = new Paint();
        mBarPaint.setColor(barColor);
        mBarPaint.setStrokeWidth(barWeight);
        mBarPaint.setAntiAlias(true);

        mActivePaint = new Paint();
        mActivePaint.setColor(activeColor);
        mActivePaint.setStrokeWidth(barWeight);
        mActivePaint.setAntiAlias(true);

        mInactivePaint = new Paint();
        mInactivePaint.setColor(inActiveColor);
        mInactivePaint.setStrokeWidth(barWeight);
        mInactivePaint.setAntiAlias(true);

    }

    public void draw(Canvas canvas) {
        //canvas.drawLine(mLeftX, mY, mRightX, mY, mBarPaint);

        long totalClipTimeMs = 0;

        for (Clip clip : mClipList) {
            totalClipTimeMs += clip.editInfo.getSelectedLength();
        }

        float widthScale = ((mLength - (mClipList.size() - 1) * mDividerWidth))
                / totalClipTimeMs;


        float left = mLeftX;
        float right = mLeftX;

        for (int i = 0; i < mClipList.size(); i++) {
            Clip clip = mClipList.get(i);

            right = left + widthScale * clip.editInfo.getSelectedLength();
            if (i == mActiveIndex) {
                canvas.drawLine(left, mY, right, mY, mActivePaint);
            } else {
                canvas.drawLine(left, mY, right, mY, mInactivePaint);
            }
            left = right + mDividerWidth;
        }
    }

    public void setActiveIndex(int activeIndex) {
        mActiveIndex = activeIndex;
    }

    public float getLeftX() {
        return mLeftX;
    }

    public float getRightX() {
        return mRightX;
    }

    public float getWidth() {
        return mRightX - mLeftX - (mClipList.size() - 1) * mDividerWidth;
    }
}
