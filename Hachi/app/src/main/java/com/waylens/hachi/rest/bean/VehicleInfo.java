package com.waylens.hachi.rest.bean;

import java.io.Serializable;

/**
 * Created by Xiaofei on 2016/9/23.
 */

public class VehicleInfo implements Serializable {
    public String vehicleMaker;
    public String vehicleModel;
    public int vehicleYear;
    public String vehicleDescription;

    @Override
    public String toString() {
        return vehicleMaker + " " + vehicleModel + " " + vehicleYear;
    }
}
