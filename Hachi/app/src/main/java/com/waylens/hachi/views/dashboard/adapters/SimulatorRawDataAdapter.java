package com.waylens.hachi.views.dashboard.adapters;

import com.waylens.hachi.vdb.GPSRawData;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

/**
 * Created by Xiaofei on 2015/12/18.
 */
public class SimulatorRawDataAdapter implements IRawDataAdapter {
    @Override
    public RawDataItem getAccDataItem(long pts) {
        RawDataItem item = new RawDataItem();
        //item.dataType = Ra
        return null;
    }

    @Override
    public RawDataItem getObdDataItem(long pts) {
        RawDataItem item = new RawDataItem();
        item.dataType = RawDataBlock.RAW_DATA_ODB;
        item.clipTimeMs = -1;
        int speed = (int) (Math.random() * 200);
        int temperater = (int) (Math.random() * 100);
        int rpm = (int) (Math.random() * 12000);
        OBDData data = new OBDData(speed, temperater, rpm);
        item.object = data;
        return item;
    }

    @Override
    public RawDataItem getGpsDataItem(long pts) {

        RawDataItem item = new RawDataItem();
        item.dataType = RawDataBlock.RAW_DATA_GPS;
        item.clipTimeMs = -1;

        GPSRawData data = new GPSRawData();
        data.speed = 0;
        data.coord.lat = data.coord.lat_orig = 31.191016;
        data.coord.lng = data.coord.lng_orig = 121.601435;

        item.object = data;

        return item;
    }
}
