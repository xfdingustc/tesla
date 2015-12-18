package com.waylens.hachi.views.dashboard.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.waylens.hachi.views.dashboard.models.Element;

/**
 * Created by Xiaofei on 2015/12/18.
 */
public class ProgressImageView extends ElementView {
    private float mProgressMax;
    private float mProgress = 0;

    private static final String PROGRESS_IMAGE_STYLE_RING_STR = "Ring";

    private String mStyle = null;

    public ProgressImageView(Context context, Element element) {
        super(context, element);
        mProgressMax = Integer.parseInt(element.getAttribute(Element.ATTRIBUTE_MAX));
        mStyle = element.getAttribute(Element.ATTRIBUTE_STYLE);

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
        if (mStyle == null) {
            onDrawDefaultProgressImage(canvas);
        } else if (mStyle.equals(PROGRESS_IMAGE_STYLE_RING_STR)) {
            onDrawRingProgressImage(canvas);
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

