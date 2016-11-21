package com.waylens.hachi.ui.clips.player;

import android.os.Bundle;

import com.google.android.exoplayer.C;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.camera.VdtCameraManager;
import com.waylens.hachi.snipe.VdbRequestFuture;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.reative.SnipeApiRx;
import com.waylens.hachi.snipe.toolbox.ClipInfoRequest;
import com.waylens.hachi.snipe.toolbox.ClipSetExRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBlockRequest;
import com.waylens.hachi.snipe.toolbox.RawDataBufRequest;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSegment;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.ClipSetManager;
import com.waylens.hachi.snipe.vdb.ClipSetPos;
import com.waylens.hachi.snipe.vdb.rawdata.GpsData;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;


import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action0;
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

    private int mRaceType = -1;

    private List<Long> mRacingTimePoints;

    private List<RawDataItem> mRawDataItemList = new ArrayList<>(3);
    int[] unchangedCount = new int[] {-1, -1, -1};
    int periodReached;


    public RawDataLoader(int clipSetIndex) {
        this.mClipSetIndex = clipSetIndex;
        this.mVdbRequestQueue = VdtCameraManager.getManager().getCurrentVdbRequestQueue();
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
                    Logger.t(TAG).d("load raw data rx do on next");
                    mRawDataBlockList.add(rawDataBlockAll);
                }
            })
            .doOnCompleted(new Action0() {
                @Override
                public void call() {
                    Logger.t(TAG).d("load raw data do on complete");
                    mRacingTimePoints = calculateRaceTimePoints();
                }
            });
    }

    public List<Long> getRacingTimePoints() {
        if (getClipSet().getCount() == 1) {
            return mRacingTimePoints;
        } else {
            return null;
        }
    }

    private List<Long> calculateRaceTimePoints() {
        for (Clip clip : getClipSet().getClipList()) {
            Logger.t(TAG).d("Vin  = " + clip.getVin());

//            Logger.t(TAG).d("clip" + clip.cid.type);
            Clip retClip = loadClipInfo(clip);
            Logger.t(TAG).d("typeRace:" + retClip.typeRace);
            if ((retClip.typeRace & Clip.TYPE_RACE) > 0) {
                mRaceType = retClip.typeRace;
                Logger.t(TAG).d("duration:" + retClip.getDurationMs());
                Logger.t(TAG).d(retClip.typeRace & Clip.MASK_RACE);
//                Logger.t(TAG).d("t1:" + retClip.raceTimingPoints.get(0));
//                Logger.t(TAG).d("t2:" + retClip.raceTimingPoints.get(1));
//                Logger.t(TAG).d("t3:" + retClip.raceTimingPoints.get(2));
//                Logger.t(TAG).d("t4:" + retClip.raceTimingPoints.get(3));
//                Logger.t(TAG).d("t5:" + retClip.raceTimingPoints.get(4));
//                Logger.t(TAG).d("t6:" + retClip.raceTimingPoints.get(5));

                Logger.t(TAG).d("start loading ");
                // First load raw data into memory
                RawDataBlock rawDataBlock = loadRawData(clip, RawDataItem.DATA_TYPE_GPS);
                Logger.t(TAG).d("raw data size:" + rawDataBlock.getItemList().size());
 /*                       GpsData firstGpsData = (GpsData) rawDataBlock.getItemList().get(0).data;
                        if (((long) firstGpsData.utc_time * 1000 + firstGpsData.reserved / 1000) >= clip.raceTimingPoints.get(0)) {
                            Logger.t(TAG).d("find the corresponding video time:" + rawDataBlock.getItemList().get(0).getPtsMs());
                            continue;
                        }*/
                int searchIndex = -1;
                long searchResult = -1;
                if ((retClip.typeRace & Clip.MASK_RACE) == Clip.TYPE_RACE_CD3T || (retClip.typeRace & Clip.MASK_RACE) == Clip.TYPE_RACE_CD6T) {
                    searchIndex = 0;
                } else {
                    searchIndex = 1;
                }
                ArrayList<Long> timeList = new ArrayList<Long>(6);
                long clipStartTime;
                for (int i = 1; i < rawDataBlock.getItemList().size(); i++) {
                    RawDataItem last = rawDataBlock.getItemList().get(i - 1);
                    RawDataItem current = rawDataBlock.getItemList().get(i);
                    GpsData lastGpsData = (GpsData) last.data;
                    GpsData currentGpsData = (GpsData) current.data;
                    long lastGpsTime = (long) lastGpsData.utc_time * 1000 + lastGpsData.reserved / 1000;
                    long currentGpsTime = (long) currentGpsData.utc_time * 1000 + currentGpsData.reserved / 1000;
                    if (lastGpsTime <= retClip.raceTimingPoints.get(searchIndex) && currentGpsTime >= retClip.raceTimingPoints.get(searchIndex)) {
                        if (2 * retClip.raceTimingPoints.get(searchIndex) <= lastGpsTime + currentGpsTime) {
                            Logger.t(TAG).d("gps utc time ms:" + ((long) lastGpsData.utc_time * 1000 + lastGpsData.reserved / 1000));
                            Logger.t(TAG).d("find the corresponding video time:" + last.getPtsMs());
                            searchResult = last.getPtsMs() + retClip.getClipDate();
                        } else {
                            Logger.t(TAG).d("gps utc time ms:" + ((long) currentGpsData.utc_time * 1000 + currentGpsData.reserved / 1000));
                            Logger.t(TAG).d("find the corresponding video time:" + current.getPtsMs());
                            searchResult = current.getPtsMs() + retClip.getClipDate();
                        }
                        clipStartTime = retClip.getStartTimeMs() + retClip.getClipDate();
                        if (searchIndex == 0) {
                            timeList.add(0, searchResult);
                            timeList.add(1, retClip.raceTimingPoints.get(1) - retClip.raceTimingPoints.get(0) + searchResult);
                            timeList.add(2, retClip.raceTimingPoints.get(2) - retClip.raceTimingPoints.get(0) + searchResult);
                            timeList.add(3, retClip.raceTimingPoints.get(3) - retClip.raceTimingPoints.get(0) + searchResult);
                            if (retClip.raceTimingPoints.get(4) > 0) {
                                timeList.add(4, retClip.raceTimingPoints.get(4) - retClip.raceTimingPoints.get(0) + searchResult);
                            } else {
                                timeList.add(4, (long) -1);
                            }
                            if (retClip.raceTimingPoints.get(5) > 0) {
                                timeList.add(5, retClip.raceTimingPoints.get(5) - retClip.raceTimingPoints.get(0) + searchResult);
                            } else {
                                timeList.add(5, (long) -1);
                            }
                        } else if (searchIndex == 1) {
                            timeList.add(0, (long) -1);
                            timeList.add(1, searchResult);
                            timeList.add(2, retClip.raceTimingPoints.get(2) - retClip.raceTimingPoints.get(1) + searchResult);
                            timeList.add(3, retClip.raceTimingPoints.get(3) - retClip.raceTimingPoints.get(1) + searchResult);
                            if (retClip.raceTimingPoints.get(4) > 0) {
                                timeList.add(4, retClip.raceTimingPoints.get(4) - retClip.raceTimingPoints.get(1) + searchResult);
                            } else {
                                timeList.add(4, (long) -1);
                            }
                            if (retClip.raceTimingPoints.get(5) > 0) {
                                timeList.add(5, retClip.raceTimingPoints.get(5) - retClip.raceTimingPoints.get(1) + searchResult);
                            } else {
                                timeList.add(5, (long) -1);
                            }
                        }
                        for (int j = 0; j < timeList.size(); j++) {
                            timeList.set(j, timeList.get(j) - clipStartTime);
                        }

                        return timeList;
                    }
                }
            }
        }
        return null;
    }

    private Clip loadClipInfo(Clip clip) {
        VdbRequestFuture<Clip> requestFuture = VdbRequestFuture.newFuture();
        ClipInfoRequest request = new ClipInfoRequest(clip.cid, ClipSetExRequest.FLAG_CLIP_EXTRA | ClipSetExRequest.FLAG_CLIP_DESC | ClipSetExRequest.FLAG_CLIP_SCENE_DATA,
            clip.cid.type, 0, requestFuture, requestFuture);
        mVdbRequestQueue.add(request);
        try {
            Clip retClip = requestFuture.get();
            return retClip;
        } catch (Exception e) {
            Logger.t(TAG).e("Load raw data: " + e.getMessage());
            return null;
        }
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



    public RawDataBufAll loadRawDataBuf(final Clip clip, long start, int duration) {
        RawDataBufAll rawDataBufAll = new RawDataBufAll();
        rawDataBufAll.obdDataBuf = getRawDataBuf(clip, RawDataItem.DATA_TYPE_OBD, start, duration);
        rawDataBufAll.gpsDataBuf = getRawDataBuf(clip, RawDataItem.DATA_TYPE_GPS, start, duration);
        rawDataBufAll.iioDataBuf = getRawDataBuf(clip, RawDataItem.DATA_TYPE_IIO, start, duration);
        return rawDataBufAll;
    }

    public static byte[] getRawDataBuf(Clip clip, int dataType, long startTime, int duration) {
        Bundle params = new Bundle();
        params.putInt(RawDataBufRequest.PARAM_DATA_TYPE, dataType);
        params.putLong(RawDataBufRequest.PARAM_CLIP_TIME, startTime);
        params.putInt(RawDataBufRequest.PARAM_CLIP_LENGTH, duration);

        VdbRequestFuture<byte[]> requestFuture = VdbRequestFuture.newFuture();
        RawDataBufRequest request = new RawDataBufRequest(clip.cid, params, requestFuture, requestFuture);
        VdtCameraManager.getManager().getCurrentVdbRequestQueue().add(request);
        try {
            byte[] buffer = requestFuture.get();
            return buffer;
        } catch (Exception e) {
            return null;
        }
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


    public class RawDataBlockAll {
        public RawDataBlock obdDataBlock = null;
        public RawDataBlock gpsDataBlock = null;
        public RawDataBlock iioDataBlock = null;
    }

    public class RawDataBufAll {
        public byte[] obdDataBuf = null;
        public byte[] gpsDataBuf = null;
        public byte[] iioDataBuf = null;
    }

}
