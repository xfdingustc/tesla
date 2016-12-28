package com.waylens.hachi.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.utils.ViewUtils;

import java.util.List;

/**
 * Created by Xiaofei on 2016/12/19.
 */

public class RectListView extends View {
    private static final String TAG = RectListView.class.getSimpleName();
    private Paint mRectPaint;
    private Paint mLinePaint;
    private List<Rect> mRectList;
    private List<Line> mLineList;
    private Rect mCanvasRect;


    public RectListView(Context context) {
        this(context, null);
    }

    public RectListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mRectPaint = new Paint();
        mRectPaint.setColor(Color.RED);
        mRectPaint.setAntiAlias(true);
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(ViewUtils.dp2px(2));

        mLinePaint = new Paint();
        mLinePaint.setColor(Color.BLUE);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(ViewUtils.dp2px(2));

    }

    public void showRects(List<Rect> rectList, Rect canvasRect) {
        this.mRectList = rectList;
        this.mCanvasRect = canvasRect;
        invalidate();
    }

    public void showLines(List<Line> lineList, Rect canvasRect) {
        this.mLineList = lineList;
        this.mCanvasRect = canvasRect;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mRectList != null) {
            float scaleX = (float) canvas.getWidth() / mCanvasRect.width();
            float scaleY = (float) canvas.getHeight() / mCanvasRect.height();

            for (Rect rect : mRectList) {
                Rect normalRect = new Rect();
                normalRect.top = (int) (rect.top * scaleY);
                normalRect.bottom = (int) (rect.bottom * scaleY);
                normalRect.left = (int) (rect.left * scaleX);
                normalRect.right = (int) (rect.right * scaleX);
                Logger.t(TAG).d("normal rect: " + normalRect.toString());
                canvas.drawRect(normalRect, mRectPaint);
            }
        }

        if (mLineList != null) {
            float scaleX = (float) canvas.getWidth() / mCanvasRect.width();
            float scaleY = (float) canvas.getHeight() / mCanvasRect.height();

            for (Line rect : mLineList) {
                Line normalLine = new Line();
                normalLine.startX = (rect.startX * scaleX);
                normalLine.startY = (rect.startY * scaleY);
                normalLine.stopX = (rect.stopX * scaleX);
                normalLine.stopY = (rect.stopY * scaleY);
                //Logger.t(TAG).d("normal rect: " + normalRect.toString());
                canvas.drawLine(normalLine.startX, normalLine.startY, normalLine.stopX, normalLine.stopY, mLinePaint);
            }

        }
    }

    public static class Line {
        public float startX;
        public float startY;
        public float stopX;
        public float stopY;
    }
}
