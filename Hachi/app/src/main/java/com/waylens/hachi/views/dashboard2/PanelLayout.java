package com.waylens.hachi.views.dashboard2;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.views.MapView;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.skin.Element;
import com.waylens.hachi.skin.Panel;
import com.waylens.hachi.utils.ViewUtils;

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
            case Element.ELEMENT_TYPE_MAP:
                elementView = new MapView(getContext(), Constants.MAP_BOX_ACCESS_TOKEN);
                ((MapView)elementView).setStyleUrl(Style.DARK);
                ((MapView)elementView).setZoomLevel(14);
                ((MapView)elementView).setLogoVisibility(View.GONE);
                ((MapView)elementView).onCreate(null);
                elementView.setBackgroundColor(Color.CYAN);
                break;
        }

        if (elementView != null) {
            elementView.setRotation(element.getRotation());
            LayoutParams params = LayoutParamUtils.createLayoutParam(element);
            addView(elementView, params);
        }
    }
}
