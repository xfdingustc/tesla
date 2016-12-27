package com.waylens.hachi.rest.bean;

import com.waylens.hachi.snipe.utils.ToStringUtils;
import java.io.Serializable;

/**
 * Created by Xiaofei on 2016/10/13.
 */

public class Vehicle implements Serializable{
    public long modelYearID;
    public String maker;
    public String model;
    public int year;

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Vehicle) {
            Vehicle v = (Vehicle) object;
            return v.modelYearID == this.modelYearID;
        } else {
            return false;
        }
    }
}
