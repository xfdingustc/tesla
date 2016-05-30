package com.waylens.hachi.ui.liveview.camerapreviewrx;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Xiaofei on 2016/5/30.
 */
public class MjpegReceiverRx {
    private final InetSocketAddress mAddress;
    private final ByteArrayBufferRx.Manager mBufferManager;
    private Socket mSocket;
    private MjpegBufferRx mBuffer;

    public MjpegReceiverRx(InetSocketAddress address) {
        mAddress = address;
        mBufferManager = new ByteArrayBufferRx.Manager(3);
    }

    public void start() throws IOException {
        mSocket = new Socket();
        mSocket.setReceiveBufferSize(64 * 1024);
        mSocket.connect(mAddress);
        PrintWriter out = new PrintWriter(mSocket.getOutputStream());
        String request = "GET / HTTP/1.1\r\n" + "Host: " + mAddress + "\r\n" + "Connection: keep-alive\r\n"
            + "Cache-Control: no-cache\r\n" + "\r\n";
        out.print(request);
        out.flush();

        mBuffer = new MjpegBufferRx(mSocket.getInputStream());
        mBuffer.refill();
        mBuffer.skipHttpEnd();


    }


    public ByteArrayBufferRx readOneFrame() throws IOException {
        mBuffer.refill();
        mBuffer.skipContentLength();

        // read frame length
        int frameLen = mBuffer.scanInteger();
        if (frameLen <= 0) {
            throw new IOException("cannot get Content-Length");
        }

        // skip http header
        mBuffer.skipHttpEnd();

        // read frame
        ByteArrayBufferRx buffer = mBufferManager.allocateBuffer(frameLen);
        mBuffer.read(buffer.getBuffer(), 0, frameLen);

        // send to decoder
        return buffer;

    }

    public void stop() throws IOException {
        mSocket.close();
    }
}
