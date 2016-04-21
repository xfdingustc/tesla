package com.waylens.hachi.ui.fragments.clipplay2;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipSegment;
import com.waylens.hachi.vdb.ClipPos;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.ClipSetPos;
import com.waylens.hachi.vdb.rawdata.GpsData;
import com.waylens.hachi.vdb.rawdata.IioData;
import com.waylens.hachi.vdb.rawdata.ObdData;
import com.waylens.hachi.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.vdb.rawdata.RawDataItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/3/8.
 */
public class RawDataLoader {
    private static final String TAG = RawDataLoader.class.getSimpleName();

    private final int mClipSetIndex;
    private final VdbRequestQueue mVdbRequestQueue;
    private OnLoadCompleteListener mListener;

    private List<RawDataBlockAll> mRawDataBlockList = new ArrayList<>();

    private int mCurrentLoadingIndex = 0;

    public RawDataLoader(int clipSetIndex, VdbRequestQueue requestQueue) {
        this.mClipSetIndex = clipSetIndex;
        this.mVdbRequestQueue = requestQueue;
    }


    public ClipSet getClipSet() {
        return ClipSetManager.getManager().getClipSet(mClipSetIndex);
    }

    public void startLoad(OnLoadCompleteListener listener) {
        this.mListener = listener;
        loadRawData();
    }


    private void loadRawData() {
        mCurrentLoadingIndex = 0;
        for (int i = 0; i < getClipSet().getCount(); i++) {
            RawDataBlockAll rawDataBlockAll = new RawDataBlockAll();
            mRawDataBlockList.add(rawDataBlockAll);
        }

        loadRawData(RawDataItem.DATA_TYPE_OBD);
    }

    private void loadRawData(final int dataType) {
        if (getClipSet() == null || mVdbRequestQueue == null) {
            return;
        }


        Logger.t(TAG).d("clipset count: " + getClipSet().getCount() + " loading index: " + mCurrentLoadingIndex);
        Clip clip = getClipSet().getClip(mCurrentLoadingIndex);

        ClipSegment clipSegment = new ClipSegment(clip);
        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, clipSegment.getStartTimeMs());
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, clipSegment.getDurationMs());

        RawDataBlockRequest obdRequest = new RawDataBlockRequest(clipSegment.getClip().cid, params,
                new VdbResponse.Listener<RawDataBlock>() {
                    @Override
                    public void onResponse(RawDataBlock response) {
                        onLoadRawDataFinished(dataType, response);
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        onLoadRawDataFinished(dataType, null);
                    }
                });
        mVdbRequestQueue.add(obdRequest);
    }

    private void onLoadRawDataFinished(int dataType, RawDataBlock block) {
        RawDataBlockAll rawDataBlockAll = mRawDataBlockList.get(mCurrentLoadingIndex);

        Logger.t(TAG).d("Load finished type: " + dataType + " index: " + mCurrentLoadingIndex);

        switch (dataType) {
            case RawDataItem.DATA_TYPE_OBD:
                rawDataBlockAll.iioDataBlock = block;
                loadRawData(RawDataItem.DATA_TYPE_IIO);
                break;
            case RawDataItem.DATA_TYPE_IIO:
                rawDataBlockAll.accDataBlock = block;
                loadRawData(RawDataItem.DATA_TYPE_GPS);
                break;
            case RawDataItem.DATA_TYPE_GPS:
                rawDataBlockAll.gpsDataBlock = block;

                if (++mCurrentLoadingIndex == getClipSet().getCount()) {
                    if (mListener != null) {
                        Logger.t(TAG).d("load finished!!!!!");
                        mListener.onLoadComplete();
                    }
                } else {
                    loadRawData(RawDataItem.DATA_TYPE_OBD);
                }


                break;
        }
    }

    public List<RawDataItem> getRawDataItemList(ClipSetPos clipSetPos) {
        int clipIndex = clipSetPos.getClipIndex();
        RawDataBlockAll rawDataBlockAll = mRawDataBlockList.get(clipIndex);

        List<RawDataItem> itemList = new ArrayList<>();

        if (rawDataBlockAll.gpsDataBlock != null) {
            RawDataItem gpsItem = rawDataBlockAll.gpsDataBlock.getRawDataItemByTime(clipSetPos.getClipTimeMs());
            if (gpsItem != null) {
                itemList.add(gpsItem);
            }
        }

        if (rawDataBlockAll.iioDataBlock != null) {
            RawDataItem iioItem = rawDataBlockAll.iioDataBlock.getRawDataItemByTime(clipSetPos.getClipTimeMs());
            if (iioItem != null) {
                itemList.add(iioItem);
            }
        }

        if (rawDataBlockAll.accDataBlock != null) {
            RawDataItem accItem = rawDataBlockAll.accDataBlock.getRawDataItemByTime(clipSetPos.getClipTimeMs());
            if (accItem != null) {
                itemList.add(accItem);
            }
        }

        return itemList;
    }

    public interface OnLoadCompleteListener {
        void onLoadComplete();
    }

    private class RawDataBlockAll {
        private RawDataBlock accDataBlock = null;
        private RawDataBlock gpsDataBlock = null;
        private RawDataBlock iioDataBlock = null;
    }
}
