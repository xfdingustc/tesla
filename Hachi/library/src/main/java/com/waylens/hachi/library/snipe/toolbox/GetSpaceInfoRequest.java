package com.waylens.hachi.library.snipe.toolbox;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.library.vdb.SpaceInfo;
import com.waylens.hachi.library.snipe.VdbAcknowledge;
import com.waylens.hachi.library.snipe.VdbCommand;
import com.waylens.hachi.library.snipe.VdbRequest;
import com.waylens.hachi.library.snipe.VdbResponse;


/**
 * Created by Xiaofei on 2016/5/5.
 */
public class GetSpaceInfoRequest extends VdbRequest<SpaceInfo> {
    private static final String TAG = GetSpaceInfoRequest.class.getSimpleName();
    public GetSpaceInfoRequest(VdbResponse.Listener<SpaceInfo> listener, VdbResponse.ErrorListener errorListener) {
        super(0, listener, errorListener);
    }

    @Override
    protected VdbCommand createVdbCommand() {
        mVdbCommand = VdbCommand.Factory.createCmdGetSpaceInfo();
        return mVdbCommand;

    }

    @Override
    protected VdbResponse<SpaceInfo> parseVdbResponse(VdbAcknowledge response) {
        if (response.getRetCode() != 0) {
            Logger.t(TAG).d("response: " + response.getRetCode());
            return null;
        }

        SpaceInfo spaceInfo = new SpaceInfo();

        spaceInfo.total = response.readi64();
        spaceInfo.used = response.readi64();
        spaceInfo.marked = response.readi64();

        return VdbResponse.success(spaceInfo);
    }
}