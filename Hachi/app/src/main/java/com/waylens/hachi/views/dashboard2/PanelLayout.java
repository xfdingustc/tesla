package com.waylens.hachi.views.dashboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.Panel;

import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by Xiaofei on 2015/11/20.
 */
public class PanelLayout extends RelativeLayout {
    private Panel mPanel;

    public PanelLayout(Context context, Panel panel) {
        this(context, null, panel);
    }

    public PanelLayout(Context context, AttributeSet attrs, Panel panel) {
        super(context, attrs);
        init(panel);
    }

    private void init(Panel panel) {
        this.mPanel = panel;
        addElements();
    }

    private void addElements() {
        List<Element> elementList = mPanel.getElementList();
        for (Element element : elementList) {
            addElementView(element);
        }
    }

    private void addElementView(Element element) {
        View elementView = null;
        switch (element.getType()) {
            case Element.ELEMENT_TYPE_STATIC_IMAGE:
                elementView = new ImageView(getContext());
                ((ImageView) elementView).setImageBitmap(element.getResource());
                break;
        }

        if (elementView != null) {
            LayoutParams params = LayoutParamUtils.createLayoutParam(element);
            addView(elementView, params);
        }
    }
}
