package com.waylens.hachi.ui.views.multisegseekbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/3/2.
 */
class Bar {
    private final Paint mBarPaint;
    private final float mLeftX;
    private final float mRightX;
    private final float mY;
    private List<Clip> mClipList;
    private List<Line> mLineList;
    private float mDividerWidth;
    private final float mLength;
    private int mActiveIndex = -1;

    private Paint mActivePaint;
    private Paint mInactivePaint;
    private boolean mIsMulti = true;

    public Bar(Context context, float x, float y, float length, float barWeight, int barColor,
               float dividerWidth, int activeColor, int inActiveColor, boolean isMulti, List<Clip> clipList) {
        mLeftX = x;
        mRightX = x + length;
        mLength = length;
        mY = y;
        mDividerWidth = dividerWidth;
        this.mClipList = clipList;
        mIsMulti = isMulti;
        if (mIsMulti == false) {
            mDividerWidth = 0;
        }

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

    public void draw(Canvas canvas, List<Clip> clipList) {

        if (clipList == null) {
            return;
        }

        generateLineList();

        mClipList = clipList;

        for (Line line : mLineList) {
            canvas.drawLine(line.startX, mY, line.endX, mY, mInactivePaint);
        }
    }

    public void setMultiStyle(boolean isMulti) {
        mIsMulti = isMulti;
        if (mIsMulti == false) {
            mDividerWidth = 0;
        }
    }

    private void generateLineList() {
        mLineList = new ArrayList<>();
        float offset = 0;

        long totalClipTimeMs = 0;
        for (Clip clip : mClipList) {
            totalClipTimeMs += clip.editInfo.getSelectedLength();
        }

        float widthScale = ((mLength - (mClipList.size() - 1) * mDividerWidth)) / totalClipTimeMs;

        for (Clip clip : mClipList) {
            Line line = new Line();
            line.startX = offset;
            line.endX = line.startX + widthScale * clip.editInfo.getSelectedLength();

            offset = line.endX + mDividerWidth;
            mLineList.add(line);
        }
    }

    public void setActiveIndex(int activeIndex) {
        mActiveIndex = activeIndex;
    }

    int getActiveIndex() {
        return mActiveIndex;
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

    public ClipPos getClipPos(float x) {

        return null;
    }

    public static class Line {
        float startX;
        float endX;
    }
}
