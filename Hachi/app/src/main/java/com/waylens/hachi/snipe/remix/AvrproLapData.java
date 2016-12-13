package com.waylens.hachi.snipe.remix;


import java.io.Serializable;

/**
 * Created by laina on 16/12/6.
 */

public class AvrproLapData implements Serializable{
    public static final int TOTAL_CHECK_POINTS = 1000;
    public int lap_time_ms;
    public int inclip_start_offset_ms;
    public int check_interval_ms;
    public int delta_ms_to_best [] = new int[TOTAL_CHECK_POINTS];
    public boolean isBestLap = false;
    public AvrproLapData(int lap_time_ms, int inclip_start_offset_ms, int check_interval_ms, int[] delta_ms_to_best) {
        this.lap_time_ms = lap_time_ms;
        this.inclip_start_offset_ms = inclip_start_offset_ms;
        this.check_interval_ms = check_interval_ms;
        this.delta_ms_to_best = delta_ms_to_best;
    }
<<<<<<< 5e16876ddb21dfaf3316b29aeb197e838daf5d12
=======

    public AvrproLapData(int lap_time_ms, int inclip_start_offset_ms, int check_interval_ms) {
        this.lap_time_ms = lap_time_ms;
        this.inclip_start_offset_ms = inclip_start_offset_ms;
        this.check_interval_ms = check_interval_ms;
    }
>>>>>>> implement laptimer
}
