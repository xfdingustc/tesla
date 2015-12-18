package com.waylens.hachi.views.dashboard.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.waylens.hachi.views.dashboard.models.Element;
import com.waylens.hachi.views.dashboard.models.ElementProgressImage;

/**
 * Created by Xiaofei on 2015/12/18.
 */
public class ProgressImageView extends ElementView {
    private float mProgressMax;
    private float mProgress = 0;

    private int mStyle;

    public ProgressImageView(Context context, Element element) {
        super(context, element);
        mProgressMax = ((ElementProgressImage) element).getMax();
        mStyle = ((ElementProgressImage) element).getStyle();
    }

    public void setProgress(float progress) {
        mProgress = progress;
        if (mProgress > mProgressMax) {
            mProgress = mProgressMax;
        }
        invalidate();
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
        switch (mStyle) {
            case ElementProgressImage.PROGRESS_IMAGE_STYLE_DEFAULT:
                onDrawDefaultProgressImage(canvas);
                break;
            case ElementProgressImage.PROGRESS_IMAGE_STYLE_RING:
                onDrawRingProgressImage(canvas);
                break;
        }



    }



    private void onDrawDefaultProgressImage(Canvas canvas) {
        Rect srcRect = new Rect(0, 0, (int) (mElement.getWidth() * mProgress / mProgressMax),
            mElement.getHeight());
        canvas.drawBitmap(mElement.getResource(), srcRect, srcRect, null);
    }

    private void onDrawRingProgressImage(Canvas canvas) {
        Rect dstRect = new Rect(0, 0, mElement.getWidth(), mElement.getHeight());
        canvas.drawBitmap(mElement.getResource(), null, dstRect, null);

    }

    @Override
    public void onEvent(Object data) {
        Float progress = (Float) data;
        setProgress(progress);
    }
}

