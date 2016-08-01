package com.waylens.hachi.ui.clips.player.multisegseekbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;


import com.xfdingustc.snipe.vdb.Clip;
import com.xfdingustc.snipe.vdb.ClipSetPos;

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
    private final float mBarHeight;
    private List<Clip> mClipList;
    private List<Line> mLineList;
    private float mDividerWidth;
    private final float mLength;
    private int mActiveIndex = -1;

    private Paint mActivePaint;
    private Paint mInactivePaint;
    private Paint mPregressPaint;
    private boolean mIsMulti = true;

    public Bar(Context context, float x, float y, float length, float barWeight, int barColor,
               float dividerWidth, int activeColor, int inActiveColor, int progressColor, boolean isMulti, List<Clip> clipList) {
        mLeftX = x;
        mRightX = x + length;
        mLength = length;
        mY = y;
        mBarHeight = barWeight;
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

        mPregressPaint = new Paint();
        mPregressPaint.setColor(progressColor);
        mPregressPaint.setStrokeWidth(barWeight);
        mPregressPaint.setAntiAlias(true);
    }

    public void draw(Canvas canvas, List<Clip> clipList, float thumbX) {

        if (clipList == null) {
            return;
        }

        mClipList = clipList;
        generateLineList();


        for (Line line : mLineList) {
            if (line.endX <= thumbX) {
                canvas.drawLine(line.startX, mY, line.endX, mY, mPregressPaint);
            } else if (line.startX > thumbX) {
                canvas.drawLine(line.startX, mY, line.endX, mY, mInactivePaint);
            } else {
                canvas.drawLine(line.startX, mY, thumbX, mY, mPregressPaint);
                canvas.drawLine(thumbX, mY, line.endX, mY, mInactivePaint);
            }

        }
    }

    public float setClipSetPos(ClipSetPos clipSetPos) {
        Line activeLine = mLineList.get(clipSetPos.getClipIndex());
        Clip activeClip = mClipList.get(clipSetPos.getClipIndex());
        long timeOffset = clipSetPos.getClipTimeMs() - activeClip.editInfo.selectedStartValue;
        float lineOffset = timeOffset * (activeLine.endX - activeLine.startX) / activeClip.editInfo.getSelectedLength();
        return activeLine.startX + lineOffset;
    }

    public void setClipSetList(ArrayList<Clip> clipList) {
        mClipList = clipList;
        generateLineList();
    }

    private void generateLineList() {
        mLineList = new ArrayList<>();
        float offset = mLeftX;

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

    public ClipSetPos getClipSetPos(float x) {
        for (int i = 0; i < mLineList.size(); i++) {
            Line line = mLineList.get(i);
            if (line.startX <= x && x <= line.endX) {
                Clip clip = mClipList.get(i);
                float offset = x - line.startX;
                long timeOffset = (long) (offset * clip.editInfo.getSelectedLength() / (line.endX - line.startX));
                return new ClipSetPos(i, clip.editInfo.selectedStartValue + timeOffset);
            }
        }

        for (int i = 0 ; i < mLineList.size() - 1; i++) {
            Line line = mLineList.get(i);
            Line nextLine = mLineList.get(i + 1);
            if (line.endX <= x && x <= nextLine.endX) {
                Clip clip = mClipList.get(i + 1);
                return new ClipSetPos(i + 1, clip.editInfo.selectedStartValue);
            }
        }
        return null;
    }

    public static class Line {
        float startX;
        float endX;
    }
}
