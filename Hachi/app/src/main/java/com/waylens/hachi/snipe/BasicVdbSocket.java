package com.waylens.hachi.snipe;

import com.orhanobut.logger.Logger;
import com.transee.vdb.Vdb;

import java.io.IOException;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class BasicVdbSocket implements VdbSocket {
    private final static String TAG = BasicVdbSocket.class.getSimpleName();

    @Override
    public void performRequest(VdbRequest<?> vdbRequest) throws SnipeError {
        try {
            VdbCommand vdbCommand = vdbRequest.createVdbCommand();
            vdbCommand.setSequence(vdbRequest.getSequence());
            Snipe.getVdbConnect().sendCommnd(vdbCommand);
        } catch (Exception e) {
            throw new SnipeError();
        }
    }

    @Override
    public VdbAcknowledge retrieveAcknowledge() throws IOException {
        return new VdbAcknowledge(0, Snipe.getVdbConnect());
    }
}
