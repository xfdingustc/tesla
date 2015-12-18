package com.waylens.hachi.views.dashboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.views.MapView;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.views.dashboard.eventbus.EventBus;
import com.waylens.hachi.views.dashboard.models.Element;
import com.waylens.hachi.views.dashboard.models.Panel;
import com.waylens.hachi.views.dashboard.views.ProgressImageView;
import com.waylens.hachi.views.dashboard.views.RingProgressImageView;

import java.util.List;

/**
 * Created by Xiaofei on 2015/11/20.
 */
public class PanelLayout extends RelativeLayout {
    private static final String TAG = PanelLayout.class.getSimpleName();
    private Panel mPanel;
    private EventBus mEventBus;

    private MapView mMapView = null;


    public PanelLayout(Context context, Panel panel, EventBus eventBus) {
        this(context, null, panel, eventBus);
    }

    public PanelLayout(Context context, AttributeSet attrs, Panel panel, EventBus eventBus) {
        super(context, attrs);
        init(panel, eventBus);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void init(Panel panel, EventBus eventBus) {
        this.mPanel = panel;
        this.mEventBus = eventBus;
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
                ((MapView) elementView).setStyleUrl(Style.DARK);
                ((MapView) elementView).setZoomLevel(14);
                ((MapView) elementView).setLogoVisibility(View.GONE);
                ((MapView) elementView).onCreate(null);
                if (element.getSubscribe() != null) {
                    MapViewEventSubscriber mapViewEventSubscriber = new MapViewEventSubscriber(
                        (MapView) elementView);
                    mEventBus.register(mapViewEventSubscriber);
                }
                mMapView = (MapView) elementView;

                break;
            case Element.ELEMENT_TYPE_PROGRESS_IMAGE:
                String style = element.getAttribute(Element.ATTRIBUTE_STYLE);
                if (style == null) {
                    elementView = new ProgressImageView(getContext(), element);
                } else if(style.equals(ProgressImageView.PROGRESS_IMAGE_STYLE_RING_STR)){
                    elementView = new RingProgressImageView(getContext(), element);
                }
                break;
        }

        if (elementView != null) {
            elementView.setRotation(element.getRotation());
            LayoutParams params = LayoutParamUtils.createLayoutParam(element);
            addView(elementView, params);
        }
    }

    public MapView getMapView() {
        return mMapView;
    }
}
