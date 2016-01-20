package com.waylens.hachi.vdb;

public class ByteStream {

	public static final int readI32(byte[] data, int offset) {
		int result = (int)data[offset] & 0xFF;
		result |= ((int)data[offset + 1] & 0xFF) << 8;
		result |= ((int)data[offset + 2] & 0xFF) << 16;
		result |= ((int)data[offset + 3] & 0xFF) << 24;
		return result;
	}

	public static final long readI64(byte[] data, int offset) {
		int lo = readI32(data, offset);
		int hi = readI32(data, offset + 4);
		return ((long)hi << 32) | ((long)lo & 0xFFFFFFFFL);
	}

	public static final float readFloat(byte[] data, int offset) {
		int bits = readI32(data, offset);
		return Float.intBitsToFloat(bits);
	}

	public static final double readDouble(byte[] data, int offset) {
		long bits = readI64(data, offset);
		return Double.longBitsToDouble(bits);
	}

}
