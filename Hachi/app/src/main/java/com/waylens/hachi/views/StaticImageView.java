package com.waylens.hachi.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.widget.ImageView;

import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.ElementStaticImage;
import com.waylens.hachi.utils.EventBus;

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
