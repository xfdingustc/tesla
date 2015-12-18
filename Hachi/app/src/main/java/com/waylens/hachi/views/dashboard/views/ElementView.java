package com.waylens.hachi.views.dashboard.views;

import android.content.Context;
import android.view.View;

import com.waylens.hachi.views.dashboard.models.Element;

/**
 * Created by Xiaofei on 2015/12/18.
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

        if (Math.abs(mElement.getXCoord()) >= 0.01) {
            setPivotX(mElement.getXCoord());
        }
        if (Math.abs(mElement.getYCoord()) >= 0.01) {
            setPivotY(mElement.getYCoord());
        }
    }

    public void onEvent(Object data) {

    }
}

