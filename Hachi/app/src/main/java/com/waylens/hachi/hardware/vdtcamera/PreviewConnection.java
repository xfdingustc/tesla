package com.waylens.hachi.hardware.vdtcamera;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Xiaofei on 2016/6/7.
 */
public class PreviewConnection {
    private final InetSocketAddress mAddress;
    private Socket mSocket;

    public PreviewConnection(InetSocketAddress address) {
        mAddress = address;
    }

    public void connect() throws IOException {
        mSocket = new Socket();
        mSocket.setReceiveBufferSize(64 * 1024);
        mSocket.connect(mAddress);

        PrintWriter out = new PrintWriter(mSocket.getOutputStream());
        String request = "GET / HTTP/1.1\r\n" + "Host: " + mAddress + "\r\n" + "Connection: keep-alive\r\n"
            + "Cache-Control: no-cache\r\n" + "\r\n";
        out.print(request);
        out.flush();
    }


    public Socket getSocket() {
        return mSocket;
    }
}
