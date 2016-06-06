package com.waylens.hachi.ui.liveview.camerapreview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;

public class CameraLiveView extends View {
    private final int MAX_STATES = 4;
    private int mMaskCounter;

    private Handler mHandler;

    private MjpegStream mMjpegStream;
    private Bitmap mMaskedBitmap;
    private BitmapCanvas mBitmapCanvas;

    private Drawable[] mStates = new Drawable[MAX_STATES];

    private WeakReference<VdtCamera> mCameraRef;


    private final Runnable mDrawBitmapAction = new Runnable() {
        @Override
        public void run() {

            invalidate();
        }
    };


    public CameraLiveView(Context context) {
        super(context);
        initView();
    }

    public CameraLiveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CameraLiveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        mHandler = new Handler();

        mMjpegStream = new MyMjpegStream();

        mBitmapCanvas = new BitmapCanvas(this) {
            @Override
            public void invalidate() {
                mHandler.post(mDrawBitmapAction);
            }

            @Override
            public void invalidateRect(int left, int top, int right, int bottom) {
                mHandler.post(mDrawBitmapAction);
            }
        };

    }


    public void startStream(InetSocketAddress serverAddr) {

        mMjpegStream.start(serverAddr);
    }

    public void bindCamera(VdtCamera camera) {
        mCameraRef = new WeakReference<VdtCamera>(camera);
    }

    public void stopStream() {
        mMjpegStream.stop();
        if (mMaskedBitmap != null) {
            mMaskedBitmap.recycle();
            mMaskedBitmap = null;
        }
    }

    private Bitmap getCurrentBitmap() {
        Bitmap bitmap = null;
        BitmapBuffer bb = mMjpegStream.getOutputBitmapBuffer(null);
        if (bb != null) {
            bitmap = bb.getBitmap();
        }
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bitmap = getCurrentBitmap();


        mBitmapCanvas.drawBitmap(canvas, bitmap, null);
    }


    class MyMjpegStream extends MjpegStream {

        @Override
        protected void onBitmapReadyAsync(MjpegDecoder decoder, MjpegStream stream) {
            BitmapBuffer bb = stream.getOutputBitmapBuffer(decoder);
            if (bb != null) {
                mHandler.post(mDrawBitmapAction);
            }
        }

        @Override
        protected void onEventAsync(MjpegDecoder decoder, MjpegStream stream) {
        }

        @Override
        protected void onIoErrorAsync(MjpegStream stream, final int error) {
            if (mCameraRef == null) {
                return;
            }
            VdtCamera camera = mCameraRef.get();
            if (camera != null) {
                camera.onPreviewSocketDisconnect();
            }
        }

    }

//

}
