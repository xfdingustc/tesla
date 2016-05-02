package com.waylens.hachi.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.utils.ViewUtils;

/**
 * Created by Richard on 12/28/15.
 */
public class SharpView extends View {

    private Paint mPaint;
    private Path mPath;

    public SharpView(Context context) {
        this(context, null, 0);
    }

    public SharpView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SharpView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SharpView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(ViewUtils.dp2px(1));
        mPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPath.reset();
        int width = getWidth();
        float wStep = width / 3.0f;
        float height = getHeight();
        float hStep = height / 3.0f;
        mPath.moveTo(wStep, 0);
        mPath.lineTo(wStep, height);
        mPath.moveTo(2 * wStep, height);
        mPath.lineTo(2 * wStep, 0);
        mPath.moveTo(0, hStep);
        mPath.lineTo(width, hStep);
        mPath.moveTo(width, 2 * hStep);
        mPath.lineTo(0, 2 * hStep);
        canvas.drawPath(mPath, mPaint);

    }
}
