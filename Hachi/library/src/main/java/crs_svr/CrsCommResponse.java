
package crs_svr;

import crs_svr.PTUntil;
import crs_svr.IWaylensCode;

public class CrsCommResponse implements IWaylensCode
{
	public String device_id;
	public String token;
	public long offset;
	public short ret;

	 public CrsCommResponse()
	 {
		 device_id = "";
		 token = "";
		 offset = 0;
		 ret = 0;
	 }

	 public int encode(byte [] writeBuffer, int iOffset)
	 {
		int ilen = 0, rt = 0;
		rt = PTUntil.putShortString4Align(writeBuffer, device_id, iOffset); ilen += rt;
		if(0 >= rt) return rt;
		rt = PTUntil.putShortString4Align(writeBuffer, token, iOffset+ ilen); ilen += rt;
		if(0 >= rt) return rt -3;
		if(writeBuffer.length < (iOffset + ilen + 10)) return -5;
		PTUntil.putLong(writeBuffer, offset, iOffset+ ilen); ilen += 8;
		PTUntil.putShortlh(writeBuffer, ret, iOffset+ ilen); ilen += 2;
		return ilen;
	 }

	 public int decode(byte[] in_buf, int iOffset)
	 {
		 int ilen = 0, rt = 0;
		 StringBuilder sb = new StringBuilder();
		 rt = PTUntil.getShortString4Align(in_buf, iOffset, sb); ilen += rt;
		 if(0 >= rt) return rt;
		 device_id = sb.toString(); sb.delete(0, sb.length());
		 rt = PTUntil.getShortString4Align(in_buf, iOffset+ ilen, sb); ilen += rt;
		 if(0 >= rt) return rt - 3;
		 token = sb.toString(); sb.delete(0, sb.length());
		 if(in_buf.length < (iOffset + ilen + 10)) return -5;
		 offset = PTUntil.getLong(in_buf, iOffset+ ilen); ilen += 8;
		 ret = PTUntil.getShortlh(in_buf, iOffset+ ilen); ilen += 2;
		 return ilen;
	 }
}
