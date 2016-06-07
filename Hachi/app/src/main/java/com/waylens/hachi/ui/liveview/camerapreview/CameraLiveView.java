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

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }

    private void initView() {
        mHandler = new Handler();

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mMjpegStream = new MyMjpegStream();


    }


    public void startStream(InetSocketAddress serverAddr) {

        mMjpegStream.start(serverAddr);
    }



    public void stopStream() {
        mMjpegStream.stop();
    }


    class MyMjpegStream extends MjpegStream {

        @Override
        protected void onBitmapReadyAsync(MjpegDecoder decoder, MjpegStream stream) {
            BitmapBuffer bb = stream.getOutputBitmapBuffer(decoder);
            if (bb != null) {
                drawBitmap(bb.getBitmap());
            }
        }

        @Override
        protected void onEventAsync(MjpegDecoder decoder, MjpegStream stream) {
        }

        @Override
        protected void onIoErrorAsync(MjpegStream stream, final int error) {

        }

    }




    private void drawBitmap(Bitmap bitmap) {
        if (bitmap == null || mSurfaceHolder == null) {
            return;
        }
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if (canvas == null) {
            return;
        }
        Rect rect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawBitmap(bitmap, null, rect, null);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

}
