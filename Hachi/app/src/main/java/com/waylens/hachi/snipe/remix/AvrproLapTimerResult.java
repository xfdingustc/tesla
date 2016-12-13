package com.waylens.hachi.snipe.remix;

import java.io.Serializable;

/**
 * Created by lshw on 16/12/8.
 */


public class AvrproLapTimerResult implements Serializable{
    public AvrproLapsHeader lapsHeader;
    public AvrproLapData[] lapList;
    public AvrproGpsParsedData[] gpsList;

    public AvrproLapTimerResult(AvrproLapsHeader lapsHeader, AvrproLapData[] lapList, AvrproGpsParsedData[] gpsList) {
        this.lapsHeader = lapsHeader;
        this.lapList = lapList;
        this.gpsList = gpsList;
    }
}
