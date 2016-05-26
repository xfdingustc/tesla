package com.waylens.hachi.ui.settings;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * Created by Xiaofei on 2016/5/25.
 */
public class FirmwareWriter {
    private final VdtCamera mVdtCamera;
    private final File mFile;
    private WriteListener mListener;

    public FirmwareWriter(File file, VdtCamera vdtCamera) {
        this.mVdtCamera = vdtCamera;
        this.mFile = file;
    }


    public void start(WriteListener listener) {
        this.mListener = listener;
        WriteThread writerThread = new WriteThread();

        writerThread.start();
    }

    public interface WriteListener {
        void onWriteProgress(int progress);
    }


    private class WriteThread extends Thread {
        private static final int CONNECT_TIMEOUT = 1000 * 30;

        @Override
        public void run() {
            super.run();

            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(mVdtCamera.getHostString(), 10097);

            try {
                socket.setSendBufferSize(1024 * 1024);
                socket.connect(socketAddress, CONNECT_TIMEOUT);

                FileInputStream inputStream = new FileInputStream(mFile);

                byte[] buffer = new byte[1024 * 1024];

                int len = 0;
                int dataSend = 0;
                while ((len = inputStream.read(buffer)) > 0) {
                    socket.getOutputStream().write(buffer, 0, len);
                    dataSend += len;
                    if (mListener != null) {
                        mListener.onWriteProgress(dataSend);
                    }
                }

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
