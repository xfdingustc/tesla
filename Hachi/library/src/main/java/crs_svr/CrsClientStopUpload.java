
package crs_svr;

import crs_svr.PTUntil;
import crs_svr.IWaylensCode;

public class CrsClientStopUpload implements IWaylensCode {
    public String device_id;
    public String token;

    public CrsClientStopUpload(String deviceID, String token) {
        device_id = deviceID;
        this.token = token;
    }

    public int encode(byte[] writeBuffer, int iOffset) {
        int ilen = 0, ret = 0;
        ret = PTUntil.putShortString4Align(writeBuffer, device_id, iOffset);
        ilen += ret;
        if (0 >= ret) return ret;
        ret = PTUntil.putShortString4Align(writeBuffer, token, iOffset + ilen);
        ilen += ret;
        if (0 >= ret) return ret - 3;
        return ilen;
    }

    public int decode(byte[] in_buf, int iOffset) {
        int ilen = 0, ret = 0;
        StringBuilder sb = new StringBuilder();
        ret = PTUntil.getShortString4Align(in_buf, iOffset, sb);
        ilen += ret;
        if (0 >= ret) return ret;
        device_id = sb.toString();
        sb.delete(0, sb.length());

        ret = PTUntil.getShortString4Align(in_buf, iOffset + ilen, sb);
        ilen += ret;
        if (0 >= ret) return ret - 3;
        token = sb.toString();
        sb.delete(0, sb.length());

        return ilen;
    }
}
