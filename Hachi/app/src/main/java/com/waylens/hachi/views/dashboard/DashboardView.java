package com.waylens.hachi.views.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.skin.Panel;
import com.waylens.hachi.skin.PanelGforce;
import com.waylens.hachi.skin.Skin;
import com.waylens.hachi.skin.SkinManager;
import com.waylens.hachi.utils.EventBus;
import com.waylens.hachi.vdb.AccData;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;
import com.waylens.mediatranscoder.engine.OverlayProvider;

import java.util.List;

/**
 * Created by Xiaofei on 2015/9/6.
 */
public class DashboardView extends ContainerView implements OverlayProvider {
    private final static String TAG = DashboardView.class.getSimpleName();

    public static final String GFORCE_LEFT = "GforceLeft";
    public static final String GFORCE_RIGHT = "GforceRight";
    public static final String RPM = "RPM";
    public static final String MPH = "MPH";

    private Skin mSkin = SkinManager.getManager().getSkin();

    private EventBus mEventBus = new EventBus();

    private Adapter mAdapter = null;

    public DashboardView(Context context) {
        this(context, null);
    }

    public DashboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public void setAdapter(Adapter adapter) {
        this.mAdapter = adapter;
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
                setRawData(DashboardView.GFORCE_LEFT, (float) accData.accX * 5);
                setRawData(DashboardView.GFORCE_RIGHT, (float) accData.accY * 5);
            }
        }
    }

    private void updateGpsData(long pts) {

    }
    private void updateObdData(long pts) {
        if (mAdapter != null) {
            RawDataItem item = mAdapter.getObdDataItem(pts);
            if (item != null) {
                OBDData obdData = (OBDData) item.object;
                setRawData(DashboardView.RPM, (float) obdData.rpm / 1000);
                setRawData(DashboardView.MPH, obdData.speed);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }


    public void setRawData(String key, int value) {
        mEventBus.postEvent(key, value);
    }

    public void setRawData(String key, float value) {
        mEventBus.postEvent(key, value);
    }

    @Override
    public Bitmap updateTexImage(long pts) {
//        if (mListener != null) {
//            mListener.update(pts);
//        }
        Bitmap bitmap = getDrawingCache();
        return bitmap;
    }

    private void init() {
        setDrawingCacheEnabled(true);
        addPanels();
    }

    private void addPanels() {
        List<Panel> panelList = mSkin.getPanels();
        for (Panel panel : panelList) {
            if (panel instanceof PanelGforce) {
                //Logger.t(TAG).d("Add Panel: " + panel.toString());
                PanelGforce gforcePanel = (PanelGforce)panel;
                PanelGforceView panelGforceView = new PanelGforceView(getContext(), gforcePanel,
                    mEventBus);
                addView(panelGforceView);

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
