package com.waylens.hachi.ui.settings;



import com.xfdingustc.snipe.control.VdtCamera;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import rx.Subscriber;

/**
 * Created by Xiaofei on 2016/5/25.
 */
public class FirmwareWriter {
    private final VdtCamera mVdtCamera;
    private final File mFile;
    private final Subscriber<? super Integer> mSubscribe;
    private static final int CONNECT_TIMEOUT = 1000 * 30;

    public FirmwareWriter(File file, VdtCamera vdtCamera, Subscriber<? super Integer> subscriber) {
        this.mVdtCamera = vdtCamera;
        this.mFile = file;
        this.mSubscribe = subscriber;
    }


    public void start() {

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
                mSubscribe.onNext(dataSend);
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
