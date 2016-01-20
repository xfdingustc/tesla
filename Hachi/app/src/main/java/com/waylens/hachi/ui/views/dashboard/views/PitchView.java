package com.waylens.hachi.ui.views.dashboard.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.waylens.hachi.ui.views.dashboard.models.Element;
import com.waylens.hachi.ui.views.dashboard.models.Font;
import com.waylens.hachi.ui.views.dashboard.models.SkinManager;

/**
 * Created by Xiaofei on 2016/1/4.
 */
public class PitchView extends ElementView {
    private static final String TAG = PitchView.class.getSimpleName();
    private static final String TAG_PITCH_TEXTURE = "pitch_texture";
    private static final String TAG_COLOR = "Color";
    private final Font mFont;

    private int mPitch;
    private int mColor;
    private Paint mTexturePaint = null;

    private Bitmap mPitchTexture = null;

    public PitchView(Context context, Element element) {
        super(context, element);
        String fontName = mElement.getAttribute(Element.ATTRIBUTE_FONT);
        this.mFont = SkinManager.getManager().getSkin().getFont(fontName);

        this.mColor = Integer.valueOf(mElement.getAttribute(Element.ATTRIBUTE_COLOR), 16);

        this.mTexturePaint = new Paint();
        mTexturePaint.setColor(mColor);

        mPitchTexture = mFont.getFontResource(TAG_PITCH_TEXTURE);
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int top  = mPitchTexture.getHeight() - (mPitch + 180) * mPitchTexture.getHeight() / 360;
        Rect dstRect = new Rect(0, top, mPitchTexture.getWidth(),  mPitchTexture.getHeight());
        canvas.drawBitmap(mPitchTexture, dstRect, dstRect, null);

    }


    @Override
    public void onEvent(Object value) {
        mPitch = (int)value;
//        Logger.t(TAG).d("mPitch: " + mPitch);
        postInvalidate();
    }
}
