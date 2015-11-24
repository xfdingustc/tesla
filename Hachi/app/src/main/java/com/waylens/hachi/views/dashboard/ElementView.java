package com.waylens.hachi.views.dashboard;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.waylens.hachi.skin.Element;
import com.waylens.hachi.views.dashboard.PanelView;

/**
 * Created by Xiaofei on 2015/9/9.
 */
public class ElementView extends View {
    protected final Element mElement;

    public ElementView(Context context, Element element) {
        super(context);
        this.mElement = element;
        init();
    }

    public String getSubscribe() {
        return mElement.getSubscribe();
    }



    private void init() {
        PanelView.LayoutParams layoutParams = new PanelView.LayoutParams(mElement.getMarginTop(),
            mElement.getMarginBottom(), mElement.getMarginLeft(), mElement.getMarginRight(),
            mElement.getAlignment());
        setLayoutParams(layoutParams);

        if (Math.abs(mElement.getXCoord()) >= 0.01) {
            setPivotX(mElement.getXCoord());
        }
        if (Math.abs(mElement.getYCoord()) >= 0.01) {
            setPivotY(mElement.getYCoord());
        }
        setRotation(mElement.getRotation());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mElement.getWidth(), mElement.getHeight());
    }

    public void onEvent(Object data) {

    }
}
