package com.waylens.hachi.rest.bean;

import android.text.TextUtils;

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
        if (!TextUtils.isEmpty(vehicleModel)) {
            return vehicleMaker + " " + vehicleModel + " " + vehicleYear;
        } else {
            return null;
        }
    }
}
