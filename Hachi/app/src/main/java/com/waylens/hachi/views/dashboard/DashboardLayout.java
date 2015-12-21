package com.waylens.hachi.views.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.widget.RelativeLayout;

import com.mapbox.mapboxsdk.views.MapView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.vdb.AccData;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.views.dashboard.adapters.IRawDataAdapter;
import com.waylens.hachi.views.dashboard.eventbus.EventBus;
import com.waylens.hachi.views.dashboard.eventbus.EventConstants;
import com.waylens.hachi.views.dashboard.models.Panel;
import com.waylens.hachi.views.dashboard.models.Skin;
import com.waylens.hachi.views.dashboard.models.SkinManager;
import com.waylens.mediatranscoder.engine.OverlayProvider;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Xiaofei on 2015/11/20.
 */
public class DashboardLayout extends RelativeLayout implements OverlayProvider {
    private static final String TAG = DashboardLayout.class.getSimpleName();

    public static final int NORMAL_WIDTH = 1920;
    public static final int NORMAL_HEIGHT = 1080;

    private Skin mSkin = SkinManager.getManager().getSkin();
    private IRawDataAdapter mAdapter;

    private EventBus mEventBus = new EventBus();

    public DashboardLayout(Context context) {
        this(context, null);
    }

    public DashboardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public Bitmap updateTexImage(long pts) {
        Bitmap mapBitmap = null;
        TextureView mTextureView = null;
        if (mMapView != null) {
            for (int i = 0; i < mMapView.getChildCount(); i++) {
                View view = mMapView.getChildAt(i);
                if (view instanceof TextureView) {
                    Logger.t(TAG).d("Found TextureView");
                    mTextureView = (TextureView) view;
                    break;
                }
            }

            if (mTextureView != null) {
                mapBitmap = mTextureView.getBitmap();
            }
        }

        Bitmap bitmap = getDrawingCache();
        if (mapBitmap != null) {
            Canvas canvas = new Canvas(bitmap);
            Rect srcRect = new Rect(0, 0, mapBitmap.getWidth(), mapBitmap.getHeight());

            canvas.drawBitmap(mapBitmap, srcRect, srcRect, null);


        }
        return bitmap;
    }

    private void init() {
        setDrawingCacheEnabled(true);
        addPanels();
    }

    public void setAdapter(IRawDataAdapter adapter) {
        mAdapter = adapter;
    }

    private MapView mMapView;

    private void addPanels() {
        List<Panel> panelList = mSkin.getPanels();
        for (Panel panel : panelList) {
            Logger.t(TAG).d("ADD Panels, alignment: " + panel.getAlignment());
            PanelLayout layout = new PanelLayout(getContext(), panel, mEventBus);
            LayoutParams params = LayoutParamUtils.createLayoutParam(panel);
            addView(layout, params);
            if (layout.getMapView() != null) {
                mMapView = layout.getMapView();
            }
        }
    }


    public void update(long pts) {
        updateAccData(pts);
        updateGpsData(pts);
        updateObdData(pts);
        updateCurrentTime(pts);
    }



    private void updateAccData(long pts) {
        if (mAdapter != null) {
            RawDataItem item = mAdapter.getAccDataItem(pts);
            if (item != null) {
                AccData accData = (AccData) item.object;
                Logger.t(TAG).d("Update gforce left as: " + (float) accData.accX * 5);
                //setRawData(DashboardView.GFORCE_LEFT, (float) accData.accX * 5);
                //setRawData(DashboardView.GFORCE_RIGHT, (float) accData.accY * 5);
            }
        }
    }

    private void updateGpsData(long pts) {
        if (mAdapter != null) {
            RawDataItem item = mAdapter.getGpsDataItem(pts);
            if (item != null) {
                mEventBus.postEvent(EventConstants.EVENT_GPS, item.object);
            }
        }
    }

    private void updateObdData(long pts) {
        if (mAdapter != null) {
            RawDataItem item = mAdapter.getObdDataItem(pts);
            if (item != null) {
                OBDData obdData = (OBDData) item.object;
                mEventBus.postEvent(EventConstants.EVENT_RPM, (float) obdData.rpm);
                mEventBus.postEvent(EventConstants.EVENT_MPH, (float) obdData.speed);
                //setRawData(DashboardView.RPM, (float) obdData.rpm / 1000);
                //setRawData(DashboardView.MPH, obdData.speed);
            }
        }
    }

    private void updateCurrentTime(long pts) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        String date = format.format(pts);
        mEventBus.postEvent(EventConstants.EVENT_TIME, date);

        SimpleDateFormat ampmFormat = new SimpleDateFormat("a");
        String ampm = ampmFormat.format(pts);
        mEventBus.postEvent(EventConstants.EVENT_TIME_APMP, ampm);
    }


}
