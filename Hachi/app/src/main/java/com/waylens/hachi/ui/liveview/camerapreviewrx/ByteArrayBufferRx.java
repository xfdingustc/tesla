package com.waylens.hachi.ui.liveview.camerapreviewrx;



/**
 * Created by Xiaofei on 2016/5/30.
 */
public class ByteArrayBufferRx {
    protected Manager mManager;
    protected byte[] mBuffer;

    protected ByteArrayBufferRx(Manager manager, byte[] buffer) {
        mManager = manager;
        mBuffer = buffer;
    }


    public void recycle() {
        mManager.recycleObject(this);
    }

    public final void setBuffer(byte[] buffer) {
        mBuffer = buffer;
    }

    public final byte[] getBuffer() {
        return mBuffer;
    }

    static public class Manager {

        private final ByteArrayBufferRx[] mArray;
        private final int mMax;

        int mAllocated;
        int mRecycled;

        public Manager(int max) {
            mArray = new ByteArrayBufferRx[max];
            mMax = max;
        }

        public ByteArrayBufferRx allocateBuffer(int size) {
            synchronized (this) {
                mAllocated++;
                int i, n = mMax;
                for (i = 0; i < n; i++) {
                    ByteArrayBufferRx buf = mArray[i];
                    if (buf != null && buf.getBuffer().length >= size) {
                        mArray[i] = null;
                        return buf;
                    }
                }
            }
            byte[] buffer = new byte[size + 4 * 1024];
            return new ByteArrayBufferRx(this, buffer);
        }

        protected void recycleObject(ByteArrayBufferRx object) {
            synchronized (this) {
                mRecycled++;
                int i, n = mMax, index = 0, min_size = Integer.MAX_VALUE;
                for (i = 0; i < n; i++) {
                    if (mArray[i] == null) {
                        index = i;
                        break;
                    }
                    int size = mArray[i].getBuffer().length;
                    if (size < min_size) {
                        min_size = size;
                        index = i;
                    }
                }
                if (mArray[index] == null || mArray[index].getBuffer().length < object.getBuffer().length) {
                    mArray[index] = object;
                }
            }
        }
    }
}
