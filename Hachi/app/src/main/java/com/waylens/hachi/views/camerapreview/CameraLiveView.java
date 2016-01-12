package com.waylens.hachi.views.camerapreview;

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
	private GestureDetector mGesture;
	private Drawable[] mStates = new Drawable[MAX_STATES];
	private int[] mResId = new int[MAX_STATES];

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
		setVisibility(View.INVISIBLE);
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

		mGesture = new GestureDetector(getContext(), new MyGestureListener());

		for (int i = 0; i < mResId.length; i++) {
			mResId[i] = -1;
		}
	}

	@Override
	public void setBackgroundColor(int color) {
		mBitmapCanvas.setBackgroundColor(color);
	}

	// API
	public final void setAnchorRect(int leftMargin, int topMargin, int rightMargin, int bottomMargin, float bestZoom) {
		mBitmapCanvas.setAnchorRect(leftMargin, topMargin, rightMargin, bottomMargin, bestZoom);
	}

	public void setAnchorTopThumbnail(int anchorTopThumbnail) {
		mBitmapCanvas.setAnchorTopThumbnail(anchorTopThumbnail);
	}

	// API
	public int getStateResId(int index) {
		return mResId[index];
	}

	// API
	public void setState(int index, int resId, Drawable drawable) {
		mStates[index] = drawable;
		mResId[index] = resId;
	}

	// API
	public final void setThumbnailScale(int thumbnailScale) {
		mBitmapCanvas.setThumbnailScale(thumbnailScale);
	}

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

	public void startMask(int frames) {
		if (mMaskCounter != frames) {
			mMaskCounter = frames;
			if (frames > 0) {
				mMaskedBitmap = getCurrentBitmap();
				invalidate();
			}
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// Log.d(TAG, "dispatchTouchEvent");
		boolean handled = super.dispatchTouchEvent(ev);
		handled |= mBitmapCanvas.dispatchTouchEvent(ev, handled, mGesture);
		return handled;
	}

	private void onDown(MotionEvent e) {
		mBitmapCanvas.onDown(e);
		if (mCallback != null) {
		}
	}

	private void onSingleTapUp(MotionEvent e) {
		if (mBitmapCanvas.isDoubleClick(e)) {
			mBitmapCanvas.scale(e, false);
		} else {
			if (mCallback != null) {
				mCallback.onSingleTapUp();
			}
		}
	}

	private void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		mBitmapCanvas.scroll(e1, e2, distanceX, distanceY);
	}

	private void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		mBitmapCanvas.fling(e1, e2, velocityX, velocityY);
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

	private class MyGestureListener implements GestureDetector.OnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			CameraLiveView.this.onDown(e);
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			CameraLiveView.this.onSingleTapUp(e);
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			CameraLiveView.this.onScroll(e1, e2, distanceX, distanceY);
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			CameraLiveView.this.onFling(e1, e2, velocityX, velocityY);
			return true;
		}

	}

}
