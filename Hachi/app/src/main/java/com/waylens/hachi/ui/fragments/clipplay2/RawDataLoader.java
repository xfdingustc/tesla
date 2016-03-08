package com.waylens.hachi.ui.fragments.clipplay2;

import android.os.Bundle;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.ClipFragment;
import com.waylens.hachi.vdb.ClipSet;
import com.waylens.hachi.vdb.ClipSetManager;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

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

    protected static final int RAW_DATA_STATE_UNKNOWN = -1;
    protected static final int RAW_DATA_STATE_READY = 0;
    protected static final int RAW_DATA_STATE_ERROR = 1;

    SparseArray<RawDataBlock> mTypedRawData = new SparseArray<>();
    SparseIntArray mTypedState = new SparseIntArray();
    SparseIntArray mTypedPosition = new SparseIntArray();

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
//        if (mTypedState.get(RawDataItem.DATA_TYPE_OBD) != RAW_DATA_STATE_READY) {
//            loadRawData(RawDataItem.DATA_TYPE_OBD);
//        }
//
//        if (mTypedState.get(RawDataItem.DATA_TYPE_ACC) != RAW_DATA_STATE_READY) {
//            loadRawData(RawDataItem.DATA_TYPE_ACC);
//        }
//        if (mTypedState.get(RawDataItem.DATA_TYPE_GPS) != RAW_DATA_STATE_READY) {
//            loadRawData(RawDataItem.DATA_TYPE_GPS);
//        }


    }

    private void loadRawData(final int dataType) {
        if (getClipSet() == null || mVdbRequestQueue == null) {

            return;
        }

//        Logger.t(TAG).d("start loading type: " + dataType + " index: " + mCurrentLoadingIndex);

        Clip clip = getClipSet().getClip(mCurrentLoadingIndex);

        ClipFragment clipFragment = new ClipFragment(clip);
        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, clipFragment.getStartTimeMs());
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, clipFragment.getDurationMs());

        RawDataBlockRequest obdRequest = new RawDataBlockRequest(clipFragment.getClip().cid, params,
                new VdbResponse.Listener<RawDataBlock>() {
                    @Override
                    public void onResponse(RawDataBlock response) {
                        //Logger.t(TAG).d("resoponse datatype: " + dataType);
                        //mTypedRawData.put(dataType, response);
                        //mTypedState.put(dataType, RAW_DATA_STATE_READY);
                        onLoadRawDataFinished(dataType, response);
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        //mTypedState.put(dataType, RAW_DATA_STATE_ERROR);
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
                rawDataBlockAll.obdDataBlock = block;
                loadRawData(RawDataItem.DATA_TYPE_ACC);
                break;
            case RawDataItem.DATA_TYPE_ACC:
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

    public interface OnLoadCompleteListener {
        void onLoadComplete();
    }

    private class RawDataBlockAll {
        private RawDataBlock accDataBlock = null;
        private RawDataBlock gpsDataBlock = null;
        private RawDataBlock obdDataBlock = null;
    }
}
