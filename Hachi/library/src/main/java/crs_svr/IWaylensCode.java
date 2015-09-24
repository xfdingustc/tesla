
package crs_svr;

public interface IWaylensCode {
    int encode(byte[] writeBuf, int iOffset);
    int decode(byte[] readBuf, int iOffset);
}
