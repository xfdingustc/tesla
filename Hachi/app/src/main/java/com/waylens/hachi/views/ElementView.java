package com.waylens.hachi.views;

import android.content.Context;
import android.view.View;

import com.waylens.hachi.skin.Element;

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

    private void init() {
        PanelView.LayoutParams layoutParams = new PanelView.LayoutParams(mElement.getMarginTop(),
            mElement.getMarginBottom(), mElement.getMarginLeft(), mElement.getMarginRight(),
            mElement.getAlignment());
        setLayoutParams(layoutParams);
        setRotation(mElement.getRotation());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mElement.getWidth(), mElement.getHeight());
    }
}
