package com.waylens.hachi.hardware.vdtcamera;

import android.util.Xml;

import com.orhanobut.logger.Logger;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Xiaofei on 2016/6/1.
 */
public class MessageThread extends Thread implements VdtCameraCmdConsts{
    private static final String TAG = MessageThread.class.getSimpleName();
    private final Socket mSocket;
    private final CameraMessageHandler mMessageHandler;

    private static final String XML_CCEV = "ccev";
    private static final String XML_CMD = "cmd";
    private static final String XML_ACT = "act";
    private static final String XML_P1 = "p1";
    private static final String XML_P2 = "p2";
    private static final int HEAD_SIZE = 128;

    public MessageThread(Socket socket, CameraMessageHandler messageHandler) {
        this.mSocket = socket;
        this.mMessageHandler = messageHandler;
    }

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
                        mMessageHandler.handleMessage(cmd + CMD_DOMAIN_CAM_START, p1, p2);
                        break;
                    case CMD_DOMAIN_REC:
//                            Logger.t(TAG).d("Domain = " + domain + " cmd =" + cmd);
                        mMessageHandler.handleMessage(cmd + CMD_DOMAIN_REC_START, p1, p2);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private final Pattern mPattern = Pattern.compile("ECMD(\\d+).(\\d+)", Pattern.CASE_INSENSITIVE
        | Pattern.MULTILINE);


    public interface CameraMessageHandler {
        void handleMessage(int code, String p1, String p2);
    }
}
