package com.waylens.hachi.views.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.ElementNumber;
import com.waylens.hachi.skin.Font;
import com.waylens.hachi.skin.SkinManager;
import com.waylens.hachi.views.dashboard.ElementView;

/**
 * Created by Xiaofei on 2015/9/17.
 */
public class NumberView extends ElementView {
    private final ElementNumber mElementNumber;
    private int mValue = 0;

    private Font mFont;

    public NumberView(Context context, Element element) {
        super(context, element);
        this.mElementNumber = (ElementNumber) element;
        this.mFont = SkinManager.getManager().getSkin().getFont(mElementNumber.getFontName());
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int height = 0;
        String valueString = String.valueOf(mValue);
        for (int i = 0; i < valueString.length(); i++) {
            char number = valueString.charAt(i);
            int numberInt = Character.digit(number, 10);
            Bitmap numberResource = mFont.getNumberResource(numberInt);
            width += numberResource.getWidth();
            height = numberResource.getHeight();
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int widthOffset = 0;
        String valueString = String.valueOf(mValue);
        for (int i = 0; i < valueString.length(); i++) {
            char number = valueString.charAt(i);
            int numberInt = Character.digit(number, 10);
            Bitmap numberResource = mFont.getNumberResource(numberInt);
            Rect dstRect = new Rect(widthOffset, 0, widthOffset + numberResource.getWidth(),
                numberResource.getHeight());
            canvas.drawBitmap(numberResource, null, dstRect, null);
            widthOffset += numberResource.getWidth();
        }
    }


    public void setValue(int value) {
        mValue = value;
        requestLayout();
        invalidate();
    }

    @Override
    public void onEvent(Object data) {
        Integer value = (Integer) data;
        setValue(value);
    }
}
