package com.waylens.hachi.views.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.skin.Panel;
import com.waylens.hachi.skin.PanelGforce;
import com.waylens.hachi.skin.Skin;
import com.waylens.hachi.skin.SkinManager;
import com.waylens.hachi.utils.EventBus;
import com.waylens.mediatranscoder.engine.OverlayProvider;

import java.util.List;

/**
 * Created by Xiaofei on 2015/9/6.
 */
public class DashboardView extends ContainerView implements OverlayProvider {
    private final static String TAG = DashboardView.class.getSimpleName();

    public static final String GFORCE_LEFT = "GforceLeft";
    public static final String GFORCE_RIGHT = "GforceRight";
    public static final String RPM = "RPM";
    public static final String MPH = "MPH";

    private Skin mSkin = SkinManager.getManager().getSkin();

    private EventBus mEventBus = new EventBus();



    public DashboardView(Context context) {
        this(context, null);
    }

    public DashboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }


    public void setRawData(String key, int value) {
        mEventBus.postEvent(key, value);
    }

    public void setRawData(String key, float value) {
        mEventBus.postEvent(key, value);
    }

    @Override
    public Bitmap updateTexImage(long pts) {
//        if (mListener != null) {
//            mListener.update(pts);
//        }
        Bitmap bitmap = getDrawingCache();
        return bitmap;
    }

    private void init() {
        setDrawingCacheEnabled(true);
        addPanels();
    }

    private void addPanels() {
        List<Panel> panelList = mSkin.getPanels();
        for (Panel panel : panelList) {
            if (panel instanceof PanelGforce) {
                //Logger.t(TAG).d("Add Panel: " + panel.toString());
                PanelGforce gforcePanel = (PanelGforce)panel;
                PanelGforceView panelGforceView = new PanelGforceView(getContext(), gforcePanel,
                    mEventBus);
                addView(panelGforceView);

            }
        }
    }

}
