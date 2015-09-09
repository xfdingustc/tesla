package com.waylens.hachi.views;

import android.content.Context;
import android.view.ViewGroup;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.skin.PanelGforce;

/**
 * Created by Xiaofei on 2015/9/8.
 */
public class PanelGforceView extends ViewGroup {
    private static final String TAG = PanelGforceView.class.getSimpleName();
    private final PanelGforce mPanel;

    public PanelGforceView(Context context, PanelGforce panel) {
        super(context);
        this.mPanel = panel;
        Logger.t(TAG).d("Create gforce view");
        //LayoutParams params = new LayoutParams(128, 128);
        //setLayoutParams(params);
        setBackgroundColor(getResources().getColor(R.color.material_yellow_400));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mPanel.getWidth(), mPanel.getHeight());
        //super.onMeasure();
    }

    private String getMeasureSpec(int mode) {
        switch (mode) {
            case MeasureSpec.AT_MOST:
                return "At Most";
            case MeasureSpec.EXACTLY:
                return "Exactly";
            case MeasureSpec.UNSPECIFIED:
                return "Unspecified";

        }
        return null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Logger.t(TAG).d("onLayout l: " + l + " t: " + t + " r: " + r + " b: " + b);
        //layout(l, t, 128, 128);
    }
}
