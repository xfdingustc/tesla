package com.waylens.hachi.hardware.vdtcamera;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;


public class SocketUtils {
    private static final String TAG = SocketUtils.class.getSimpleName();




    public static void readFully(Socket socket, byte[] buffer, int pos, int size) throws IOException {
        InputStream input = socket.getInputStream();
        while (size > 0) {
            int ret = input.read(buffer, pos, size);
            if (ret < 0) {
                throw new IOException();
            }
            pos += ret;
            size -= ret;
        }
    }


    public static void sendByteArray(Socket socket, byte[] data) throws IOException {
        socket.getOutputStream().write(data);
    }




    public static void sendByteArray(Socket socket, byte[] data, int offset, int count) throws IOException {
        socket.getOutputStream().write(data, offset, count);
    }




}
