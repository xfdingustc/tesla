package com.waylens.hachi.rest.body;

/**
 * Created by liushuwei on 2016/6/13.
 */
public class DeviceLoginBody {
    public String deviceType;
    public String deviceID;

    public DeviceLoginBody(String deviceType, String deviceID) {
        this.deviceType = deviceType;
        this.deviceID = deviceID;
    }
}
