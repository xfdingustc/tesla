package com.waylens.hachi.snipe.utils;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.rawdata.GpsData;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataItem;

import java.util.ArrayList;

/**
 * Created by laina on 16/12/2.
 */

public class RaceTimeParseUtils {
    public static final String TAG = RaceTimeParseUtils.class.getSimpleName();

    public static ArrayList<Long> parseRaceTime(Clip retClip, RawDataBlock rawDataBlock) {
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
                    Logger.t(TAG).d("time " + j + " " + timeList.get(j));
                }
                return timeList;
            }
        }
        return null;
    }

}
