package crs_svr;

public class EncodeCommHead {
    public short size;
    public short encode_type; //enum CRE_ENCODE_TYPE; only support aes now

    public EncodeCommHead() {
        size = 0;
        encode_type = 0;
    }

    public int encode(byte[] writeBuffer, int offset) {
        if (writeBuffer.length < 3)
            return -1;
        PTUntil.putShortlh(writeBuffer, size, offset);
        PTUntil.putShortlh(writeBuffer, encode_type, offset + 2);
        return 0;
    }

    public int decode(byte[] in_buf, int in_buf_size) {
        if (in_buf_size < 3) {
            System.out.println("in_buf is error ,in_buf len:" + in_buf.length + "class len:" + this.size);
            return -1;
        } else {
            size = PTUntil.getShortlh(in_buf, 0);
            encode_type = PTUntil.getShortlh(in_buf, 2);
            return 0;
        }
    }
}
