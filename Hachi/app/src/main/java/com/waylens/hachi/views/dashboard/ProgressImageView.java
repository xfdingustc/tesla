package com.waylens.hachi.views.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.ElementProgressImage;
import com.waylens.hachi.views.dashboard.ElementView;

/**
 * Created by Xiaofei on 2015/9/10.
 */
public class ProgressImageView extends ElementView {
    private float mProgressMax;
    private float mProgress = 0;

    public ProgressImageView(Context context, Element image) {
        super(context, image);
        mProgressMax = ((ElementProgressImage) image).getMax();
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
