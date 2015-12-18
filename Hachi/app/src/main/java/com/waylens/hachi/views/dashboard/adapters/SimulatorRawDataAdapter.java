package com.waylens.hachi.views.dashboard.adapters;

import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

import java.util.Random;

/**
 * Created by Xiaofei on 2015/12/18.
 */
public class SimulatorRawDataAdapter implements IRawDataAdapter {
    @Override
    public RawDataItem getAccDataItem(long pts) {
        return null;
    }

    @Override
    public RawDataItem getObdDataItem(long pts) {
        RawDataItem item = new RawDataItem();
        item.dataType = RawDataBlock.RAW_DATA_ODB;
        item.clipTimeMs = -1;
        int speed = (int)(Math.random() * 200);
        int temperater = (int)(Math.random() * 100);
        int rpm = (int)(Math.random() * 12000);
        OBDData data = new OBDData(speed, temperater, rpm);
        item.object = data;
        return item;
    }

    @Override
    public RawDataItem getGpsDataItem(long pts) {
        return null;
    }
}
