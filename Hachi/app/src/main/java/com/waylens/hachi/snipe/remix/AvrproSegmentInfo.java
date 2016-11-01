package com.waylens.hachi.snipe.remix;

/**
 * Created by laina on 16/10/27.
 */

public class AvrproSegmentInfo {
    public AvrproClipInfo parent_clip;
    public int inclip_offset_ms;
    public int duration_ms;
    public int filter_type;
    public int max_speed_kph;

    public AvrproSegmentInfo(AvrproClipInfo parentClip, int inclipOffMs, int durationMs, int filterType, int maxSpeedKph) {
        this.parent_clip = parentClip;
        this.inclip_offset_ms = inclipOffMs;
        this.duration_ms = durationMs;
        this.filter_type = filterType;
        this.max_speed_kph = maxSpeedKph;
    }
}
