package com.waylens.hachi.ui.clips.player;

import android.os.Bundle;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.library.vdb.Clip;
import com.waylens.hachi.library.vdb.ClipSegment;
import com.waylens.hachi.library.vdb.ClipSet;
import com.waylens.hachi.library.vdb.ClipSetManager;
import com.waylens.hachi.library.vdb.ClipSetPos;
import com.waylens.hachi.library.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.library.vdb.rawdata.RawDataItem;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/3/8.
 */
public class RawDataLoader {
    private static final String TAG = RawDataLoader.class.getSimpleName();

    private final int mClipSetIndex;
    private final VdbRequestQueue mVdbRequestQueue;

    private List<RawDataBlockAll> mRawDataBlockList = new ArrayList<>();


    public RawDataLoader(int clipSetIndex, VdbRequestQueue requestQueue) {
        this.mClipSetIndex = clipSetIndex;
        this.mVdbRequestQueue = requestQueue;
    }


    public ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mClipSetIndex);
    }



    public void loadRawData() {
        for (int i = 0; i < getClipSet().getCount(); i++) {
            RawDataBlockAll rawDataBlockAll = new RawDataBlockAll();


            Clip clip = getClipSet().getClip(i);
            rawDataBlockAll.obdDataBlock = loadRawData(clip, RawDataItem.DATA_TYPE_OBD);
            rawDataBlockAll.gpsDataBlock = loadRawData(clip, RawDataItem.DATA_TYPE_GPS);
            rawDataBlockAll.iioDataBlock = loadRawData(clip, RawDataItem.DATA_TYPE_IIO);

            mRawDataBlockList.add(rawDataBlockAll);

        }
    }


    private RawDataBlock loadRawData(Clip clip, int dataType) {
        ClipSegment clipSegment = new ClipSegment(clip);
        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, clipSegment.getStartTimeMs());
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, clipSegment.getDurationMs());

        VdbRequestFuture<RawDataBlock> requestFuture = VdbRequestFuture.newFuture();
        RawDataBlockRequest request = new RawDataBlockRequest(clipSegment.getClip().cid, params, requestFuture, requestFuture);
        mVdbRequestQueue.add(request);
        try {
            RawDataBlock block = requestFuture.get();
            return block;
        } catch (Exception e) {
            Logger.t(TAG).e("Load raw data: " + dataType + " error");
            return null;
        }
    }

    public List<RawDataItem> getRawDataItemList(ClipSetPos clipSetPos) {

        if (clipSetPos == null) {
            return null;
        }
        int clipIndex = clipSetPos.getClipIndex();
        if (mRawDataBlockList == null || clipIndex >= mRawDataBlockList.size()) {
            return null;
        }
        RawDataBlockAll rawDataBlockAll = mRawDataBlockList.get(clipIndex);

        Clip clip = getClipSet().getClip(clipIndex);

        List<RawDataItem> itemList = new ArrayList<>();

        if (rawDataBlockAll.gpsDataBlock != null) {
            RawDataItem gpsItem = rawDataBlockAll.gpsDataBlock.getRawDataItemByTime(clipSetPos.getClipTimeMs());

            if (gpsItem != null) {
                gpsItem.setPtsMs(clip.getClipDate()  + gpsItem.getPtsMs());
                itemList.add(gpsItem);
            }
        }

        if (rawDataBlockAll.iioDataBlock != null) {
            RawDataItem iioItem = rawDataBlockAll.iioDataBlock.getRawDataItemByTime(clipSetPos.getClipTimeMs());
            if (iioItem != null) {
                iioItem.setPtsMs(clip.getClipDate()  + iioItem.getPtsMs());
                itemList.add(iioItem);
            }
        }

        if (rawDataBlockAll.obdDataBlock != null) {
            RawDataItem obdItem = rawDataBlockAll.obdDataBlock.getRawDataItemByTime(clipSetPos.getClipTimeMs());

            if (obdItem != null) {
                obdItem.setPtsMs(clip.getClipDate()  + obdItem.getPtsMs());
                itemList.add(obdItem);
            }
        }

        return itemList;
    }


    private class RawDataBlockAll {
        private RawDataBlock obdDataBlock = null;
        private RawDataBlock gpsDataBlock = null;
        private RawDataBlock iioDataBlock = null;
    }
}
