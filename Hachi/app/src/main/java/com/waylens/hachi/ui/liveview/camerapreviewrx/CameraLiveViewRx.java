package com.waylens.hachi.ui.liveview.camerapreviewrx;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.waylens.hachi.R;

import java.io.IOException;
import java.net.InetSocketAddress;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Xiaofei on 2016/5/30.
 */
public class CameraLiveViewRx extends FrameLayout {

    private boolean mIsLooping = false;

    @BindView(R.id.mjpeg)
    ImageView ivMjpeg;
    private MjpegReceiverRx mReceiverRx;

    public CameraLiveViewRx(Context context) {
        this(context, null, 0);
    }

    public CameraLiveViewRx(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraLiveViewRx(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View.inflate(context, R.layout.view_camera_preview, this);
        ButterKnife.bind(this);
    }


    public void startStream(final InetSocketAddress address) {
        Observable.create(new Observable.OnSubscribe<Drawable>() {
            @Override
            public void call(Subscriber<? super Drawable> subscriber) {
                mReceiverRx = new MjpegReceiverRx(address);
                MjpegDecoderRx decoderRx = new MjpegDecoderRx();
                try {
                    mReceiverRx.start();
                    mIsLooping = true;

                    while (mIsLooping) {
                        ByteArrayBufferRx buffer = mReceiverRx.readOneFrame();
                        BitmapBufferRx bitmap = decoderRx.decodeOneFrame(buffer);
                        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap.getBitmap());
                        subscriber.onNext(bitmapDrawable);
                    }

                    mReceiverRx.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<Drawable>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Drawable drawable) {
                    ivMjpeg.setImageDrawable(drawable);
                }
            });

    }


    public void stopStream() {
        mIsLooping = false;
    }
}
