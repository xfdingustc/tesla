
package crs_svr;

import crs_svr.PTUntil;
import crs_svr.IWaylensCode;

public class CrsClientStartUpload implements IWaylensCode {
    public String device_id;
    public String device_model;
    public String token;
    public byte[] file_sha1;    //upload file sha1
    public int data_type;    //raw\video\picture
    public long file_size;
    public int upload_type; //live data\history data
    public int duration; //only for mp4, the mp4 total time(unit:second)

    public CrsClientStartUpload() {
        device_id = "";
        device_model = "";
        token = "";
        file_sha1 = new byte[20];
        data_type = 0;
        file_size = 0;
        upload_type = 0;
        duration = 0;
    }

    public int encode(byte[] writeBuffer, int iOffset) {
        int ilen = 0, ret = 0;
        ret = PTUntil.putShortString4Align(writeBuffer, device_id, iOffset);
        ilen += ret;
        if (0 >= ret) return ret;
        ret = PTUntil.putLongString4Align(writeBuffer, device_model, iOffset + ilen);
        ilen += ret;
        if (0 >= ret) return ret - 3;
        ret = PTUntil.putShortString4Align(writeBuffer, token, iOffset + ilen);
        ilen += ret;
        if (0 >= ret) return ret - 6;
        if (writeBuffer.length < (iOffset + ilen + 34)) return -7;
        System.arraycopy(file_sha1, 0, writeBuffer, iOffset + ilen, 20);
        ilen += 20;
        PTUntil.putIntlh(writeBuffer, data_type, iOffset + ilen);
        ilen += 4;
        PTUntil.putLonglh(writeBuffer, file_size, iOffset + ilen);
        ilen += 8;
        PTUntil.putIntlh(writeBuffer, upload_type, iOffset + ilen);
        ilen += 4;
        PTUntil.putIntlh(writeBuffer, duration, iOffset + ilen);
        ilen += 4;
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

        ret = PTUntil.getLongString4Align(in_buf, iOffset + ilen, sb);
        ilen += ret;
        if (0 >= ret) return ret - 3;
        device_model = sb.toString();
        sb.delete(0, sb.length());

        ret = PTUntil.getShortString4Align(in_buf, iOffset + ilen, sb);
        ilen += ret;
        if (0 >= ret) return ret - 6;
        token = sb.toString();
        sb.delete(0, sb.length());

        if (in_buf.length < (iOffset + ilen + 34)) return -7;

        System.arraycopy(in_buf, iOffset + ilen, file_sha1, 0, 20);
        ilen += 20;
        data_type = PTUntil.getIntlh(in_buf, iOffset + ilen);
        ilen += 4;
        file_size = PTUntil.getLonglh(in_buf, iOffset + ilen);
        ilen += 8;
        upload_type = PTUntil.getIntlh(in_buf, iOffset + ilen);
        ilen += 4;
        duration = PTUntil.getIntlh(in_buf, iOffset + ilen);
        ilen += 4;
        return ilen;
    }
}
