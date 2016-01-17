package crs_svr.v2;

/**
 * CrsFragment
 * Created by Richard on 1/13/16.
 */
public class CrsFragment {
    String guid;
    String captureTime;
    long startTime;
    long offset;
    int duration;
    double frameRate;
    int resolution;
    int dataType;

    CrsFragment() {

    }

    public CrsFragment(String guid, String captureTime, long startTime, long offset, int duration,
                       short videoWidth,
                       short videoHeight,
                       int dataType) {
        this.guid = guid;
        this.captureTime = captureTime;
        this.startTime = startTime;
        this.offset = offset;
        this.duration = duration;
        this.frameRate = 1.0d;
        this.resolution = ((videoWidth << 16) & 0xFFFF0000) | videoHeight;
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        return String.format("guid[%s], captureTime[%s], startTime[%d], offset[%d], duration[%d]",
                guid, captureTime, startTime, offset, duration);
    }
}
