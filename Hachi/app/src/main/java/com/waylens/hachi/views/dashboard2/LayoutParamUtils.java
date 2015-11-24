package com.waylens.hachi.views.dashboard2;

import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.waylens.hachi.skin.Element;
import com.waylens.hachi.views.dashboard.ContainerLayouts;

/**
 * Created by Xiaofei on 2015/11/24.
 */
public class LayoutParamUtils implements ContainerLayouts {
    public static RelativeLayout.LayoutParams createLayoutParam(Element element) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(element.getWidth(),
            element.getHeight());
        switch (element.getAlignment()) {
            case TOP_LEFT:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
                break;
            case BOTTOM_LEFT:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
                break;
            case BOTTOM_RIGHT:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
                break;
            default:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
                break;
        }

        layoutParams.setMargins(element.getMarginLeft(), element.getMarginTop(), element
            .getMarginRight(), element.getMarginBottom());
        return layoutParams;
    }
}
