
package crs_svr;

import crs_svr.WaylensCommHead;
import crs_svr.EncodeCommHead;
import crs_svr.IWaylensCode;

public class CommWaylensParse {
    public static String private_key = "";
    public static byte encode_type = ProtocolConstMsg.ENCODE_TYPE_OPEN;

    public static int encode_head(WaylensCommHead head, byte[] out_buf) {
        try {
            EncodeCommHead encode_head = new EncodeCommHead();
            if (null == encode_head) return -1;

            head.size = (short) (head.COMM_HEAD_LEGTH);
            head.encode(out_buf, 4);
            if (ProtocolConstMsg.ENCODE_TYPE_OPEN == encode_type) {
                encode_head.encode_type = encode_type;
                encode_head.size = (short) (head.size + 4);
                encode_head.encode(out_buf, 0);
                return encode_head.size;
            } else if (ProtocolConstMsg.ENCODE_TYPE_AES == encode_type) {

                byte[] temp = PTUntil.encrypt(out_buf, 4, head.size + 4, private_key);
                byte[] encode_buf = null;
                if (null == temp || (out_buf.length < (temp.length + 5)))
                    return -4;

                int enlen = 16 - (head.size % 16);
                if (16 == enlen)
                    enlen = 0;
                encode_head.encode_type = encode_type;
                encode_head.size = (short) (temp.length + 5);
                encode_head.encode(out_buf, 0);
                out_buf[4] = (byte) enlen;
                System.arraycopy(temp, 0, out_buf, 5, temp.length);
                return encode_head.size;
            } else return -3;
        } catch (Exception e) {
            return -4;
        }
    }

    public static int encode(WaylensCommHead head, IWaylensCode body, byte[] out_buf) {
        try {
            EncodeCommHead encode_head = new EncodeCommHead();
            if (null == encode_head) return -1;

            int ilen = body.encode(out_buf, head.COMM_HEAD_LEGTH + 4);
            if (0 >= ilen) return -2;
            head.size = (short) (head.COMM_HEAD_LEGTH + ilen);
            head.encode(out_buf, 4);
            if (ProtocolConstMsg.ENCODE_TYPE_OPEN == encode_type) {
                encode_head.encode_type = encode_type;
                encode_head.size = (short) (head.size + 4);
                encode_head.encode(out_buf, 0);
                return encode_head.size;
            } else if (ProtocolConstMsg.ENCODE_TYPE_AES == encode_type) {

                byte[] temp = PTUntil.encrypt(out_buf, 4, head.size + 4, private_key);
                byte[] encode_buf = null;
                if (null == temp || (out_buf.length < (temp.length + 5)))
                    return -4;

                int enlen = 16 - (head.size % 16);
                if (16 == enlen)
                    enlen = 0;
                encode_head.encode_type = encode_type;
                encode_head.size = (short) (temp.length + 5);
                encode_head.encode(out_buf, 0);
                out_buf[4] = (byte) enlen;
                System.arraycopy(temp, 0, out_buf, 5, temp.length);
                return encode_head.size;
            } else return -4;
        } catch (Exception e) {
            return -5;
        }
    }

    public static int decode(byte[] in_buf, int inbuf_size, WaylensCommHead head, IWaylensCode body) {
        try {
            if (in_buf.length < 4) return -1;
            EncodeCommHead decode_head = new EncodeCommHead();
            if (null == decode_head) return -2;
            decode_head.decode(in_buf, inbuf_size);
            if (ProtocolConstMsg.ENCODE_TYPE_OPEN == encode_type) {
                if (0 != head.decode(in_buf, 4)) return -3;
                if ((head.size + 4) > inbuf_size) return -4;
                if (0 > body.decode(in_buf, head.COMM_HEAD_LEGTH + 4)) return -5;
                return 0;
            } else if (ProtocolConstMsg.ENCODE_TYPE_AES == encode_type) {
                byte[] decode_buf = PTUntil.decrypt(in_buf, 5, decode_head.size, private_key);
                if (null == decode_buf) return -3;
                if (0 != head.decode(decode_buf, 0)) return -4;
                if (0 > body.decode(decode_buf, head.COMM_HEAD_LEGTH)) return -5;
                return 0;
            } else return -10;
        } catch (Exception e) {
            return -4;
        }
    }

    public static int decode_head(byte[] in_buf, int inbuf_size, WaylensCommHead head) {
        try {
            if (in_buf.length < 4) return -1;
            EncodeCommHead decode_head = new EncodeCommHead();
            if (null == decode_head) return -2;
            decode_head.decode(in_buf, inbuf_size);
            if (ProtocolConstMsg.ENCODE_TYPE_OPEN == encode_type) {
                if (0 != head.decode(in_buf, 4)) return -3;
                return 0;
            } else if (ProtocolConstMsg.ENCODE_TYPE_AES == encode_type) {
                byte[] decode_buf = PTUntil.decrypt(in_buf, 5, decode_head.size, private_key);
                if (null == decode_buf) return -3;
                if (0 != head.decode(decode_buf, 0)) return -4;
                return 0;
            } else return -10;
        } catch (Exception e) {
            return -4;
        }
    }
}
