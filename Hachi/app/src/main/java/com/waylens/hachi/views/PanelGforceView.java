package com.waylens.hachi.views;

import android.content.Context;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.ElementStaticImage;
import com.waylens.hachi.skin.PanelGforce;

import java.util.List;

/**
 * Created by Xiaofei on 2015/9/8.
 */
public class PanelGforceView extends PanelView {
    private static final String TAG = PanelGforceView.class.getSimpleName();
    private final PanelGforce mPanel;

    public PanelGforceView(Context context, PanelGforce panel) {
        super(context, panel);
        this.mPanel = panel;
        init();
    }

    private void init() {
        ContainerView.LayoutParams layoutParams = new ContainerView.LayoutParams(mPanel
            .getMarginTop(), mPanel.getMarginBottom(), mPanel.getMarginLeft(), mPanel.getMarginRight(),
            mPanel.getAlignment());
        setLayoutParams(layoutParams);
        addElements();
    }

    private void addElements() {
        List<Element> elementList = mPanel.getElementList();
        for (Element element : elementList) {
            switch (element.getType()) {
                case Element.ELEMENT_TYPE_STATIC_IMAGE:
                    StaticImageView imageView = new StaticImageView(getContext(), element);
                    addView(imageView);
                    break;
                case Element.ELEMENT_TYPE_PROGRESS_IMAGE:
                    ProgressImageView progressImageView = new ProgressImageView(getContext(), element);
                    addView(progressImageView);
                    break;
            }
        }
    }



    /*
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mPanel.getWidth(), mPanel.getHeight());
        //super.onMeasure();
    }
    */


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        /*
        int count = getChildCount();
        Logger.t(TAG).d("l: " + l + " t: " + t + " r: " + r + " b: " + b);
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int childHeight = child.getMeasuredHeight();
            final int childWidth = child.getMeasuredWidth();
            child.layout(0, 0,  childWidth, childHeight);
        }*/
        super.onLayout(changed, l, t, r, b);
    }


}
