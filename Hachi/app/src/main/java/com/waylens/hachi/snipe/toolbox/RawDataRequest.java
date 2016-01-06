package com.waylens.hachi.snipe.toolbox;

import android.os.Bundle;

import com.waylens.hachi.snipe.VdbAcknowledge;
import com.waylens.hachi.snipe.VdbCommand;
import com.waylens.hachi.snipe.VdbRequest;
import com.waylens.hachi.snipe.VdbResponse;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.RawData;
import com.waylens.hachi.vdb.RawDataItem;


/**
 * Created by Xiaofei on 2015/9/11.
 */
public class RawDataRequest extends VdbRequest<RawData> {
    private final Bundle mParameters;
    private final Clip mClip;

    public static final String PARAMETER_CLIP_TIME_MS = "clip_time_ms";
    public static final String PARAMETER_TYPE = "type";

    public RawDataRequest(Clip clip, Bundle parameters, VdbResponse.Listener<RawData> listener,
                          VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
        this.mClip = clip;
        this.mParameters = parameters;
    }

    @Override
    protected VdbCommand createVdbCommand() {
        long clipTimeMs = mParameters.getLong(PARAMETER_CLIP_TIME_MS);
        int type = mParameters.getInt(PARAMETER_TYPE);
        mVdbCommand = VdbCommand.Factory.createCmdGetRawData(mClip, clipTimeMs, type);
        return mVdbCommand;
    }

    @Override
    protected VdbResponse<RawData> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            return null;
        }

        int clipType = response.readi32();
        int clipId = response.readi32();
        Clip.ID cid = new Clip.ID(Clip.CAT_REMOTE, clipType, clipId, null);
        RawData result = new RawData(cid);

        result.clipDate = response.readi32();
        while (true) {
            int dataType = response.readi32();
            if (dataType == 0)
                break;

            long clipTimeMs = response.readi64();
            int size = response.readi32();

            if (size > 0) {
                RawDataItem item = new RawDataItem(dataType, clipTimeMs);

                byte[] data = response.readByteArray(size);
                if (dataType == RawDataItem.DATA_TYPE_GPS) {
                    item.data = RawDataItem.GpsData.fromBinary(data);
                } else if (dataType == RawDataItem.DATA_TYPE_ACC) {
                    item.data = RawDataItem.AccData.fromBinary(data);
                } else if (dataType == RawDataItem.DATA_TYPE_ODB) {
                    item.data = RawDataItem.OBDData.fromBinary(data);
                }


                result.items.add(item);
            }
        }

        return VdbResponse.success(result);

    }
}
