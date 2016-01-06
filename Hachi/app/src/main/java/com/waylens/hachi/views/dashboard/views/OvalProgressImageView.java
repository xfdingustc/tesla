package com.waylens.hachi.views.dashboard.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;


import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.views.dashboard.models.Element;

/**
 * Created by Xiaofei on 2016/1/5.
 */
public class OvalProgressImageView extends ElementView {
    private static final String TAG = OvalProgressImageView.class.getSimpleName();

    private Bitmap mTexture = null;

    private Paint mTexturePaint = null;
    private float mAccX = 0.0f;
    private float mAccY = 0.0f;

    private int mStartRadius = 0;
    private int mEndRadius = 0;

    public OvalProgressImageView(Context context, Element element) {
        super(context, element);
        init();
    }

    private void init() {
        mTexture = mElement.getResource();
        mTexturePaint = new Paint();
        mTexturePaint.setColor(0xFFFFFF15);

        mStartRadius = Integer.parseInt(mElement.getAttribute(Element.ATTRIBUTE_START_RADIUS));
        mEndRadius = Integer.parseInt(mElement.getAttribute(Element.ATTRIBUTE_END_RADIUS));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //
        int width = mEndRadius * 2;
        int height = mEndRadius * 2;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float xOffset = mAccX * (mEndRadius - mStartRadius) / 1.0f;
        float yOffset = mAccY * (mEndRadius - mStartRadius) / 1.0f;



        RectF rectF = new RectF(mEndRadius - mStartRadius, mEndRadius - mStartRadius,
            mEndRadius + mStartRadius, mEndRadius + mStartRadius);
        if (xOffset >= 0) {
            rectF.right += xOffset;
        } else {
            rectF.left += xOffset;
        }

        if (yOffset >= 0) {
            rectF.top -= yOffset;
        } else {
            rectF.bottom -= yOffset;
        }

        canvas.drawOval(rectF, mTexturePaint);

    }

    @Override
    public void onEvent(Object value) {
        RawDataItem.AccData data = (RawDataItem.AccData)value;
        mAccX = (float)data.accX / 1000;
        mAccY = (float)data.accZ/ 1000;

        postInvalidate();
    }
}
