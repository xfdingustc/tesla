
package crs_svr;

import crs_svr.PTUntil;
import crs_svr.IWaylensCode;
import crs_svr.ProtocolConstMsg;

public class CrsClientTranData implements IWaylensCode {
    public String device_id;
    public String token;
    public byte[] file_sha1;
    public byte[] block_sha1;
    public int upload_type; //live data\history data
    public int data_type;    //raw\video\picture
    public int seq_num;
    public int block_num; //the last 16 bit block_num, the top 2 bit indicate: 00 not split packet,
    //01 the first packet, 10 middle packet, 11 the last packet;
    //the 3 bit indicate:0 the next 13 bit reserved,1 the next 4 bit duration times,other 9 bit reserved
    public short length;

    public byte[] buf;

    public CrsClientTranData() {
        device_id = "";
        token = "";
        file_sha1 = new byte[20];
        block_sha1 = new byte[20];
        upload_type = 0;
        data_type = 0;
        seq_num = 0;
        block_num = 0;
        length = 0;
        buf = new byte[ProtocolConstMsg.CAM_TRAN_BLOCK_SIZE];
    }

    public int encode(byte[] writeBuffer, int iOffset) {
        int ilen = 0, ret = 0;
        ret = PTUntil.putShortString4Align(writeBuffer, device_id, iOffset);
        ilen += ret;
        if (0 >= ret) return ret;
        ret = PTUntil.putShortString4Align(writeBuffer, token, iOffset + ilen);
        ilen += ret;
        if (0 >= ret) return ret - 3;

        if (writeBuffer.length < (iOffset + ilen + 52)) return -7;

        System.arraycopy(file_sha1, 0, writeBuffer, iOffset + ilen, 20);
        ilen += 20;
        System.arraycopy(block_sha1, 0, writeBuffer, iOffset + ilen, 20);
        ilen += 20;
        PTUntil.putIntlh(writeBuffer, upload_type, iOffset + ilen);
        ilen += 4;
        PTUntil.putIntlh(writeBuffer, data_type, iOffset + ilen);
        ilen += 4;
        PTUntil.putIntlh(writeBuffer, seq_num, iOffset + ilen);
        ilen += 4;
        PTUntil.putIntlh(writeBuffer, block_num, iOffset + ilen);
        ilen += 4;
        PTUntil.putShortlh(writeBuffer, length, iOffset + ilen);
        ilen += 2;
        if (writeBuffer.length < (iOffset + ilen + length)) return -8;
        System.arraycopy(buf, 0, writeBuffer, iOffset + ilen, length);
        ilen += length;
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

        if (in_buf.length < (iOffset + ilen + 52)) return -7;

        System.arraycopy(in_buf, iOffset + ilen, file_sha1, 0, 20);
        ilen += 20;
        System.arraycopy(in_buf, iOffset + ilen, block_sha1, 0, 20);
        ilen += 20;
        upload_type = PTUntil.getIntlh(in_buf, iOffset + ilen);
        ilen += 4;
        data_type = PTUntil.getIntlh(in_buf, iOffset + ilen);
        ilen += 4;
        seq_num = PTUntil.getIntlh(in_buf, iOffset + ilen);
        ilen += 4;
        block_num = PTUntil.getIntlh(in_buf, iOffset + ilen);
        ilen += 4;
        length = PTUntil.getShortlh(in_buf, iOffset + ilen);
        ilen += 2;
        if (length > ProtocolConstMsg.CAM_TRAN_BLOCK_SIZE || in_buf.length < (iOffset + ilen + length)) {
            System.out.println("length is error ,length:" + length);
            return -8;
        }
        System.arraycopy(in_buf, iOffset + ilen, buf, 0, length);
        ilen += length;
        return ilen;
    }
}
