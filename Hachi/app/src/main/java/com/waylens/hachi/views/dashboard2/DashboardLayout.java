package com.waylens.hachi.views.dashboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.skin.Panel;
import com.waylens.hachi.skin.PanelGforce;
import com.waylens.hachi.skin.Skin;
import com.waylens.hachi.skin.SkinManager;

import java.util.List;

/**
 * Created by Xiaofei on 2015/11/20.
 */
public class DashboardLayout extends RelativeLayout {
    private static final String TAG = DashboardLayout.class.getSimpleName();

    private Skin mSkin = SkinManager.getManager().getSkin();

    public DashboardLayout(Context context) {
        this(context, null);
    }

    public DashboardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        addPanels();
    }

    private void addPanels() {
        List<Panel> panelList = mSkin.getPanels();
        for (Panel panel : panelList) {
            if (panel instanceof PanelGforce) {
                Logger.t(TAG).d("ADD Panels, alignment: " + panel.getAlignment());
                PanelLayout layout = new PanelLayout(getContext(), panel);
                LayoutParams params = LayoutParamUtils.createLayoutParam(panel);
                addView(layout, params);

            }
        }
    }


}
