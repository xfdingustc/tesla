package com.waylens.hachi.views.dashboard.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;

import com.waylens.hachi.views.dashboard.models.Element;
import com.waylens.hachi.views.dashboard.models.Font;
import com.waylens.hachi.views.dashboard.models.SkinManager;

import java.text.SimpleDateFormat;

/**
 * Created by Xiaofei on 2015/12/21.
 */
public class StringView extends ElementView {
    private Font mFont;
    private String mValue;

    private Handler mHandler;



    public StringView(Context context, Element element) {
        super(context, element);
        String fontName = mElement.getAttribute(Element.ATTRIBUTE_FONT);
        this.mFont = SkinManager.getManager().getSkin().getFont(fontName);
        mHandler = new Handler();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int height = 0;

        Bitmap numberResource = mFont.getFontResource("0");
        width = numberResource.getWidth() * 6;
        width += mElement.getWidth() * 2;

        height = mElement.getHeight();
        setMeasuredDimension(width, height);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mValue == null) {
            return;
        }
        int widthOffset = 0;

        for (int i = 0; i < mValue.length(); i++) {
            char number = mValue.charAt(i);
            Bitmap resource;
            Rect dstRect;

            resource = mFont.getFontResource(String.valueOf(number));
            if (resource != null) {

                dstRect = new Rect(widthOffset, 0, widthOffset + resource.getWidth(),
                    resource.getHeight());
                canvas.drawBitmap(resource, null, dstRect, null);
                widthOffset += resource.getWidth();
            }
        }

    }


    @Override
    public void onEvent(Object value) {
        mValue = (String)value;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });

    }
}
