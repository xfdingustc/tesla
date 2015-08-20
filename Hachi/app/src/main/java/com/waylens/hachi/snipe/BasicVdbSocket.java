package com.waylens.hachi.snipe;

import com.orhanobut.logger.Logger;

import java.io.IOException;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class BasicVdbSocket implements VdbSocket {
    private final static String TAG = BasicVdbSocket.class.getSimpleName();

    @Override
    public RawResponse performRequest(VdbRequest<?> vdbRequest) throws SnipeError {
        Logger.t(TAG).d("perform request !!!!");
        try {
            sendCmd(vdbRequest);
            byte[] array = waitForAck(vdbRequest);
            Logger.t(TAG).d("received bytes: " + array.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new RawResponse(0, false);
    }

    private void sendCmd(VdbRequest<?> vdbRequest) throws IOException {
        VdbConnection connection = vdbRequest.getVdbConnection();
        VdbCommand vdbCommand = vdbRequest.getVdbCommand();

        connection.sendCommnd(vdbCommand);
        //mConnection.sendByteArray(mCmdBuffer);
    }

    private byte[] waitForAck(VdbRequest<?> vdbRequest) throws IOException{
        VdbConnection connection = vdbRequest.getVdbConnection();
        Logger.t(TAG).d("WWWWWWWWWWWWWWWWWWWWWWW");
        return connection.receivedAck();
    }

    //private
}
