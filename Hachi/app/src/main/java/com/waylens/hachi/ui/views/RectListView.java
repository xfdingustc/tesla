package com.waylens.hachi.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.orhanobut.logger.Logger;

import java.util.List;

/**
 * Created by Xiaofei on 2016/12/19.
 */

public class RectListView extends View {
    private static final String TAG = RectListView.class.getSimpleName();
    private Paint mRectPaint;
    private List<Rect> mRectList;
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
    }

    public void showRects(List<Rect> rectList, Rect canvasRect) {
        this.mRectList = rectList;
        this.mCanvasRect = canvasRect;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mRectList == null) {
            return;
        }

        float scaleX = (float)canvas.getWidth() / mCanvasRect.width();
        float scaleY = (float)canvas.getHeight() / mCanvasRect.height();

        for (Rect rect : mRectList) {
            Rect normalRect = new Rect();
            normalRect.top = (int)(rect.top * scaleY);
            normalRect.bottom = (int)(rect.bottom * scaleY);
            normalRect.left = (int)(rect.left * scaleX);
            normalRect.right = (int)(rect.right * scaleX);
            Logger.t(TAG).d("normal rect: " + normalRect.toString());
            canvas.drawRect(normalRect, mRectPaint);
        }
    }
}
