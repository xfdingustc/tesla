package com.waylens.hachi.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.skin.Panel;
import com.waylens.hachi.skin.PanelGforce;
import com.waylens.hachi.skin.Skin;
import com.waylens.hachi.skin.SkinManager;

import java.util.List;

/**
 * Created by Xiaofei on 2015/9/6.
 */
public class DashboardView extends ViewGroup {
    private final static String TAG = DashboardView.class.getSimpleName();
    private Bitmap mBackground;

    private Skin mSkin = SkinManager.getManager().getSkin();



    public DashboardView(Context context) {
        this(context, null, 0);
    }

    public DashboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DashboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }



    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DashboardView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();


        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int childHeight = child.getMeasuredHeight();
            final int childWidth = child.getMeasuredWidth();
            Logger.t(TAG).d("childWidth:" + childWidth + " childHeight: " + childHeight);
            child.layout(l, t, l + childWidth, t + childHeight);
        }
    }

    private void init() {
        addPanels();
    }

    private void addPanels() {
        List<Panel> panelList = mSkin.getPanels();
        for (Panel panel : panelList) {
            if (panel instanceof PanelGforce) {
                PanelGforce gforcePanel = (PanelGforce)panel;
                PanelGforceView panelGforceView = new PanelGforceView(getContext(), gforcePanel);
                addView(panelGforceView);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //drawDashboard();
        //addPanels();
    }

}
