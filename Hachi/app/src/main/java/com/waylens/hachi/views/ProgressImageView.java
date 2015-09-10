package com.waylens.hachi.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.ElementStaticImage;
import com.waylens.hachi.utils.EventBus;

/**
 * Created by Xiaofei on 2015/9/10.
 */
public class ProgressImageView extends ElementView {
    private int mProgress = 0;

    public ProgressImageView(Context context, Element image) {
        super(context, image);
    }

    public void setProgress(int progress) {
        mProgress = progress;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect srcRect = new Rect(0, 0, mElement.getWidth() * mProgress / 100,
            mElement.getHeight());
        canvas.drawBitmap(mElement.getResource(getContext()), srcRect, srcRect, null);
    }

    private static int progress = 0;

    @Override
    public void onEvent() {
        progress++;
        setProgress(progress);
    }
}
