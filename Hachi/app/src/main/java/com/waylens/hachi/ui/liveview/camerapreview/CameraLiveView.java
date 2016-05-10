package com.waylens.hachi.ui.liveview.camerapreview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.net.InetSocketAddress;

public class CameraLiveView extends View {
	public interface Callback {
		void onSingleTapUp();
		void onIoErrorAsync(int error);
	}

	private final int MAX_STATES = 4;
	private int mMaskCounter;

	private Handler mHandler;
	private Callback mCallback;
	private MjpegStream mMjpegStream;
	private Bitmap mMaskedBitmap;
	private BitmapCanvas mBitmapCanvas;

	private Drawable[] mStates = new Drawable[MAX_STATES];


	private final Runnable mDrawBitmapAction = new Runnable() {
		@Override
		public void run() {
			// TODO - optimize
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

	@Override
	public void setBackgroundColor(int color) {
		mBitmapCanvas.setBackgroundColor(color);
	}

	// API


	// API
	public final void startStream(InetSocketAddress serverAddr, Callback callback, boolean bLoop) {
		mCallback = callback;
		setVisibility(View.VISIBLE);
		mMjpegStream.start(serverAddr, bLoop);
	}

	// API
	public final void stopStream() {
		mMjpegStream.stop();
		if (mMaskedBitmap != null) {
			mMaskedBitmap.recycle();
			mMaskedBitmap = null;
		}
		setVisibility(View.INVISIBLE);
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
		boolean bMasked = false;
		if (mMaskCounter > 0) {
			mMaskCounter--;
			bMasked = true;
		} else if (bitmap == mMaskedBitmap) {
			bMasked = true;
		} else {
			mMaskedBitmap = null;
		}
		mBitmapCanvas.drawBitmap(canvas, bitmap, bMasked, mStates);
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
			if (mCallback != null) {
				mCallback.onIoErrorAsync(error);
			}
		}

	}

//

}
