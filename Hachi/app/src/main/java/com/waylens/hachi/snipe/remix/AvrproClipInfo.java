package com.waylens.hachi.snipe.remix;

/**
 * Created by laina on 16/10/27.
 */

public class AvrproClipInfo {
    public String guid_str;
    public int id;
    public int type;
    public int start_time_lo;
    public int start_time_hi;
    public int duration_ms;

    public AvrproClipInfo(String guidStr, int id, int type, int start_time_lo, int start_time_hi, int duration_ms) {
        this.guid_str = guidStr;
        this.id = id;
        this.type = type;
        this.start_time_lo = start_time_lo;
        this.start_time_hi = start_time_hi;
        this.duration_ms = duration_ms;
    }
}
