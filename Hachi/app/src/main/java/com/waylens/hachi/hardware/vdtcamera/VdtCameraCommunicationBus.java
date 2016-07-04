package com.waylens.hachi.hardware.vdtcamera;

import android.util.Xml;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.hardware.vdtcamera.mina.VdtCodecFactory;
import com.waylens.hachi.hardware.vdtcamera.mina.VdtCameraCommand;
import com.waylens.hachi.hardware.vdtcamera.mina.VdtMessage;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Xiaofei on 2016/6/1.
 */
public class VdtCameraCommunicationBus implements VdtCameraCmdConsts {
    private static final String TAG = VdtCameraCommunicationBus.class.getSimpleName();
    private final InetSocketAddress mAddress;
    private final ConnectionChangeListener mConnectionListener;
    private final CameraMessageHandler mCameraMessageHandler;
    private Socket mSocket;

    private CommandThread mCommandThread = null;
    private MessageThread mMessageThread = null;

    private boolean mConnectError;

    private boolean mUseMina = true;

    private IoSession mSession = null;

    private BlockingQueue<VdtCameraCommand> mCameraCommandQueue = new LinkedBlockingQueue<>();

    public VdtCameraCommunicationBus(InetSocketAddress address, ConnectionChangeListener connectionListener, CameraMessageHandler messageHandler) {
        this.mAddress = address;
        this.mConnectionListener = connectionListener;
        this.mCameraMessageHandler = messageHandler;
    }

    public void start() {
        if (mUseMina) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    startMinaConnection();
                }
            }).start();

        } else {
            if (mCommandThread == null) {
                mCommandThread = new CommandThread();
                mCommandThread.start();
            }

            if (mMessageThread == null) {
                mMessageThread = new MessageThread();
            }
        }
    }

    private void startMinaConnection() {
        IoConnector connector = new NioSocketConnector();
        connector.setConnectTimeoutMillis(30000);

        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new VdtCodecFactory()));

        connector.addListener(new IoServiceListener() {
            @Override
            public void serviceActivated(IoService ioService) throws Exception {
                Logger.t(TAG).d("serviceActivated");
            }

            @Override
            public void serviceIdle(IoService ioService, IdleStatus idleStatus) throws Exception {
                Logger.t(TAG).d("serviceIdle");
            }

            @Override
            public void serviceDeactivated(IoService ioService) throws Exception {
                Logger.t(TAG).d("serviceDeactivated");
            }

            @Override
            public void sessionCreated(IoSession ioSession) throws Exception {
                Logger.t(TAG).d("sessionCreated");
            }

            @Override
            public void sessionClosed(IoSession ioSession) throws Exception {
                Logger.t(TAG).d("sessionClosed");
            }

            @Override
            public void sessionDestroyed(IoSession ioSession) throws Exception {
                Logger.t(TAG).d("sessionDestroyed");
            }
        });

        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                super.messageReceived(session, message);
                VdtMessage message1 = (VdtMessage)message;
//                Logger.t(TAG).d("message received: " + message1.toString());
                switch (message1.domain) {
                    case CMD_DOMAIN_CAM:
//                        Logger.t(TAG).d("Domain = " + message1.domain + " cmd = " + message1.messageType);
                        mCameraMessageHandler.handleMessage(message1.messageType + CMD_DOMAIN_CAM_START, message1.parameter1, message1.parameter2);
                        break;
                    case CMD_DOMAIN_REC:
//                        Logger.t(TAG).d("Domain = " + message1.domain + " cmd =" + message1.messageType);
                        mCameraMessageHandler.handleMessage(message1.messageType + CMD_DOMAIN_REC_START, message1.parameter1, message1.parameter2);
                        break;
                    default:
                        break;
                }
            }


        });


        ConnectFuture future = connector.connect(mAddress);
        Logger.t(TAG).d("start connection");
        future.awaitUninterruptibly();
        mSession = future.getSession();
        Logger.t(TAG).d("connected");
        new Thread(new Runnable() {
            @Override
            public void run() {
                mConnectionListener.onConnected();
            }
        }).start();


    }

    private void handleOneMessage(byte[] oneBuffer) {
        SimpleInputStream sis = new SimpleInputStream(oneBuffer);
        XmlPullParser xpp = Xml.newPullParser();
        int length = oneBuffer.length;
        int appended = 0;
//        length = sis.readi32(0);

        sis.setRange(4, length - 4);

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
                        Logger.t(TAG).d("Domain = " + domain + " cmd = " + cmd);
                        mCameraMessageHandler.handleMessage(cmd + CMD_DOMAIN_CAM_START, p1, p2);
                        break;
                    case CMD_DOMAIN_REC:
                        Logger.t(TAG).d("Domain = " + domain + " cmd =" + cmd);
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
        if (mUseMina) {
//                  writeCommand(command);
            mSession.write(command);

        } else {
            mCameraCommandQueue.offer(command);
        }
    }

    private static final String XML_CCEV = "ccev";
    private static final String XML_CMD = "cmd";
    private static final String XML_ACT = "act";
    private static final String XML_P1 = "p1";
    private static final String XML_P2 = "p2";
    private static final int HEAD_SIZE = 128;

    private void writeCommand(VdtCameraCommand request) throws IOException {

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


//            socket.getOutputStream().write(sos.getBuffer(), 0, size);
//            socket.getOutputStream().flush();

        byte[] byteArray = new byte[size];
        System.arraycopy(sos.getBuffer(), 0, byteArray, 0, size);

        mSession.write(new String(byteArray));

    }

    private synchronized void connectError() {
        if (!mConnectError) {
            Logger.t(TAG).d("connectError");
            mConnectError = true;
            mConnectionListener.onDisconnected();
            if (mSocket.isConnected()) {
                try {
                    mSocket.shutdownInput();
                    mSocket.shutdownOutput();
                    mSocket.close();
                    mSocket = null;

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Logger.t(TAG).d("socket is closed");
        }
    }


    private class CommandThread extends Thread {


        @Override
        public void run() {
            try {
                Logger.t(TAG).d("create socket: " + mAddress.toString());
                mSocket = new Socket();

                mSocket.setReceiveBufferSize(8192);
                mSocket.connect(mAddress, 3000);

                Logger.t(TAG).d("socket is connected");
                mSocket.setKeepAlive(true);
//                mSocket.setSoTimeout(10000);
                mSocket.setTcpNoDelay(true);

                mConnectionListener.onConnected();
                mMessageThread.start();

                while (true) {
                    VdtCameraCommand command = mCameraCommandQueue.take();
//                    if (command == null) {
//                        command = new VdtCameraCommand(CMD_DOMAIN_CAM, CMD_CAM_GET_NAME, "", "");
//
//                    }
//                    SocketUtils.writeCommand(mSocket, command);


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




    public interface ConnectionChangeListener {
        void onConnected();

        void onDisconnected();
    }

    public interface CameraMessageHandler {
        void handleMessage(int code, String p1, String p2);
    }
}
