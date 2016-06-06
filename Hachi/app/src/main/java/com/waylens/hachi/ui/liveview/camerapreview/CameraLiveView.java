package com.waylens.hachi.ui.liveview.camerapreview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.waylens.hachi.eventbus.events.CameraConnectionEvent;
import com.waylens.hachi.hardware.vdtcamera.VdtCamera;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;

public class CameraLiveView extends SurfaceView implements SurfaceHolder.Callback {

    private Handler mHandler;

    private MjpegStream mMjpegStream;


    private SurfaceHolder mSurfaceHolder;

    private WeakReference<VdtCamera> mCameraRef;

    private DrawBitmapThread mDrawBitmapThread;


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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        mDrawBitmapThread.start();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private void initView() {
        mHandler = new Handler();

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mMjpegStream = new MyMjpegStream();
        mDrawBitmapThread = new DrawBitmapThread();

    }




    public void startStream(InetSocketAddress serverAddr) {

        mMjpegStream.start(serverAddr);
    }

    public void bindCamera(VdtCamera camera) {
        mCameraRef = new WeakReference<VdtCamera>(camera);
    }

    public void stopStream() {
        mMjpegStream.stop();
    }

    private Bitmap getCurrentBitmap() {
        Bitmap bitmap = null;
        BitmapBuffer bb = mMjpegStream.getOutputBitmapBuffer(null);
        if (bb != null) {
            bitmap = bb.getBitmap();


        }
        return bitmap;
    }

//    @Override
//    protected void onDraw(Canvas canvas) {
//        Bitmap bitmap = getCurrentBitmap();
//
//        if (bitmap != null) {
//            Rect rect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
//            canvas.drawBitmap(bitmap, null, rect, null);
//        }
//
//        mHandler.post(mDrawBitmapAction);
//    }


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


    private class DrawBitmapThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                Bitmap bitmap = getCurrentBitmap();
                if (bitmap != null) {
                    drawBitmap(bitmap);
                }
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void drawBitmap(Bitmap bitmap) {
        Canvas canvas = mSurfaceHolder.lockCanvas();
        Rect rect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmap, null, rect, null);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

}
