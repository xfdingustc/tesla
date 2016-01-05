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
import com.waylens.hachi.vdb.GPSRawData;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.hachi.views.dashboard.adapters.RawDataAdapter;
import com.waylens.hachi.views.dashboard.eventbus.EventBus;
import com.waylens.hachi.views.dashboard.eventbus.EventConstants;
import com.waylens.hachi.views.dashboard.models.Panel;
import com.waylens.hachi.views.dashboard.models.Skin;
import com.waylens.hachi.views.dashboard.models.SkinManager;
import com.waylens.mediatranscoder.engine.OverlayProvider;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Xiaofei on 2015/11/20.
 */
public class DashboardLayout extends RelativeLayout implements OverlayProvider {
    private static final String TAG = DashboardLayout.class.getSimpleName();

    private final DashboardLayoutDataOberserver mDataObserver = new DashboardLayoutDataOberserver();

    public static final int NORMAL_WIDTH = 1920;
    public static final int NORMAL_HEIGHT = 1080;

    private Skin mSkin = SkinManager.getManager().getSkin();
    private RawDataAdapter mAdapter;

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

    public void setAdapter(RawDataAdapter adapter) {
        mAdapter = adapter;
        mAdapter.registerAdapterDataObserver(mDataObserver);
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

    public void updateLive(RawDataItem item) {
        if (item == null || item.object == null) {
            return;
        }


        updateCurrentTime(System.currentTimeMillis());

        switch (item.dataType) {
            case RawDataBlock.RAW_DATA_GPS:
                GPSRawData gpsRawData = (GPSRawData) item.object;
                mEventBus.postEvent(EventConstants.EVENT_GPS, gpsRawData);
                break;
            case RawDataBlock.RAW_DATA_ACC:
                AccData accData = (AccData) item.object;
                mEventBus.postEvent(EventConstants.EVENT_ROLL, -accData.euler_roll / 1000);
                mEventBus.postEvent(EventConstants.EVENT_ROLL_NUM, String.valueOf(-accData.euler_roll / 1000));
                mEventBus.postEvent(EventConstants.EVENT_PITCH, -accData.euler_pitch / 1000);
                mEventBus.postEvent(EventConstants.EVENT_PITCH_NUM, String.valueOf(-accData.euler_pitch / 1000));
                mEventBus.postEvent(EventConstants.EVENT_ACC, accData);
                float accX = (float) accData.accX / 1000;
                float accZ = (float) accData.accZ / 1000;
                DecimalFormat format = new DecimalFormat("0.00");
                mEventBus.postEvent(EventConstants.EVENT_ACC_X, String.valueOf(format.format(accX)));
                mEventBus.postEvent(EventConstants.EVENT_ACC_Z, String.valueOf(format.format(accZ)));
                break;
            case RawDataBlock.RAW_DATA_ODB:
                OBDData obdData = (OBDData) item.object;
                mEventBus.postEvent(EventConstants.EVENT_RPM, (float) obdData.rpm);
                int rpm = obdData.rpm / 1000;
                mEventBus.postEvent(EventConstants.EVENT_RPM_NUMBER, String.valueOf(rpm));
                mEventBus.postEvent(EventConstants.EVENT_MPH, (float) obdData.speed);
                int mph = (int) obdData.speed;
                mEventBus.postEvent(EventConstants.EVENT_MPH_NUMBER, String.valueOf(mph));
        }
    }


    private void updateAccData(long pts) {
        if (mAdapter != null) {
            RawDataItem item = mAdapter.getAccDataItem(pts);
            if (item != null) {
                AccData accData = (AccData) item.object;
                //Logger.t(TAG).d("Update gforce left as: " + (float) accData.accX * 5);
                mEventBus.postEvent(EventConstants.EVENT_ROLL, -accData.euler_roll / 1000);
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
                int rpm = obdData.rpm / 1000;
                mEventBus.postEvent(EventConstants.EVENT_RPM_NUMBER, String.valueOf(rpm));
                mEventBus.postEvent(EventConstants.EVENT_MPH, (float) obdData.speed);
                int mph = (int) obdData.speed;
                mEventBus.postEvent(EventConstants.EVENT_MPH_NUMBER, String.valueOf(mph));

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

        SimpleDateFormat monthFormat = new SimpleDateFormat("mm");
        String month = monthFormat.format(pts);
        mEventBus.postEvent(EventConstants.EVENT_MONTH, month);

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
        String day = dayFormat.format(pts);
        mEventBus.postEvent(EventConstants.EVENT_DAY, day);
    }

    private class DashboardLayoutDataOberserver extends RawDataAdapter.AdapterDataObserver {
        @Override
        public void onDataChanged(RawDataItem dataItem) {
            updateLive(dataItem);
        }
    }


}
