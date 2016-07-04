package com.waylens.hachi.hardware.vdtcamera.mina;

import android.util.Xml;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.hardware.vdtcamera.SimpleOutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.xmlpull.v1.XmlSerializer;

import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Created by Xiaofei on 2016/7/4.
 */
public class VdtEncoder implements ProtocolEncoder {
    private static final String TAG = VdtEncoder.class.getSimpleName();
    private final static Charset charset = Charset.forName("UTF-8");


    private static final String XML_CCEV = "ccev";
    private static final String XML_CMD = "cmd";
    private static final String XML_ACT = "act";
    private static final String XML_P1 = "p1";
    private static final String XML_P2 = "p2";
    private static final int HEAD_SIZE = 128;

    @Override
    public void dispose(IoSession session) throws Exception {
        Logger.t(TAG).d("#############dispose############");
    }

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
//        Logger.t(TAG).d(message.toString());

        VdtCameraCommand command = (VdtCameraCommand)message;
        IoBuffer buff = IoBuffer.allocate(HEAD_SIZE).setAutoExpand(true);


        SimpleOutputStream sos = new SimpleOutputStream(1024);
        XmlSerializer xml = Xml.newSerializer();

        sos.reset();
        sos.writeZero(8);

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


//            socket.getOutputStream().write(sos.getBuffer(), 0, size);
//            socket.getOutputStream().flush();

//        byte[] byteArray = new byte[size];
//        System.arraycopy(sos.getBuffer(), 0, byteArray, 0, size);
        buff.put(sos.getBuffer(), 0, size);
        buff.flip();

//        mSession.write(new String(byteArray));

//        Logger.t(TAG).d(new String(buff.array()));
        out.write(buff);
        out.flush();


//        buff.putString(message.toString(), charset.newEncoder());
//        // put 当前系统默认换行符
//        buff.putString(LineDelimiter.DEFAULT.getValue(), charset.newEncoder());
//        // 为下一次读取数据做准备
//        buff.flip();
//
//        out.write(buff);
//        out.flush();


    }
}
