package com.waylens.hachi.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbMessageHandler;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.rawdata.GpsData;
import com.waylens.hachi.vdb.rawdata.IioData;
import com.waylens.hachi.vdb.rawdata.ObdData;
import com.waylens.hachi.vdb.rawdata.RawData;
import com.waylens.hachi.vdb.rawdata.RawDataItem;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Richard on 12/28/15.
 */
public class RawDataMsgHandler extends VdbMessageHandler<List<RawDataItem>> {
    public static String TAG = RawDataMsgHandler.class.getSimpleName();
    public RawDataMsgHandler(VdbResponse.Listener<List<RawDataItem>> listener,
                             VdbResponse.ErrorListener errorListener) {
        super(VdbCommand.Factory.MSG_RawData, listener, errorListener);
        rawDataItemList = new ArrayList<RawDataItem>(3);
        Logger.t(TAG).d("rawDataList size = " + rawDataItemList.size());
        for (int i = 0; i < 3; i++) {
            rawDataItemList.add(null);
        }
        Logger.t(TAG).d("rawDataList size = " + rawDataItemList.size());

    }
    public static int OBD_DATA = 0;
    public static int IIO_DATA = 1;
    public static int GPS_DATA = 2;

    private List<RawDataItem> rawDataItemList;
    int[] unchangedCount = new int[] {-1, -1, -1};
    int periodReched = 0;



    @Override
    protected VdbResponse<List<RawDataItem>> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            return null;
        }
        //Logger.t(TAG).d("parseVdbResponse");
        int dataType = response.readi32();
        byte[] data = response.readByteArray();
        List<RawDataItem> rawDataItemListTmp = new ArrayList<RawDataItem>();
        RawDataItem rawDataItem = new RawDataItem(dataType, 0);
        for (int i = 0; i < unchangedCount.length; i++) {
            if (unchangedCount[i] >= 0)
                unchangedCount[i]++;
        }
        switch (dataType) {
            case RawDataItem.DATA_TYPE_OBD:
                unchangedCount[OBD_DATA] = 0;
                rawDataItem.data = ObdData.fromBinary(data);
                if (rawDataItemList.get(OBD_DATA) != null) {
                    periodReched = 1;
                }
                rawDataItemList.set(OBD_DATA, rawDataItem);
                break;
            case RawDataItem.DATA_TYPE_IIO:
                unchangedCount[IIO_DATA] = 0;
                rawDataItem.data = IioData.fromBinary(data);
                if (rawDataItemList.get(IIO_DATA) != null) {
                    periodReched = 1;
                }
                rawDataItemList.set(IIO_DATA, rawDataItem);
                break;
            case RawDataItem.DATA_TYPE_GPS:
                unchangedCount[GPS_DATA] = 0;
                rawDataItem.data = GpsData.fromBinary(data);
                if (rawDataItemList.get(GPS_DATA) != null) {
                    periodReched = 1;
                }
                rawDataItemList.set(GPS_DATA, rawDataItem);
                break;
            default:
                return null;
        }
        for (int i = 0; i < unchangedCount.length; i++) {
            if (unchangedCount[i] > 300) {
                rawDataItemList.set(i, null);
                unchangedCount[i] = -1;
            }
        }



        if (periodReched != 0) {
            for (int i = 0; i < rawDataItemList.size(); i++) {
                if (rawDataItemList.get(i) != null)
                    rawDataItemListTmp.add(rawDataItemList.get(i));
            }
            periodReched = 0;
            //Logger.t(TAG).d("should show off");
        } else {
            Logger.t(TAG).d(unchangedCount[0] + "   "+ unchangedCount[1] + "    " + unchangedCount[2]);
            return null;

        }
        return VdbResponse.success(rawDataItemListTmp);
    }
}
