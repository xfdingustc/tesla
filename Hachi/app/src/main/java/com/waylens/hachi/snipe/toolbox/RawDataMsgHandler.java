package com.waylens.hachi.snipe.toolbox;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbMessageHandler;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.RawDataItem;


/**
 * Created by Richard on 12/28/15.
 */
public class RawDataMsgHandler extends VdbMessageHandler<RawDataItem> {
    public RawDataMsgHandler(VdbResponse.Listener<RawDataItem> listener,
                             VdbResponse.ErrorListener errorListener) {
        super(VdbCommand.Factory.MSG_RawData, listener, errorListener);
    }

    @Override
    protected VdbResponse<RawDataItem> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            return null;
        }
        int dataType = response.readi32();
        byte[] data = response.readByteArray();
        RawDataItem rawDataItem = new RawDataItem(dataType, 0);
        switch (dataType) {
            case RawDataItem.RAW_DATA_ODB:
                rawDataItem.object = RawDataItem.OBDData.parse(data);
                break;
            case RawDataItem.RAW_DATA_ACC:
                rawDataItem.object = RawDataItem.AccData.parse(data);
                break;
            case RawDataItem.RAW_DATA_GPS:
                rawDataItem.object = RawDataItem.GPSRawData.translate(data);
                break;
            default:
                return null;
        }
        return VdbResponse.success(rawDataItem);
    }
}
