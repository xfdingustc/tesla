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
        Logger.t(TAG).d("perform request !!!!");
        try {

            byte[] array = waitForAck(vdbRequest);
            Logger.t(TAG).d("rRRRRRRRRReceived bytes: " + array.length);

            sendCmd(vdbRequest);
            byte[] array1 = waitForAck(vdbRequest);
            Logger.t(TAG).d("rRRRRRRRRReceived bytes: " + array1.length);

            return new VdbAcknowledge(0, false, array1, vdbRequest.getVdbConnection());

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    private void sendCmd(VdbRequest<?> vdbRequest) throws IOException {
        VdbConnection connection = vdbRequest.getVdbConnection();
        VdbCommand vdbCommand = vdbRequest.getVdbCommand();

        connection.sendCommnd(vdbCommand);
        //mConnection.sendByteArray(mCmdBuffer);
    }

    private byte[] waitForAck(VdbRequest<?> vdbRequest) throws IOException{
        VdbConnection connection = vdbRequest.getVdbConnection();

        return connection.receivedAck();
    }

    //private
}
