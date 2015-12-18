package com.waylens.hachi.views.dashboard.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.views.dashboard.eventbus.EventBus;
import com.waylens.hachi.views.dashboard.models.Element;

/**
 * Created by Xiaofei on 2015/12/18.
 */
public class ProgressImageView extends ElementView  {
    private static final String TAG = ProgressImageView.class.getSimpleName();
    protected float mProgressMax;
    protected float mProgress = 0;

    public static final String PROGRESS_IMAGE_STYLE_RING_STR = "Ring";

    private Handler mHandler;

    public ProgressImageView(Context context, Element element) {
        super(context, element);
        mProgressMax = Integer.parseInt(element.getAttribute(Element.ATTRIBUTE_MAX));
        mHandler = new Handler();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        int width, height;


        width = mElement.getWidth();

        height = mElement.getHeight();
        setMeasuredDimension(width, height);
    }


    public void setProgress(float progress) {
        mProgress = progress;
        if (mProgress > mProgressMax) {
            mProgress = mProgressMax;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });

    }

    public float getProgress() {
        return mProgress;
    }

    public float getProgressMax() {
        return mProgressMax;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect srcRect = new Rect(0, 0, (int) (mElement.getWidth() * mProgress / mProgressMax),
            mElement.getHeight());
        canvas.drawBitmap(mElement.getResource(), srcRect, srcRect, null);

    }


    @Override
    public void onEvent(Object data) {
        Float progress = (Float) data;
        setProgress(progress);
    }
}

