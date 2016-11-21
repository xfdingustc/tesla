package com.waylens.hachi.ui.clips.player;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetPos;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;
import com.waylens.hachi.view.gauge.GaugeView;

import java.util.List;

/**
 * Created by Xiaofei on 2016/11/21.
 */

public class ClipRawDataAdapter extends GaugeView.GaugeViewAdapter {
    private static final String TAG = ClipRawDataAdapter.class.getSimpleName();
    private final ClipSet mClipSet;
    private RawDataLoader mRawDataLoader;

    public ClipRawDataAdapter(ClipSet clipSet) {
        this.mClipSet = clipSet;
    }

    public void setRawDataLoader(RawDataLoader loader) {
        this.mRawDataLoader = loader;
        ClipSetPos clipSetPos = new ClipSetPos(0, mClipSet.getClip(0).editInfo.selectedStartValue);
        if (mRawDataLoader != null) {
            List<RawDataItem> rawDataItemList = mRawDataLoader.getRawDataItemList(clipSetPos);
            if (rawDataItemList != null && !rawDataItemList.isEmpty()) {
                notifyRawDataItemUpdated(rawDataItemList);
            }
        }
    }

    @Override
    public List<RawDataItem> getRawDataItemList(long pts) {
        return null;
    }

    public void setClipSetPos(ClipSetPos clipSetPos) {
        if (mRawDataLoader != null) {
            List<RawDataItem> rawDataItemList = mRawDataLoader.getRawDataItemList(clipSetPos);
            if (rawDataItemList != null && !rawDataItemList.isEmpty()) {
                notifyRawDataItemUpdated(rawDataItemList);
            }
        }
    }
}
