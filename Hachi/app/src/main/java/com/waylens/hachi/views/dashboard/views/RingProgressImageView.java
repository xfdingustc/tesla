package com.waylens.hachi.views.dashboard.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.waylens.hachi.views.dashboard.models.Element;

/**
 * Created by Xiaofei on 2015/12/18.
 */
public class RingProgressImageView extends ProgressImageView {
    private int mStartRadius = 0;
    private int mEndRadius = 0;

    public RingProgressImageView(Context context, Element element) {
        super(context, element);
        mStartRadius = Integer.parseInt(element.getAttribute(Element.ATTRIBUTE_START_RADIUS));
        mEndRadius = Integer.parseInt(element.getAttribute(Element.ATTRIBUTE_END_RADIUS));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int drawRadius = mStartRadius +
            (int) ((mEndRadius - mStartRadius) * mProgress / mProgressMax);
        int left = (mElement.getWidth() - drawRadius * 2) / 2;
        int top = (mElement.getHeight() - drawRadius * 2) / 2;

        Rect dstRect = new Rect(left, top, left + drawRadius * 2, top + drawRadius * 2);
        canvas.drawBitmap(mElement.getResource(), null, dstRect, null);
    }
}
