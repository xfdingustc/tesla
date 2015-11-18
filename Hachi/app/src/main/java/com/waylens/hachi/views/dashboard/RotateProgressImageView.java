package com.waylens.hachi.views.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.ElementRotateProgressImage;
import com.waylens.hachi.views.dashboard.ProgressImageView;

/**
 * Created by Xiaofei on 2015/9/16.
 */
public class RotateProgressImageView extends ProgressImageView {
    private float mStartAngle;
    private float mEndAngle;

    public RotateProgressImageView(Context context, Element image) {
        super(context, image);
        mStartAngle = ((ElementRotateProgressImage) image).getStartAngle();
        mEndAngle = ((ElementRotateProgressImage) image).getEndAngle();
        setRotation(mStartAngle);
    }

    @Override
    public void setProgress(float progress) {

        float angle = mStartAngle + (mEndAngle - mStartAngle) * progress / getProgressMax();
        setRotation(angle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect srcRect = new Rect(0, 0, mElement.getWidth(), mElement.getHeight());
        canvas.drawBitmap(mElement.getResource(), srcRect, srcRect, null);
    }

    @Override
    public void onEvent(Object data) {
        Float progress = (Float) data;
        setProgress(progress);
    }
}
