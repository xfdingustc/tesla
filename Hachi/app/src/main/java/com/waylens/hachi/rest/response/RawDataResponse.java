package com.waylens.hachi.rest.response;





import com.waylens.hachi.snipe.utils.ToStringUtils;

import java.util.List;

/**
 * Created by Xiaofei on 2016/6/15.
 */
public class RawDataResponse {
    public AccRawData acc;

    public static class AccRawData {
        public List<Long> captureTime;
        public List<Acceleration> acceleration;


        public class Acceleration {
            public int accelX;
            public int accelY;
            public int accelZ;
            public int gyroX;
            public int gyroY;
            public int gyroZ;
            public int magnX;
            public int magnY;
            public int magnZ;
            public int eulerHeading;
            public int eulerRoll;
            public int eulerPitch;
            public int quaternionW;
            public int quaternionX;
            public int quaternionY;
            public int quaternionZ;
            public int pressure;

            @Override
            public String toString() {
                return ToStringUtils.getString(this);
            }
        }
    }
}
