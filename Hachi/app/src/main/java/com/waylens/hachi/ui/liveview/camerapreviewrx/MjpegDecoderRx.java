package com.waylens.hachi.ui.liveview.camerapreviewrx;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;



/**
 * Created by Xiaofei on 2016/5/30.
 */
public class MjpegDecoderRx {

    protected final BitmapFactory.Options mOptions;
    private final BitmapBufferRx.Manager mBitmapManager;

    public MjpegDecoderRx() {
        mOptions = new BitmapFactory.Options();
        mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        mBitmapManager = new BitmapBufferRx.Manager(2);
    }

    public BitmapBufferRx decodeOneFrame(ByteArrayBufferRx buffer) {


        byte[] data = buffer.getBuffer();
        BitmapBufferRx bb = mBitmapManager.allocateBitmap();

        mOptions.inMutable = true;
        mOptions.inSampleSize = 1;

        if (bb.mBitmap != null)

        {
            // using the old bitmap; need to check if sizes match
            mOptions.inJustDecodeBounds = true;
            mOptions.inBitmap = null;
            BitmapFactory.decodeByteArray(data, 0, data.length, mOptions);
            mOptions.inJustDecodeBounds = false;
            if (mOptions.outWidth != bb.mBitmap.getWidth() || mOptions.outHeight != bb.mBitmap.getHeight()) {
                mOptions.inBitmap = null;
            } else {
                mOptions.inBitmap = bb.mBitmap;
            }
        } else

        {
            mOptions.inBitmap = null;
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, mOptions);
        buffer.recycle();
        bb.setBitmap(bitmap);

        return bb;
    }
}
