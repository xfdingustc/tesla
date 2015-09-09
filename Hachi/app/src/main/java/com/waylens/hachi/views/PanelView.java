package com.waylens.hachi.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.waylens.hachi.skin.Panel;

/**
 * Created by Xiaofei on 2015/9/9.
 */
public abstract class PanelView extends ContainerView {
    private final Panel mPanel;

    public PanelView(Context context, Panel panel) {
        super(context);
        this.mPanel = panel;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        setMeasuredDimension(mPanel.getWidth(), mPanel.getHeight());
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


}
