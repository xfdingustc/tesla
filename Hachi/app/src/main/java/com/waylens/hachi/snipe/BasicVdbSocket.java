package com.waylens.hachi.snipe;

import com.orhanobut.logger.Logger;

import java.io.IOException;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class BasicVdbSocket implements VdbSocket {
    private final static String TAG = BasicVdbSocket.class.getSimpleName();

    @Override
    public VdbAcknowledge performRequest(VdbRequest<?> vdbRequest) throws SnipeError {

        try {
            sendCmd(vdbRequest);
            return new VdbAcknowledge(0, false, vdbRequest.getVdbCommand().getCommandCode(),
                vdbRequest.getVdbConnection());


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    private void sendCmd(VdbRequest<?> vdbRequest) throws IOException {
        VdbConnection connection = vdbRequest.getVdbConnection();
        VdbCommand vdbCommand = vdbRequest.createVdbCommand();

        connection.sendCommnd(vdbCommand);
        //mConnection.sendByteArray(mCmdBuffer);
    }

    //private
}
