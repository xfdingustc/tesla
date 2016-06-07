package com.waylens.hachi.ui.liveview.camerapreview;

import android.util.Log;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.hardware.vdtcamera.PreviewConnection;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

abstract public class MjpegReceiver extends Thread {
    private static final String TAG = MjpegReceiver.class.getSimpleName();
    public static final int ERROR_CANNOT_CONNECT = 1;
    public static final int ERROR_CONNECTION = 2;

    abstract public void onIOError(int error);

    private boolean mbRunning;
    private final PreviewConnection mPreviewConnection;

    private final ByteArrayBuffer.Manager mBufferManager;
    private final SimpleQueue<ByteArrayBuffer> mOutputQ;

    private MjpegBuffer mBuffer;

    public MjpegReceiver(PreviewConnection connection, SimpleQueue<ByteArrayBuffer> outputQ) {
        super("MjpegReceiver");
        mPreviewConnection = connection;
        // 3 buffers: receiving; in queue; decoding
        mBufferManager = new ByteArrayBuffer.Manager(3);
        mOutputQ = outputQ;
    }

    // API
    public void shutdown() {
        mbRunning = false;
        interrupt();


        Logger.t(TAG).d("shutdown");
        try {
            join();
        } catch (Exception e) {

        }
        Logger.t(TAG).d("join");
    }





    private void runOnce() {
        int error = ERROR_CANNOT_CONNECT;
        try {
            connect();
            error = ERROR_CONNECTION;
            while (checkRunning()) {
                readOneFrame();
            }
        } catch (IOException e) {
            Logger.t(TAG).d("IOException: " + e.getMessage());
            onIOError(error);
        }
    }

    @Override
    public void start() {
        mbRunning = true;
        super.start();
    }

    @Override
    public void run() {
        runOnce();

        checkRunning();
    }

    private boolean checkRunning() {
        if (mbRunning && !isInterrupted()) {
            return true;
        }

        Logger.t(TAG).d("mbRunning: " + mbRunning + ", isInterrupted: " + isInterrupted());
        return false;
    }

    private void connect() throws IOException {

        mBuffer = new MjpegBuffer(mPreviewConnection.getSocket().getInputStream());
        mBuffer.refill();
        mBuffer.skipHttpEnd();
    }

    private void readOneFrame() throws IOException {
        // find Content-Length
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
        ByteArrayBuffer buffer = mBufferManager.allocateBuffer(frameLen);
        mBuffer.read(buffer.getBuffer(), 0, frameLen);

        // send to decoder
        mOutputQ.putObject(buffer);
    }
}
