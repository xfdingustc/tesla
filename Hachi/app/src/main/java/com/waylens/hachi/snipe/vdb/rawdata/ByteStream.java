package com.waylens.hachi.snipe.vdb.rawdata;

class ByteStream {
    private byte[] mData;
    private int mPos;

    public ByteStream(byte[] data) {
        this.mData = data;
        this.mPos = 0;
    }

    public int getLength() {
        return mData.length;
    }

    public int readInt16() {
        int result = (int) mData[mPos] & 0xFF;
        result |= ((int) mData[mPos + 1] & 0xFF) << 8;
        mPos += 2;
        return result;
    }

    public int readInt32() {
        int result = (int) mData[mPos] & 0xFF;
        result |= ((int) mData[mPos + 1] & 0xFF) << 8;
        result |= ((int) mData[mPos + 2] & 0xFF) << 16;
        result |= ((int) mData[mPos + 3] & 0xFF) << 24;
        mPos += 4;
        return result;
    }

    public long readUint32() {
        int firstByte = (0x000000FF & mData[mPos++]);
        int secondByte = (0x000000FF & mData[mPos++]);
        int thirdByte = (0x000000FF & mData[mPos++]);
        int fourthByte = (0x000000FF & mData[mPos++]);

        return ((long) (firstByte | secondByte << 8 | thirdByte << 16 | fourthByte << 24)) & 0xFFFFFFFFL;
    }



    public long readInt64() {
        int lo = readInt32();
        int hi = readInt32();
        return ((long) hi << 32) | ((long) lo & 0xFFFFFFFFL);
    }

    public float readFloat() {
        int bits = readInt32();
        return Float.intBitsToFloat(bits);
    }

    public double readDouble() {
        long bits = readInt64();
        return Double.longBitsToDouble(bits);
    }

}
