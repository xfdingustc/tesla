package com.waylens.hachi.views.dashboard.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Handler;

import com.waylens.hachi.views.dashboard.models.Element;
import com.waylens.hachi.views.dashboard.models.Font;
import com.waylens.hachi.views.dashboard.models.SkinManager;

/**
 * Created by Xiaofei on 2015/12/21.
 */
public class RollView extends ElementView {
    private final Font mFont;
    private static final String TAG_ROLL_TEXTURE = "roll_texture";
    private static final String TAG_ROLL_SEPARATOR = "roll_separate";

    private float mRotation;

    private Bitmap mRollTexture = null;
    private Bitmap mRollSeparator = null;
    private Bitmap mRotatedSeparator = null;

    private Handler mHandler;

    public RollView(Context context, Element element) {
        super(context, element);
        String fontName = mElement.getAttribute(Element.ATTRIBUTE_FONT);
        this.mFont = SkinManager.getManager().getSkin().getFont(fontName);
        mRollTexture = mFont.getFontResource(TAG_ROLL_TEXTURE);
        mRollSeparator = mFont.getFontResource(TAG_ROLL_SEPARATOR);
        Matrix matrix = new Matrix();
        matrix.postRotate(180);
        mRotatedSeparator = Bitmap.createBitmap(mRollSeparator, 0, 0, mRollSeparator.getWidth(),
            mRollSeparator.getHeight(), matrix,true);

        mHandler = new Handler();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect dstRect = new Rect(0, mRollTexture.getHeight()/2, mRollTexture.getWidth(),
            mRollTexture.getHeight());
        canvas.drawBitmap(mRollTexture, dstRect, dstRect, null);

        // draw left separator:
        Rect separatorDstRect = new Rect(dstRect);
        separatorDstRect.top -= mRollSeparator.getHeight() / 2;
        separatorDstRect.right = mRollSeparator.getWidth();
        separatorDstRect.bottom = separatorDstRect.top + mRollSeparator.getHeight();


        canvas.drawBitmap(mRotatedSeparator, null, separatorDstRect, null);

        // draw right separator:
        separatorDstRect.right = dstRect.right;
        separatorDstRect.left = separatorDstRect.right - mRollSeparator.getWidth();

        canvas.drawBitmap(mRollSeparator, null, separatorDstRect, null);
    }

    @Override
    public void onEvent(Object value) {
        int rotation = (int)value;
        setRotation(rotation);
    }
}
