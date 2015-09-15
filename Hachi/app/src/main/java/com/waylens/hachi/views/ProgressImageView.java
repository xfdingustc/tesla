package com.waylens.hachi.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;

import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.ElementStaticImage;
import com.waylens.hachi.utils.EventBus;

/**
 * Created by Xiaofei on 2015/9/10.
 */
public class ProgressImageView extends ElementView {
    private static final int PROGRESS_MAX = 100;
    private int mProgress = 0;

    public ProgressImageView(Context context, Element image) {
        super(context, image);
    }

    public void setProgress(int progress) {
        mProgress = progress;
        if (mProgress > PROGRESS_MAX) {
            mProgress = PROGRESS_MAX;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect srcRect = new Rect(0, 0, mElement.getWidth() * mProgress / PROGRESS_MAX,
            mElement.getHeight());
        canvas.drawBitmap(mElement.getResource(getContext()), srcRect, srcRect, null);
    }

    @Override
    public void onEvent(Object data) {
        Integer progress = (Integer)data;
        setProgress(progress);
    }
}
