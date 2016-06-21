package com.waylens.hachi.hardware.vdtcamera;

/**
 * Created by Xiaofei on 2016/6/21.
 */
public class SimpleCircularBuffer {
    private final byte[] mBuf;
    private final int mCapacity;

    private int mReadPos;
    private int mWritePos;

    public static SimpleCircularBuffer allocate(int capacity) {
        return new SimpleCircularBuffer(new byte[capacity]);

    }


    private SimpleCircularBuffer(byte[]  bytes) {
        this.mBuf = bytes;
        this.mCapacity = mBuf.length;
        this.mReadPos = 0;
        this.mWritePos = 0;
    }

    public void append(byte[] append, int offset, int size) {
        System.arraycopy(append, offset, mBuf, mWritePos, size);
        mWritePos += size;
    }


    public byte[] readBuf(int size) {
        byte[] ret = new byte[size];
        System.arraycopy(mBuf, mReadPos, ret, 0, size);
        mReadPos += size;
        return ret;
    }

    public int availalbe() {
        return mCapacity - mWritePos;
    }

    public int remaining() {
        return mWritePos - mReadPos;
    }


    public void compact() {
        int remainning = remaining();
        byte[] temp = new byte[remainning];
        System.arraycopy(mBuf, mReadPos, temp, 0, remainning);

        System.arraycopy(temp, 0, mBuf, 0, remainning);

        mReadPos = 0;
        mWritePos = remainning;
    }


    public int peekInt() {
        int result = (int) mBuf[mReadPos] & 0xFF;
        result |= ((int) mBuf[mReadPos + 1] & 0xFF) << 8;
        result |= ((int) mBuf[mReadPos + 2] & 0xFF) << 8;
        result |= ((int) mBuf[mReadPos + 3] & 0xFF) << 8;
        return result;

    }
}
