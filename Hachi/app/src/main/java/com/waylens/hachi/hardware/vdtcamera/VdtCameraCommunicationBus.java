package com.waylens.hachi.hardware.vdtcamera;

import android.util.Xml;

import com.orhanobut.logger.Logger;

import org.xmlpull.v1.XmlPullParser;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Xiaofei on 2016/6/1.
 */
public class VdtCameraCommunicationBus implements VdtCameraCmdConsts{
    private static final String TAG = VdtCameraCommunicationBus.class.getSimpleName();
    private final InetSocketAddress mAddress;
    private final ConnectionChangeListener mConnectionListener;
    private final CameraMessageHandler mCameraMessageHandler;
    private Socket mSocket;

    private CommandThread mCommandThread = null;
    private MessageThread mMessageThread = null;

    private boolean mConnectError;

    private BlockingQueue<VdtCameraCommand> mCameraCommandQueue = new LinkedBlockingQueue<>();

    public VdtCameraCommunicationBus(InetSocketAddress address, ConnectionChangeListener connectionListener, CameraMessageHandler messageHandler) {
        this.mAddress = address;
        this.mConnectionListener = connectionListener;
        this.mCameraMessageHandler = messageHandler;
    }

    public void start() {
        if (mCommandThread == null) {
            mCommandThread = new CommandThread();
            mCommandThread.start();
        }

        if (mMessageThread == null) {
            mMessageThread = new MessageThread();
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

        sendCommand(command);

    }

    private void sendCommand(VdtCameraCommand command) {
        mCameraCommandQueue.offer(command);
    }

    private synchronized  void connectError() {
        if (!mConnectError) {
            Logger.t(TAG).d("connectError");
            mConnectError = true;
            mConnectionListener.onDisconnected();
        }
    }


    private class CommandThread extends Thread {


        @Override
        public void run() {
            try {
                mSocket = new Socket();

                mSocket.setReceiveBufferSize(8192);
                mSocket.connect(mAddress);
                mSocket.setKeepAlive(true);
                mSocket.setSoTimeout(3000);


                mConnectionListener.onConnected();
                mMessageThread.start();

                while (true) {
                    VdtCameraCommand command = mCameraCommandQueue.poll(1, TimeUnit.SECONDS);
                    if (command == null) {
                        command = new VdtCameraCommand(CMD_DOMAIN_CAM, CMD_CAM_GET_NAME, "", "");
                    }

                    SocketUtils.writeCommand(mSocket, command);
                }
            } catch (Exception e) {
                connectError();
                e.printStackTrace();

            }
        }
    }



    public class MessageThread extends Thread {

        private static final String XML_CCEV = "ccev";
        private static final String XML_CMD = "cmd";
        private static final String XML_ACT = "act";
        private static final String XML_P1 = "p1";
        private static final String XML_P2 = "p2";
        private static final int HEAD_SIZE = 128;

        @Override
        public void run() {
            while (true) {
                try {
                    SimpleInputStream sis = new SimpleInputStream(8192);
                    XmlPullParser xpp = Xml.newPullParser();
                    int length = 0;
                    int appended = 0;

                    sis.clear();

                    SocketUtils.readFully(mSocket, sis.getBuffer(), 0, HEAD_SIZE);
                    length = sis.readi32(0);
                    appended = sis.readi32(4);
                    if (appended > 0) {
                        sis.expand(HEAD_SIZE + appended);
                        SocketUtils.readFully(mSocket, sis.getBuffer(), HEAD_SIZE, appended);
                    }
                    sis.setRange(8, length);

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

                } catch (Exception e) {
                    e.printStackTrace();
                    connectError();
                    return;
                }

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
//                            Logger.t(TAG).d("Domain = " + domain + " cmd = " + cmd);
                            mCameraMessageHandler.handleMessage(cmd + CMD_DOMAIN_CAM_START, p1, p2);
                            break;
                        case CMD_DOMAIN_REC:
//                            Logger.t(TAG).d("Domain = " + domain + " cmd =" + cmd);
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
