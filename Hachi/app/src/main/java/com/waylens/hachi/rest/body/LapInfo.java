package com.waylens.hachi.rest.body;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lshw on 16/12/19.
 */

public class LapInfo implements Serializable {
    public LapTimer lapTimer;
    public List<LapData> lapDatas;

    public LapInfo(LapTimer lapTimer, LapData[] lapDatas) {
        this.lapTimer = lapTimer;
        this.lapDatas = Arrays.asList(lapDatas);
    }

    public LapInfo(LapTimer lapTimer, ArrayList<LapData> lapDatas) {
        this.lapTimer = lapTimer;
        this.lapDatas = lapDatas;
    }

    public static class LapTimer implements Serializable{
        public int totalLaps;
        public long bestLapTime;
        public int bestLapSpeed;
        public int checkPoints;

    }

    public static class LapData implements Serializable{
        public long totalLapTime;
        public long startOffsetMs;
        public int checkIntervalMs;
        public List<Integer> deltaMsToBest;
    }
}
