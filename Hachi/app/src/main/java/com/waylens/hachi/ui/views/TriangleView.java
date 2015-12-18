package com.waylens.hachi.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.R;

/**
 * Created by Richard on 12/17/15.
 */
public class TriangleView extends View {

    private Paint mPaint;
    private Path mPath;

    public TriangleView(Context context) {
        this(context, null, 0);
    }

    public TriangleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TriangleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TriangleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        int color;
        if (Build.VERSION.SDK_INT >= 23) {
            color = getResources().getColor(R.color.style_color_primary, null);
        } else {
            color = getResources().getColor(R.color.style_color_primary);
        }
        mPaint.setColor(color);
        mPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mPath.reset();
        mPath.moveTo(0, 0);
        mPath.lineTo(w/2, h);
        mPath.lineTo(w, 0);
        mPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
    }
}
