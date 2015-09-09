package com.waylens.hachi.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.widget.ImageView;

import com.waylens.hachi.skin.ElementStaticImage;

/**
 * Created by Xiaofei on 2015/9/9.
 */
public class StaticImageView extends ElementView {
    private final ElementStaticImage mStaticImage;

    public StaticImageView(Context context, ElementStaticImage image) {
        super(context, image);
        this.mStaticImage = image;
    }




    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mStaticImage.getResource(getContext()), null, new Rect(0, 0,
            mStaticImage.getWidth(), mStaticImage.getHeight()), null);
    }
}
