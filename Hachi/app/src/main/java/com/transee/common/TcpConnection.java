package com.transee.common;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

// this class uses 2 threads,
// one for sending, and one for receiving
abstract public class TcpConnection {

    static final boolean DEBUG = false;

    abstract public void onConnectedAsync();

    abstract public void onConnectErrorAsync();

    abstract public void cmdLoop(Thread thread) throws IOException, InterruptedException;

    abstract public void msgLoop(Thread thread) throws IOException, InterruptedException;

    private final String mName;
    private InetSocketAddress mAddress; // can be set in constructor or start()

    private Socket mSocket;
    private CmdThread mThread;
    private boolean mbConnectError;

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
        if (mSocket == null) {
            mSocket = new Socket();
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
            if (DEBUG) {
                Log.d(TAG(), "close socket");
            }
            try {
                mSocket.close();
            } catch (Exception e) {

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
    public OutputStream getOutputStream() throws IOException {
        return mSocket.getOutputStream();
    }

    // API
    public InputStream getInputStream() throws IOException {
        return mSocket.getInputStream();
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
            if (DEBUG) {
                Log.d(TAG(), "connectError");
            }
            mbConnectError = true;
            onConnectErrorAsync();
        }
    }

    private void connectToServer() throws IOException, InterruptedException {
        mSocket.setReceiveBufferSize(8192);
        Log.e("test", "Connecting to: " + mAddress.getHostName() + ": " + mAddress.getPort());
        mSocket.connect(mAddress);
        Log.e("test", "Connected: " + mAddress.getHostName());

        TcpConnection.this.onConnectedAsync();

        if (DEBUG) {
            Log.d(TAG(), "connected to " + mAddress.toString());
        }
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

            } catch (IOException e) {

            } catch (InterruptedException e) {

            }

            if (msgThread != null) {
                msgThread.interrupt();
            }

            if (isInterrupted()) {
                if (DEBUG) {
                    Log.d(TAG(), "cmd thread interrupted");
                }
            } else {
                if (DEBUG) {
                    Log.d(TAG(), "cmd thread error");
                }
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
                Log.e(TAG(), "", e);
            }

            if (isInterrupted()) {
                if (DEBUG) {
                    Log.d(TAG(), "msg thread interrupted");
                }
            } else {
                if (DEBUG) {
                    Log.d(TAG(), "msg thread error");
                }
                TcpConnection.this.connectError();
            }
        }

    }
}
