package com.waylens.hachi.views.dashboard;

import android.content.Context;

import com.waylens.hachi.skin.Panel;
import com.waylens.hachi.views.dashboard.ContainerView;

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
