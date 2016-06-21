package com.waylens.hachi.hardware.vdtcamera;

import android.util.Xml;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.orhanobut.logger.Logger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Xiaofei on 2016/6/1.
 */
public class VdtCameraClient implements VdtCameraCmdConsts {
    private static final String TAG = VdtCameraClient.class.getSimpleName();
    private final InetSocketAddress mAddress;
    private final ConnectionChangeListener mConnectionListener;
    private final CameraMessageHandler mCameraMessageHandler;


    private AsyncSocket mAsyncSocket;

    private SimpleCircularBuffer mReceiveCircularBuffer = SimpleCircularBuffer.allocate(8192);


    private boolean mConnectError;


    public VdtCameraClient(InetSocketAddress address, ConnectionChangeListener connectionListener, CameraMessageHandler messageHandler) {
        this.mAddress = address;
        this.mConnectionListener = connectionListener;
        this.mCameraMessageHandler = messageHandler;
    }

    public void start() {
        setupSocket();

    }

    private void setupSocket() {
//        Logger.t(TAG).d("mAddress: " + mAddress.toString());
        AsyncServer.getDefault().connectSocket(mAddress, new ConnectCallback() {
            @Override
            public void onConnectCompleted(Exception ex, final AsyncSocket socket) {
                mAsyncSocket = socket;
                handleConnectCompleted(ex, socket);
            }
        });
    }

    private void handleConnectCompleted(Exception ex, AsyncSocket socket) {

        mAsyncSocket.setDataCallback(new DataCallback() {
            @Override
            public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                for (ByteBuffer byteBuffer : bb.getAllArray()) {
//                    Logger.t(TAG).d("on Data available " + byteBuffer.toString());
                    handleByteBuffer(byteBuffer);
                }
            }
        });
        mConnectionListener.onConnected();
    }


    private synchronized void handleByteBuffer(ByteBuffer byteBuffer) {

        mReceiveCircularBuffer.append(byteBuffer.array(), 0, byteBuffer.limit());


        while (mReceiveCircularBuffer.remaining() >= HEAD_SIZE) {
            int length = mReceiveCircularBuffer.peekInt();
//            Logger.t(TAG).d("remaining: " + mReceiveCircularBuffer.remaining() + " availabe: " + mReceiveCircularBuffer.availalbe());

            if (mReceiveCircularBuffer.remaining() >= length) {
                byte[] oneMessage = mReceiveCircularBuffer.readBuf(length);
                handleOneMessage(oneMessage);
            } else {
                break;
            }
        }
        mReceiveCircularBuffer.compact();
    }

    private void handleOneMessage(byte[] oneBuffer) {
        SimpleInputStream sis = new SimpleInputStream(oneBuffer);
        XmlPullParser xpp = Xml.newPullParser();
        int length = sis.readi32(0);


        sis.setRange(8, length - 8);

        try {
            xpp.setInput(sis, "UTF-8");
            int eventType = xpp.getEventType();


            while (true) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (xpp.getName().equals(XML_CMD)) {
                            parseCmdTag(xpp);
                        }
                        break;
                    default:
                        break;
                }
                if (eventType == XmlPullParser.END_DOCUMENT) {
                    break;
                }

                eventType = xpp.next();

            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public void stop() {

    }

    public void sendCommand(int cmd) {
        sendCommand(cmd, "", "");
    }

    public void sendCommand(int cmd, int p1) {
        sendCommand(cmd, Integer.toString(p1), "");
    }

    public void sendCommand(int cmd, int p1, int p2) {
        sendCommand(cmd, Integer.toString(p1), Integer.toString(p2));
    }

    public void sendCommand(int cmd, String p1) {
        sendCommand(cmd, p1, "");
    }

    public void sendCommand(int cmd, String p1, String p2) {
        VdtCameraCommand command;
        if (cmd >= CMD_DOMAIN_REC_START) {
            command = new VdtCameraCommand(CMD_DOMAIN_REC, cmd - CMD_DOMAIN_REC_START, p1, p2);
        } else {
            command = new VdtCameraCommand(CMD_DOMAIN_CAM, cmd - CMD_DOMAIN_CAM_START, p1, p2);
        }


        writeCommand(command);

    }




    private static final String XML_CCEV = "ccev";
    private static final String XML_CMD = "cmd";
    private static final String XML_ACT = "act";
    private static final String XML_P1 = "p1";
    private static final String XML_P2 = "p2";

    private static final int HEAD_SIZE = 128;


    private void writeCommand(VdtCameraCommand command) {
        SimpleOutputStream sos = new SimpleOutputStream(1024);
        XmlSerializer xml = Xml.newSerializer();

        sos.reset();
        sos.writeZero(8);

        try {
            xml.setOutput(sos, "UTF-8");
            xml.startDocument("UTF-8", true);
            xml.startTag(null, XML_CCEV);

            xml.startTag(null, XML_CMD);
            String act = String.format(Locale.US, "ECMD%1$d.%2$d", command.mDomain, command.mCmd); // TODO : why US

            xml.attribute(null, XML_ACT, act);
            xml.attribute(null, XML_P1, command.mP1);
            xml.attribute(null, XML_P2, command.mP2);
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


            byte[] bytes = new byte[size];
            System.arraycopy(sos.getBuffer(), 0, bytes, 0, size);


            Util.writeAll(mAsyncSocket, bytes, new CompletedCallback() {
                @Override
                public void onCompleted(Exception ex) {
                    if (ex != null) {
                        throw new RuntimeException(ex);
                    }
//                    Logger.t(TAG).d("[Client] Successfully wrote message");
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void parseCmdTag(XmlPullParser xpp) {
        int count = xpp.getAttributeCount();
        if (count >= 1) {
            String act = "";
            String p1 = "";
            String p2 = "";
            if (xpp.getAttributeName(0).equals(XML_ACT)) {
                act = xpp.getAttributeValue(0);
            }
            if (count >= 2) {
                if (xpp.getAttributeName(1).equals(XML_P1)) {
                    p1 = xpp.getAttributeValue(1);
                }
                if (count >= 3) {
                    if (xpp.getAttributeName(2).equals(XML_P2)) {
                        p2 = xpp.getAttributeValue(2);
                    }
                }
            }


            // ECMD0.5
            Matcher matcher = mPattern.matcher(act);
            if (matcher.find() && matcher.groupCount() == 2) {
                int domain = Integer.parseInt(matcher.group(1));
                int cmd = Integer.parseInt(matcher.group(2));


                switch (domain) {
                    case CMD_DOMAIN_CAM:
//                        Logger.t(TAG).d("Domain = " + domain + " cmd = " + cmd);
                        mCameraMessageHandler.handleMessage(cmd + CMD_DOMAIN_CAM_START, p1, p2);
                        break;
                    case CMD_DOMAIN_REC:
//                        Logger.t(TAG).d("Domain = " + domain + " cmd =" + cmd);
                        mCameraMessageHandler.handleMessage(cmd + CMD_DOMAIN_REC_START, p1, p2);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private final Pattern mPattern = Pattern.compile("ECMD(\\d+).(\\d+)", Pattern.CASE_INSENSITIVE
        | Pattern.MULTILINE);

    private synchronized void connectError() {
        if (!mConnectError) {
            Logger.t(TAG).d("connectError");
            mConnectError = true;
            mConnectionListener.onDisconnected();
        }
    }


    public static class VdtCameraCommand {
        final int mDomain;
        final int mCmd;
        final String mP1;
        final String mP2;

        VdtCameraCommand(int domain, int cmd, String p1, String p2) {
            mDomain = domain;
            mCmd = cmd;
            mP1 = p1;
            mP2 = p2;
        }
    }

    public interface ConnectionChangeListener {
        void onConnected();

        void onDisconnected();
    }

    public interface CameraMessageHandler {
        void handleMessage(int code, String p1, String p2);
    }
}
