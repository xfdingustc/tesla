package com.waylens.hachi.snipe.remix;


/**
 * Created by laina on 16/12/6.
 */

public class AvrproLapData {
    public static final int TOTAL_CHECK_POINTS = 1000;
    public int lap_time_ms;
    public int inclip_start_offset_ms;
    public int check_interval_ms;
    public int delta_ms_to_best [] = new int[TOTAL_CHECK_POINTS];
}
