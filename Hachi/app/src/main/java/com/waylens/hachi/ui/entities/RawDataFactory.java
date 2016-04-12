package com.waylens.hachi.ui.entities;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.vdb.rawdata.RawDataItem;

import java.util.ArrayList;

/**
 * Richard Liang
 * <p/>
 * Created by Richard on 12/14/15.
 */
public class RawDataFactory {

    public static final int RAW_DATA_STATE_UNKNOWN = 0;
    public static final int RAW_DATA_STATE_READY = 1;
    public static final int RAW_DATA_STATE_ERROR = 2;
    public static final int RAW_DATA_STATE_FINISHED = 3;

    private static final String TAG = "RawDataFactory";
    protected static final String REQUEST_TAG = "RETRIEVE_CLIP_DATA";

    SparseArray<ArrayList<RawDataBlock>> mTypedRawData = new SparseArray<>();
    int[] mTypedBlockIndex = new int[4];
    RawDataBlock[] mTypedCurrentBlock = new RawDataBlock[4];
    int[] mTypedState = new int[4];
    int[] mTypedPosition = new int[4];

    private final VdbRequestQueue mVdbRequestQueue;
    Clip mClip;

    long mStartTimeMs;
    int mDuration = 1000 * 60;
    Handler mHandler;

    public RawDataFactory(Clip clip, Handler handler) {
        mVdbRequestQueue = Snipe.newRequestQueue();
        mClip = clip;
        mHandler = handler;

        mStartTimeMs = clip.getStartTimeMs();
        if (mClip.cid.type == Clip.TYPE_MARKED) {
            mDuration = mClip.getDurationMs();
        }
    }

    public int getState(int dataType) {
        if (dataType < 0 || dataType > 3) {
            return RAW_DATA_STATE_UNKNOWN;
        }
        return mTypedState[dataType];
    }

    public RawDataItem getRawDataAt(int dataType, int position) {
        RawDataBlock dataBlock = getNextBlock(dataType, position);
        if (dataBlock == null) {
            return null;
        }

        long requestTimeMs = dataBlock.header.mRequestedTimeMs;
        int pos = mTypedPosition[dataType];
        RawDataItem rawDataItem = null;
        while (pos < dataBlock.dataSize.length) {
            RawDataItem tmp = dataBlock.getRawDataItem(pos);
            long timeOffsetMs =  dataBlock.timeOffsetMs[pos] + requestTimeMs;
            if (timeOffsetMs == position) {
                rawDataItem = tmp;
                mTypedPosition[dataType] = pos;
                break;
            } else if (timeOffsetMs < position) {
                rawDataItem = tmp;
                mTypedPosition[dataType] = pos;
                pos++;
            } else if (timeOffsetMs > position) {
                break;
            }
        }
        return rawDataItem;
    }

    RawDataBlock getNextBlock(int dataType, int position) {
        RawDataBlock dataBlock = mTypedCurrentBlock[dataType];
        if (dataBlock != null
                && position <= (dataBlock.header.mRequestedTimeMs + dataBlock.timeOffsetMs[dataBlock.timeOffsetMs.length - 1])) {
            return dataBlock;
        }

        ArrayList<RawDataBlock> rawDataBlocks = mTypedRawData.get(dataType);
        if (rawDataBlocks == null || mTypedBlockIndex[dataType] > (rawDataBlocks.size() - 1)) {
            return null;
        }

        dataBlock = null;
        for (int i = mTypedBlockIndex[dataType]; i < rawDataBlocks.size(); i++) {
            dataBlock = rawDataBlocks.get(i);
            mTypedBlockIndex[dataType] = i;
            if (position <= (dataBlock.header.mRequestedTimeMs + dataBlock.timeOffsetMs[dataBlock.timeOffsetMs.length - 1])) {
                break;
            }
        }
        mTypedCurrentBlock[dataType] = dataBlock;
        mTypedPosition[dataType] = 0;
        return dataBlock;
    }

    public int loadNextRawData(byte options) {
        if ((mStartTimeMs + mDuration) > (mClip.getStartTimeMs() + mClip.getDurationMs())) {
            Log.e(TAG, "Raw data loading is finished.");
            return RAW_DATA_STATE_FINISHED;
        }
        if ((options & RawDataBlock.F_RAW_DATA_GPS) == RawDataBlock.F_RAW_DATA_GPS) {
            loadRawData(RawDataItem.DATA_TYPE_GPS);
        }
        if ((options & RawDataBlock.F_RAW_DATA_ACC) == RawDataBlock.F_RAW_DATA_ACC) {
            loadRawData(RawDataItem.DATA_TYPE_IIO);
        }
        if ((options & RawDataBlock.F_RAW_DATA_ODB) == RawDataBlock.F_RAW_DATA_ODB) {
            loadRawData(RawDataItem.DATA_TYPE_OBD);
        }
        mStartTimeMs += mDuration;
        return RAW_DATA_STATE_READY;
    }

    void loadRawData(final int dataType) {
        if (mClip == null || mVdbRequestQueue == null) {
            mTypedState[dataType] = RAW_DATA_STATE_ERROR;
            return;
        }

        Logger.t(TAG).d("DataType[1]: " + dataType);

        Bundle params = new Bundle();
        params.putInt(RawDataBlockRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBlockRequest.PARAM_CLIP_TIME, mStartTimeMs);
        params.putInt(RawDataBlockRequest.PARAM_CLIP_LENGTH, mDuration);
        RawDataBlockRequest obdRequest = new RawDataBlockRequest(mClip.cid, params,
                new VdbResponse.Listener<RawDataBlock>() {
                    @Override
                    public void onResponse(RawDataBlock response) {
                        saveRawData(dataType, response);
                        mTypedState[dataType] = RAW_DATA_STATE_READY;
                        mHandler.sendEmptyMessageDelayed(3, 100);
                    }
                },
                new VdbResponse.ErrorListener() {
                    @Override
                    public void onErrorResponse(SnipeError error) {
                        mTypedState[dataType] = RAW_DATA_STATE_ERROR;
                        Logger.t(TAG).d("error response:");
                    }
                });
        mVdbRequestQueue.add(obdRequest.setTag(REQUEST_TAG));
    }

    void saveRawData(int dataType, RawDataBlock dataBlock) {
        if (dataBlock.header == null || dataBlock.header.mNumItems == 0) {
            return;
        }
        ArrayList<RawDataBlock> rawDataBlocks = mTypedRawData.get(dataType);
        if (rawDataBlocks == null) {
            rawDataBlocks = new ArrayList<>();
            mTypedRawData.put(dataType, rawDataBlocks);
        }
        int i = 0;
        for (RawDataBlock block : rawDataBlocks) {
            if (block.header.mRequestedTimeMs > dataBlock.header.mRequestedTimeMs) {
                break;
            }
            i++;
        }
        rawDataBlocks.add(i, dataBlock);
    }
}
