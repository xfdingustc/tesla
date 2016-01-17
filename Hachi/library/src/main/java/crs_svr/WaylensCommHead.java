package crs_svr;

public class WaylensCommHead {
    public short size;    //packet size
    public short cmd;    //command word
    public int version;//the version number

    //comm head  length
    public static final int COMM_HEAD_LEGTH = 8;    //login server

    public WaylensCommHead() {
        size = 0;
        cmd = 0;
        version = 0;
    }

    public int encode(byte[] writeBuffer, int offset) {
        if (writeBuffer.length < 8)
            return -1;
        PTUntil.putShortlh(writeBuffer, size, offset);
        PTUntil.putShortlh(writeBuffer, cmd, offset + 2);
        PTUntil.putIntlh(writeBuffer, version, offset + 4);
        return 0;
    }

    public int decode(byte[] in_buf, int offset) {
        if (in_buf.length < 8) {
            System.out.println("in_buf is error ,in_buf len:" + in_buf.length);
            return -1;
        } else {
            size = PTUntil.getShortlh(in_buf, offset);
            cmd = PTUntil.getShortlh(in_buf, offset + 2);
            version = PTUntil.getIntlh(in_buf, offset + 4);
            return 0;
        }
    }
}
