package com.waylens.hachi.ui.clips.player;

import android.os.Bundle;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSegment;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.snipe.vdb.ClipSetPos;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;


import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;

/**
 * Created by Xiaofei on 2016/3/8.
 */
public class RawDataLoader {
    private static final String TAG = RawDataLoader.class.getSimpleName();
    public static int OBD_DATA = 0;
    public static int IIO_DATA = 1;
    public static int GPS_DATA = 2;

    private final int mClipSetIndex;
    private final VdbRequestQueue mVdbRequestQueue;

    private List<RawDataBlockAll> mRawDataBlockList = new ArrayList<>();

    private List<RawDataItem> mRawDataItemList = new ArrayList<>(3);
    int[] unchangedCount = new int[] {-1, -1, -1};
    int periodReached;


    public RawDataLoader(int clipSetIndex, VdbRequestQueue requestQueue) {
        this.mClipSetIndex = clipSetIndex;
        this.mVdbRequestQueue = requestQueue;
        for (int i = 0; i < 3; i++) {
            mRawDataItemList.add(null);
        }
        periodReached = 0;
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

    public Observable loadRawDataRx() {
        return Observable.from(getClipSet().getClipList())
            .concatMap(new Func1<Clip, Observable<RawDataBlockAll>>() {
                @Override
                public Observable<RawDataBlockAll> call(Clip clip) {
                    return getRawDataBlockAllRx(clip);
                }
            })
            .doOnNext(new Action1<RawDataBlockAll>() {
                @Override
                public void call(RawDataBlockAll rawDataBlockAll) {
                    mRawDataBlockList.add(rawDataBlockAll);
                }
            });
    }

    public Observable loadRawDataRx(final int duration) {
        return Observable.from(getClipSet().getClipList())
            .concatMap(new Func1<Clip, Observable<RawDataBlockAll>>() {
                @Override
                public Observable<RawDataBlockAll> call(Clip clip) {
                    return getRawDataBlockAllRx(clip, duration);
                }
            })
            .doOnNext(new Action1<RawDataBlockAll>() {
                @Override
                public void call(RawDataBlockAll rawDataBlockAll) {
                    mRawDataBlockList.add(rawDataBlockAll);
                }
            });
    }

    private Observable<RawDataBlockAll> getRawDataBlockAllRx(Clip clip, int duration) {
        Observable<RawDataBlock> obdObservable = SnipeApiRx.getRawDataBlockRx(clip, RawDataItem.DATA_TYPE_OBD, clip.getStartTimeMs(), duration);
        Observable<RawDataBlock> gpsObservable = SnipeApiRx.getRawDataBlockRx(clip, RawDataItem.DATA_TYPE_GPS, clip.getStartTimeMs(), duration);
        Observable<RawDataBlock> iioObservalbe = SnipeApiRx.getRawDataBlockRx(clip, RawDataItem.DATA_TYPE_IIO, clip.getStartTimeMs(), duration);
        return Observable.zip(obdObservable, gpsObservable, iioObservalbe, new Func3<RawDataBlock, RawDataBlock, RawDataBlock, RawDataBlockAll>() {
            @Override
            public RawDataBlockAll call(RawDataBlock obd, RawDataBlock gps, RawDataBlock iio) {
                RawDataBlockAll rawDataBlockAll = new RawDataBlockAll();
                rawDataBlockAll.obdDataBlock = obd;
                rawDataBlockAll.gpsDataBlock = gps;
                rawDataBlockAll.iioDataBlock = iio;
                return rawDataBlockAll;
            }
        });
    }


    private Observable<RawDataBlockAll> getRawDataBlockAllRx(Clip clip) {
        return getRawDataBlockAllRx(clip, clip.getDurationMs());
    }


    public RawDataBlock loadRawData(Clip clip, int dataType) {
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

        for (int i = 0; i < unchangedCount.length; i++) {
            if (unchangedCount[i] >= 0)
                unchangedCount[i]++;
        }

        if (rawDataBlockAll.gpsDataBlock != null) {
            RawDataItem gpsItem = rawDataBlockAll.gpsDataBlock.getRawDataItemByTime(clipSetPos.getClipTimeMs());
//            Logger.t(TAG).d("gpsDataBlock != null");
            if (gpsItem != null) {
                gpsItem.setPtsMs(clip.getClipDate()  + gpsItem.getPtsMs());
                unchangedCount[GPS_DATA] = 0;
                mRawDataItemList.set(GPS_DATA, gpsItem);
                periodReached = 1;
//                Logger.t(TAG).d("gpsItem" + gpsItem.toString());
            }
        }

        if (rawDataBlockAll.iioDataBlock != null) {
            RawDataItem iioItem = rawDataBlockAll.iioDataBlock.getRawDataItemByTime(clipSetPos.getClipTimeMs());
//            Logger.t(TAG).d("iioDataBlock != null");
            if (iioItem != null) {
                iioItem.setPtsMs(clip.getClipDate()  + iioItem.getPtsMs());
                unchangedCount[IIO_DATA] = 0;
                mRawDataItemList.set(IIO_DATA, iioItem);
                periodReached = 1;
//                Logger.t(TAG).d("iioItem" + iioItem.toString());
            }
        }

        if (rawDataBlockAll.obdDataBlock != null) {
            RawDataItem obdItem = rawDataBlockAll.obdDataBlock.getRawDataItemByTime(clipSetPos.getClipTimeMs());

            if (obdItem != null) {
                obdItem.setPtsMs(clip.getClipDate()  + obdItem.getPtsMs());
                unchangedCount[OBD_DATA] = 0;
                periodReached = 1;
                mRawDataItemList.set(OBD_DATA, obdItem);
            }
        }

        for (int i = 0; i < unchangedCount.length; i++) {
            if (unchangedCount[i] > 300) {
                mRawDataItemList.set(i, null);
                unchangedCount[i] = -1;
            }
        }

        if (periodReached != 0) {
            for (int i = 0; i < mRawDataItemList.size(); i++) {
                if (mRawDataItemList.get(i) != null)
                    itemList.add(mRawDataItemList.get(i));
            }
            periodReached = 0;
        }

        return itemList;
    }


    private class RawDataBlockAll {
        private RawDataBlock obdDataBlock = null;
        private RawDataBlock gpsDataBlock = null;
        private RawDataBlock iioDataBlock = null;
    }
}
