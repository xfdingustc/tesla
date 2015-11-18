package com.waylens.hachi.views.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.waylens.hachi.skin.Element;

/**
 * Created by Xiaofei on 2015/9/9.
 */
public class StaticImageView extends ElementView {
    public StaticImageView(Context context, Element image) {
        super(context, image);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect dstRect = new Rect(0, 0, mElement.getWidth(), mElement.getHeight());
        canvas.drawBitmap(mElement.getResource(), null, dstRect, null);
    }
}
