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
import com.waylens.hachi.skin.Panel;
import com.waylens.hachi.skin.PanelGforce;
import com.waylens.hachi.skin.Skin;
import com.waylens.hachi.skin.SkinManager;
import com.waylens.hachi.vdb.AccData;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.views.dashboard.eventbus.EventBus;
import com.waylens.hachi.views.dashboard.eventbus.EventConstants;
import com.waylens.mediatranscoder.engine.OverlayProvider;

import java.util.List;

/**
 * Created by Xiaofei on 2015/11/20.
 */
public class DashboardLayout extends RelativeLayout implements OverlayProvider {
    private static final String TAG = DashboardLayout.class.getSimpleName();

    private Skin mSkin = SkinManager.getManager().getSkin();
    private Adapter mAdapter;

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

    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;
    }

    private MapView mMapView;

    private void addPanels() {
        List<Panel> panelList = mSkin.getPanels();
        for (Panel panel : panelList) {
            if (panel instanceof PanelGforce) {
                Logger.t(TAG).d("ADD Panels, alignment: " + panel.getAlignment());
                PanelLayout layout = new PanelLayout(getContext(), panel, mEventBus);
                LayoutParams params = LayoutParamUtils.createLayoutParam(panel);
                addView(layout, params);
                if (layout.getMapView() != null) {
                    mMapView = layout.getMapView();
                }
            }
        }
    }


    public void update(long pts) {
        updateAccData(pts);
        updateGpsData(pts);
        updateObdData(pts);
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
                //setRawData(DashboardView.RPM, (float) obdData.rpm / 1000);
                //setRawData(DashboardView.MPH, obdData.speed);
            }
        }
    }

    public static class Adapter {
        private RawDataBlock mAccDataBlock;
        private RawDataBlock mObdDataBlock;
        private RawDataBlock mGpsDataBlock;

        public Adapter() {

        }

        public void setAccDataBlock(RawDataBlock accDataBlock) {
            this.mAccDataBlock = accDataBlock;
        }

        public void setObdDataBlock(RawDataBlock obdDataBlock) {
            this.mObdDataBlock = obdDataBlock;
        }

        public void setGpsDataBlock(RawDataBlock gpsDataBlock) {
            this.mGpsDataBlock = gpsDataBlock;
        }

        public RawDataItem getAccDataItem(long pts) {
            return getRawDataItem(mAccDataBlock, pts);
        }

        public RawDataItem getObdDataItem(long pts) {
            return getRawDataItem(mObdDataBlock, pts);
        }

        public RawDataItem getGpsDataItem(long pts) {
            return getRawDataItem(mGpsDataBlock, pts);
        }

        // TODO: We need refine this algorithm:
        private RawDataItem getRawDataItem(RawDataBlock datablock, long pts) {
            if (datablock == null) {
                return null;
            }

            for (int i = 0; i < datablock.header.mNumItems; i++) {
                RawDataItem item = datablock.getRawDataItem(i);
                if (item.clipTimeMs > pts) {
                    return item;
                }
            }
            return null;
        }
    }


}