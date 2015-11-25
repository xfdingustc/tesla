package com.waylens.hachi.views.dashboard2;

import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.skin.Element;
import com.waylens.hachi.views.dashboard.ContainerLayouts;

/**
 * Created by Xiaofei on 2015/11/24.
 */
public class LayoutParamUtils implements ContainerLayouts {
    private static final String TAG = LayoutParamUtils.class.getSimpleName();

    public static RelativeLayout.LayoutParams createLayoutParam(Element element) {
        Logger.t(TAG).d("Element alignment: " + element.getAlignment() + " size: " + element
            .getWidth() + " X " + element.getHeight() + " type: " + element.getType());



        int widthMeasureSpec = LayoutParamUtils.getSizeMeasureSpec(element.getWidthSizeMode());
        int heightMeasureSpec = LayoutParamUtils.getSizeMeasureSpec(element.getHeightSizeMode());

        if (widthMeasureSpec == 0) {
            widthMeasureSpec = element.getWidth();
        }

        if (heightMeasureSpec == 0) {
            heightMeasureSpec = element.getHeight();
        }

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams
            (widthMeasureSpec, heightMeasureSpec);

        switch (element.getAlignment()) {
            case TOP_LEFT:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
                break;
            case TOP_CENTER:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, 1);
                break;
            case TOP_RIGHT:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
                break;
            case CENTER_LEFT:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
                layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, 1);
                break;
            case CENTER:
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
                break;
            case CENTER_RIGHT:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
                layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, 1);
                break;
            case BOTTOM_LEFT:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
                break;
            case BOTTOM_CENTER:
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, 1);
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

    private static int getSizeMeasureSpec(int elementSizeMode) {
        switch (elementSizeMode) {
            case Element.SIZE_MODE_MATCH_PARENT:
                return ViewGroup.LayoutParams.MATCH_PARENT;
            case Element.SIZE_MODE_WRAP_CONTENT:
                return ViewGroup.LayoutParams.WRAP_CONTENT;
            default:
                return 0;

        }
    }
}
