package com.waylens.hachi.hardware.vdtcamera;

import android.util.Xml;

import com.orhanobut.logger.Logger;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Locale;


public class SocketUtils {
    private static final String TAG = SocketUtils.class.getSimpleName();

    private static final String XML_CCEV = "ccev";
    private static final String XML_CMD = "cmd";
    private static final String XML_ACT = "act";
    private static final String XML_P1 = "p1";
    private static final String XML_P2 = "p2";

    private static final int HEAD_SIZE = 128;


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

    public static void readFully(InputStream inputStream, byte[] buffer, int pos, int size) throws IOException {

        while (size > 0) {
            int ret = inputStream.read(buffer, pos, size);
            if (ret < 0) {
                throw new IOException();
            }
            pos += ret;
            size -= ret;
        }
    }

    public static void writeCommand(Socket socket, VdtCameraCommunicationBus.VdtCameraCommand request) throws IOException, InterruptedException {
        SimpleOutputStream sos = new SimpleOutputStream(1024);
        XmlSerializer xml = Xml.newSerializer();

        sos.reset();
        sos.writeZero(8);

        xml.setOutput(sos, "UTF-8");
        xml.startDocument("UTF-8", true);
        xml.startTag(null, XML_CCEV);

        xml.startTag(null, XML_CMD);
        String act = String.format(Locale.US, "ECMD%1$d.%2$d", request.mDomain, request.mCmd); // TODO : why US

        xml.attribute(null, XML_ACT, act);
        xml.attribute(null, XML_P1, request.mP1);
        xml.attribute(null, XML_P2, request.mP2);
        xml.endTag(null, XML_CMD);

        xml.endTag(null, XML_CCEV);
        xml.endDocument();


        int size = sos.getSize();
        if (size >= HEAD_SIZE) {
            sos.writei32(0, size);
            sos.writei32(4, size - HEAD_SIZE);
        } else {
            sos.writei32(0, HEAD_SIZE);
            // append is 0
            sos.clear(size, HEAD_SIZE - size);
            size = HEAD_SIZE;
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put(sos.getBuffer(), 0, size);

//        buffer.limit(size);

        Logger.t(TAG).d(" size: " + buffer.array().length + " " + new String(buffer.array()));

        socket.getOutputStream().write(sos.getBuffer(), 0, size);
    }


}
