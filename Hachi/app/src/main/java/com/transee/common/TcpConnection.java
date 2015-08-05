package com.transee.common;

import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

// this class uses 2 threads,
// one for sending, and one for receiving
abstract public class TcpConnection {
    static final int CONNECT_TIMEOUT = 1000 * 30;

    abstract public void onConnectedAsync();

    abstract public void onConnectErrorAsync();

    abstract public void cmdLoop(Thread thread) throws IOException, InterruptedException;

    abstract public void msgLoop(Thread thread) throws IOException, InterruptedException;

    final String mName;
    InetSocketAddress mAddress; // can be set in constructor or start()

    Socket mSocket;
    CmdThread mThread;
    boolean mbConnectError;

    private String TAG() {
        return Thread.currentThread().getName();
    }

    public TcpConnection(String name, InetSocketAddress address) {
        mName = name;
        mAddress = address;
    }

    // API
    public void start(InetSocketAddress address) {
        if (address != null) {
            mAddress = address;
        }

        if (mThread == null) {
            mThread = new CmdThread();
            mThread.start();
        }
    }

    // API
    public void stop() {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        if (mSocket != null) {
            Logger.t(TAG()).d("close socket");
            try {
                mSocket.close();
            } catch (Exception e) {
                Logger.t(TAG()).e(e, "close socket");
            }
            mSocket = null;
        }
    }

    // API
    public boolean isRunning() {
        return mThread != null;
    }

    // API
    public InetSocketAddress getInetSocketAddress() {
        return mAddress;
    }

    // API
    public void readFully(byte[] buffer, int pos, int size) throws IOException {
        InputStream input = mSocket.getInputStream();
        while (size > 0) {
            int ret = input.read(buffer, pos, size);
            if (ret < 0) {
                throw new IOException();
            }
            pos += ret;
            size -= ret;
        }
    }

    // API
    public void sendByteArray(byte[] data) throws IOException {
        mSocket.getOutputStream().write(data);
    }

    // API
    public void sendByteArray(byte[] data, int offset, int count) throws IOException {
        mSocket.getOutputStream().write(data, offset, count);
    }

    synchronized private void connectError() {
        if (!mbConnectError) {
            Logger.t(TAG()).d("connectError");
            mbConnectError = true;
            onConnectErrorAsync();
        }
    }

    void connectToServer() throws IOException, InterruptedException {
        if (mSocket == null) {
            mSocket = new Socket();
        }
        mSocket.setReceiveBufferSize(8192);
        Logger.t(TAG()).d("Connecting to: " + mAddress.getHostName() + ": " + mAddress.getPort());
        mSocket.connect(mAddress, CONNECT_TIMEOUT);
        Logger.t(TAG()).d("Connected: " + mAddress.getHostName());

        TcpConnection.this.onConnectedAsync();
        Logger.t(TAG()).d("connected to " + mAddress.toString());
    }

    class CmdThread extends Thread {

        public CmdThread() {
            super(mName + "-msg");
        }

        @Override
        public void run() {
            MsgThread msgThread = null;
            try {
                connectToServer();
                msgThread = new MsgThread();
                msgThread.start();
                cmdLoop(this);
            } catch (Exception e) {
                Logger.t(TAG()).e(e, "");
            }

            if (msgThread != null) {
                msgThread.interrupt();
            }

            if (isInterrupted()) {
                Logger.t(TAG()).d("cmd thread interrupted");
            } else {
                Logger.t(TAG()).d("cmd thread error");
                TcpConnection.this.connectError();
            }
        }
    }

    class MsgThread extends Thread {

        public MsgThread() {
            super(mName + "-msg");
        }

        @Override
        public void run() {
            try {
                msgLoop(this);
            } catch (Exception e) {
                Logger.t(TAG()).e(e, "");
            }

            if (isInterrupted()) {
                Logger.t(TAG()).d("msg thread interrupted");
            } else {
                Logger.t(TAG()).d("msg thread error");
                TcpConnection.this.connectError();
            }
        }

    }
}
