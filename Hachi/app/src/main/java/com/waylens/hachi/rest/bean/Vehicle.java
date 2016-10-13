package com.waylens.hachi.rest.bean;

import com.waylens.hachi.snipe.utils.ToStringUtils;

/**
 * Created by Xiaofei on 2016/10/13.
 */

public class Vehicle {
    public long modelYearID;
    public String maker;
    public String model;
    public int year;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
