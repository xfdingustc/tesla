package com.waylens.hachi.snipe;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class VdbConnection {
    private static final String TAG = VdbConnection.class.getSimpleName();
    private static final int VDB_CMD_PORT = 8083;
    private static final int CONNECT_TIMEOUT = 1000 * 30;
    private Socket mSocket;
    private InetSocketAddress mSocketAddress;

    public VdbConnection(String address) {
        mSocketAddress = new InetSocketAddress(address, VDB_CMD_PORT);
    }

    public void connect() throws IOException, InterruptedException{
        if (mSocket == null) {
            mSocket = new Socket();
        }
        mSocket.setReceiveBufferSize(8192);
        Logger.t(TAG).d("Connecting to: " + mSocketAddress.getHostName() + ": " + mSocketAddress.getPort());
        mSocket.connect(mSocketAddress, CONNECT_TIMEOUT);
        Logger.t(TAG).d("Connected: " + mSocketAddress.getHostName());
    }

    public void sendByteArray(byte[] data) throws IOException {
        mSocket.getOutputStream().write(data);
    }

    // API
    public void sendByteArray(byte[] data, int offset, int count) throws IOException {
        mSocket.getOutputStream().write(data, offset, count);
    }
}
