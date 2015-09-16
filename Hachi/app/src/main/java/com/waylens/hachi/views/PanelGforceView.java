package com.waylens.hachi.views;

import android.content.Context;

import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.PanelGforce;
import com.waylens.hachi.utils.EventBus;

import java.util.List;

/**
 * Created by Xiaofei on 2015/9/8.
 */
public class PanelGforceView extends PanelView {
    private static final String TAG = PanelGforceView.class.getSimpleName();
    private final PanelGforce mPanel;
    private final EventBus mEventBus;

    public PanelGforceView(Context context, PanelGforce panel, EventBus eventBus) {
        super(context, panel);
        this.mPanel = panel;
        this.mEventBus = eventBus;
        init();
    }

    private void init() {
        ContainerView.LayoutParams layoutParams = new ContainerView.LayoutParams(mPanel
            .getMarginTop(), mPanel.getMarginBottom(), mPanel.getMarginLeft(), mPanel.getMarginRight(),
            mPanel.getAlignment());
        setLayoutParams(layoutParams);
        addElementViews();
    }

    private void addElementViews() {
        List<Element> elementList = mPanel.getElementList();
        for (Element element : elementList) {
            ElementView elementView = null;
            switch (element.getType()) {
                case Element.ELEMENT_TYPE_STATIC_IMAGE:
                    elementView = new StaticImageView(getContext(), element);
                    break;
                case Element.ELEMENT_TYPE_PROGRESS_IMAGE:
                    elementView = new ProgressImageView(getContext(), element);
                    break;
                case Element.ElEMENT_TYPE_ROTATE_PROGRESS_IMAGE:
                    elementView = new RotateProgressImageView(getContext(), element);
                    break;
            }
            if (elementView != null) {
                mEventBus.register(elementView);
                addView(elementView);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }


}
