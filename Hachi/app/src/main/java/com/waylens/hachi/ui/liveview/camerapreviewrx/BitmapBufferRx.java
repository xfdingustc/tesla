package com.waylens.hachi.ui.liveview.camerapreviewrx;

import android.graphics.Bitmap;



/**
 * Created by Xiaofei on 2016/5/30.
 */
public class BitmapBufferRx {

    protected Manager mManager;
    protected Bitmap mBitmap;

    protected BitmapBufferRx(Manager manager, Bitmap bitmap) {
        mManager = manager;
        mBitmap = bitmap;
    }

    // API
    public final Bitmap getBitmap() {
        return mBitmap;
    }

    // API
    public final void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }


    public void recycle() {
        mManager.recycleObject(this);
    }

    static public class Manager {

        private final BitmapBufferRx[] mArray;
        private final int mMax;

        public Manager(int max) {
            mArray = new BitmapBufferRx[max];
            mMax = max;
        }

        public BitmapBufferRx allocateBitmap() {
            synchronized (this) {
                int i, n = mMax;
                for (i = 0; i < n; i++) {
                    BitmapBufferRx buffer = mArray[i];
                    if (buffer != null) {
                        mArray[i] = null;
                        return buffer;
                    }
                }
            }
            BitmapBufferRx buffer = new BitmapBufferRx(this, null);
            return buffer;
        }

        protected void recycleObject(BitmapBufferRx bb) {
            synchronized (this) {
                int i, n = mMax;
                for (i = 0; i < n; i++) {
                    if (mArray[i] == null) {
                        mArray[i] = bb;
                        return;
                    }
                }
                mArray[0] = bb;
            }
        }

        public void clear() {
            synchronized (this) {
                int i, n = mMax;
                for (i = 0; i < n; i++) {
                    BitmapBufferRx bb = mArray[i];
                    if (bb != null) {
                        if (bb.mBitmap != null) {
                            bb.mBitmap.recycle();
                        }
                        mArray[i] = null;
                    }
                }
            }
        }

    }
}