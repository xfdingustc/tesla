package com.waylens.hachi.ui.views.dashboard.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;

import com.waylens.hachi.ui.views.dashboard.models.Element;
import com.waylens.hachi.ui.views.dashboard.models.Font;
import com.waylens.hachi.ui.views.dashboard.models.SkinManager;

/**
 * Created by Xiaofei on 2015/12/21.
 */
public class StringView extends ElementView {
    private static final String TAG = StringView.class.getSimpleName();
    private Font mFont;
    private String mValue;

    private Handler mHandler;


    public StringView(Context context, Element element) {
        super(context, element);
        String fontName = mElement.getAttribute(Element.ATTRIBUTE_FONT);
        this.mFont = SkinManager.getManager().getSkin().getFont(fontName);
        mHandler = new Handler();
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        if (mFont.getCharNumber() != 1) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//            return;
//        } else {
//            int width = 0;
//            int height = 0;
//
//            if (mValue != null) {
//                width = mFont.getFontWidth() * mValue.length() / mFont.getCharNumber();
//                height = mFont.getFontHeight();
//            }
//
//            Logger.t(TAG).d("WWWWWWWWWWWWWWW: " + width + " HHHHHHHHHHHHHHH: " + height);
//
//
//            setMeasuredDimension(width, height);
//        }
//    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mValue == null) {
            mValue = "0";
        }
        int widthOffset = 0;

        if (mFont.getCharNumber() == 1) {
            // First calculate the left offset
            int stringWidth = 0;
            for (int i = 0; i < mValue.length(); i++) {
                char number = mValue.charAt(i);
                Bitmap resource;

                resource = mFont.getFontResource(String.valueOf(number));
                if (resource != null) {
                    stringWidth += resource.getWidth();
                }
            }

            widthOffset = (getMeasuredWidth() - stringWidth) / 2;

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
        } else {
            Bitmap resource = mFont.getFontResource(mValue);
            if (resource != null) {
                Rect dstRect = new Rect(widthOffset, 0, widthOffset + resource.getWidth(),
                    resource.getHeight());
                canvas.drawBitmap(resource, null, dstRect, null);

            }
        }

    }


    @Override
    public void onEvent(Object value) {
        if (mValue != null && mValue.equals(value)) {
            return;
        }
        mValue = (String) value;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });

    }
}
