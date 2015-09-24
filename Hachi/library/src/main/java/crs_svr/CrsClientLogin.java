
package crs_svr;

import java.io.UnsupportedEncodingException;

import crs_svr.PTUntil;
import crs_svr.IWaylensCode;

public class CrsClientLogin implements IWaylensCode {
    public String device_id;
    public String token;
    public String hash_value;
    public byte device_type; //vidit camera\mobile,etc.

    //comm head  length
    public CrsClientLogin() {
        device_id = "";
        token = "";
        hash_value = "";
        device_type = 0;
    }

    public int encode(byte[] writeBuffer, int iOffset) {
        int ilen = 0, ret = 0;
        if ((device_id.length() + 1) > (writeBuffer.length - iOffset))
            return -1;
        ret = PTUntil.putShortString4Align(writeBuffer, device_id, iOffset);
        if (0 > ret) return (ret - 1);
        ilen += ret;

        if ((token.length() + 1) > (writeBuffer.length - iOffset - ilen))
            return -2;
        ret = PTUntil.putShortString4Align(writeBuffer, token, iOffset + ilen);
        if (0 > ret) return (ret - 2);
        ilen += ret;

        if (32 > (writeBuffer.length - iOffset - ilen))
            return -3;

        try {
            int hl = hash_value.getBytes().length;
            System.arraycopy(hash_value.getBytes(), 0, writeBuffer, iOffset + ilen, hl);
            ilen += hl;
        } catch (Exception e) {
            return -4;
        }
        if (1 > (writeBuffer.length - iOffset - ilen))
            return -5;
        writeBuffer[iOffset + ilen] = device_type;
        ilen += 1;
        return ilen;
    }

    public int decode(byte[] in_buf, int iOffset) {
        int ilen = 0, ret = 0;
        StringBuilder sb = new StringBuilder();
        if (null == sb) return -1;

        ret = PTUntil.getShortString4Align(in_buf, iOffset, sb);
        ilen += ret;
        if (0 >= ret) return ret;
        device_id = sb.toString();
        sb.delete(0, sb.length());

        ret = PTUntil.getShortString4Align(in_buf, iOffset + ilen, sb);
        ilen += ret;
        if (0 >= ret) return (ret - 3);
        token = sb.toString();
        sb.delete(0, sb.length());

        if (in_buf.length < (32 + ilen + iOffset)) return -8;
        try {
            byte[] tmp = new byte[33];
            System.arraycopy(in_buf, iOffset + ilen, tmp, 0, 32);
            ilen += 32;
            hash_value = new String(tmp, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return -9;
        }
        device_type = in_buf[iOffset + ilen];
        ilen += 1;
        return ilen;
    }
}
