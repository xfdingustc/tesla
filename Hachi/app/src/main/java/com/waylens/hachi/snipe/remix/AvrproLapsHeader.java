package com.waylens.hachi.snipe.remix;

/**
 * Created by laina on 16/12/6.
 */

public class AvrproLapsHeader {
    public int total_laps;
    public int best_lap_time_ms;
    public int top_speed_kph;

    public AvrproLapsHeader(int total_laps, int best_lap_time_ms, int top_speed_kph) {
        this.total_laps = total_laps;
        this.best_lap_time_ms = best_lap_time_ms;
        this.top_speed_kph = top_speed_kph;
    }
}
