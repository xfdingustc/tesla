package com.waylens.hachi.views.dashboard;

import android.content.Context;
import android.graphics.Outline;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.views.MapView;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Constants;
import com.waylens.hachi.views.dashboard.eventbus.EventBus;
import com.waylens.hachi.views.dashboard.models.Element;
import com.waylens.hachi.views.dashboard.models.Panel;
import com.waylens.hachi.views.dashboard.subscribers.MapViewEventSubscriber;
import com.waylens.hachi.views.dashboard.views.ElementView;
import com.waylens.hachi.views.dashboard.views.NumberView;
import com.waylens.hachi.views.dashboard.views.PitchView;
import com.waylens.hachi.views.dashboard.views.ProgressImageView;
import com.waylens.hachi.views.dashboard.views.RingProgressImageView;
import com.waylens.hachi.views.dashboard.views.RollView;
import com.waylens.hachi.views.dashboard.views.StringView;

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mMapView.setClipToOutline(true);
                    mMapView.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            outline.setOval(0, 0, view.getWidth(), view.getHeight());
                        }
                    });
                }


                break;
            case Element.ELEMENT_TYPE_PROGRESS_IMAGE:
                String style = element.getAttribute(Element.ATTRIBUTE_STYLE);
                if (style == null) {
                    elementView = new ProgressImageView(getContext(), element);
                } else if (style.equals(ProgressImageView.PROGRESS_IMAGE_STYLE_RING_STR)) {
                    elementView = new RingProgressImageView(getContext(), element);
                }


                break;
            case Element.ELEMENT_TYPE_NUMBER_VIEW:
                elementView = new NumberView(getContext(), element);
                break;
            case Element.ELEMENT_TYPE_STRING:
                elementView = new StringView(getContext(), element);
                break;
            case Element.ELEMENT_TYPE_ROLL:
                elementView = new RollView(getContext(), element);
                break;
            case Element.ELEMENT_TYPE_PITCH:
                elementView = new PitchView(getContext(), element);
                break;
        }

        if (elementView != null && element.getSubscribe() != null
            && (elementView instanceof ElementView)) {
            mEventBus.register((ElementView) elementView);
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
