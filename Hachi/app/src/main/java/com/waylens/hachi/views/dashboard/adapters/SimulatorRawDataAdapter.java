package com.waylens.hachi.views.dashboard.adapters;

import com.orhanobut.logger.Logger;
import com.transee.vdb.VdbClient;
import com.waylens.hachi.snipe.Snipe;
import com.waylens.hachi.snipe.SnipeError;
import com.waylens.hachi.snipe.VdbRequestQueue;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.snipe.toolbox.LiveRawDataRequest;
import com.waylens.hachi.vdb.AccData;
import com.waylens.hachi.vdb.GPSRawData;
import com.waylens.hachi.vdb.OBDData;
import com.waylens.hachi.vdb.RawDataBlock;
import com.waylens.hachi.vdb.RawDataItem;

/**
 * Created by Xiaofei on 2015/12/18.
 */
public class SimulatorRawDataAdapter implements IRawDataAdapter {
    private static final String TAG = SimulatorRawDataAdapter.class.getSimpleName();

    private final VdbRequestQueue mVdbRequestQueue;
    private RawDataItem mLatestAccRawData = null;

    public SimulatorRawDataAdapter() {
        this.mVdbRequestQueue = Snipe.newRequestQueue();
        mVdbRequestQueue.start();
    }

    @Override
    public RawDataItem getAccDataItem(long pts) {
        LiveRawDataRequest request = new LiveRawDataRequest(0, RawDataBlock.F_RAW_DATA_GPS +
            RawDataBlock.F_RAW_DATA_ACC + RawDataBlock.F_RAW_DATA_ODB, new
            VdbResponse.Listener<RawDataItem>() {
            @Override
            public void onResponse(RawDataItem response) {
                if (response.dataType == RawDataBlock.RAW_DATA_ACC) {
                    AccData accData = (AccData)response.object;
                    Logger.t(TAG).d("accData: " + accData.toString());
                    mLatestAccRawData = response;
                }
            }
        }, new VdbResponse.ErrorListener() {
            @Override
            public void onErrorResponse(SnipeError error) {

            }
        });
        mVdbRequestQueue.add(request);
        return mLatestAccRawData;
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
