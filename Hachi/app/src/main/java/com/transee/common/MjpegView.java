package com.transee.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.net.InetSocketAddress;

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {

	public interface Callback {
		public void onDown();

		public void onSingleTapUp();

		public void onIoErrorAsync(int error);
	}

	private static final boolean DEBUG = false;
	private static final String TAG = "MjpegView";

	private final int MAX_STATES = 4;

	private MjpegStream mMjpegStream;
	private Callback mCallback;

	long mStartTime;
	long mTotalFrames;

	private int mMaskCounter;

	// need synchronize
	private boolean mbSurfaceReady;
	private BitmapCanvas mBitmapCanvas;
	private GestureDetector mGesture;

	private Drawable[] mStates = new Drawable[MAX_STATES];

	public MjpegView(Context context) {
		super(context);
		initView();
	}

	public MjpegView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public MjpegView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		setVisibility(View.INVISIBLE);

		getHolder().setFormat(PixelFormat.RGBA_8888);

		mMjpegStream = new MyMjpegStream();

		mBitmapCanvas = new BitmapCanvas(this) {
			@Override
			public void invalidate() {
				mMjpegStream.postEvent(0, 15);
			}

			@Override
			public void invalidateRect(int left, int top, int right, int bottom) {
				mMjpegStream.postEvent(0, 15);
			}
		};

		mGesture = new GestureDetector(getContext(), new MyGestureListener());
	}

	// API
	public void setState(int index, Drawable drawable) {
		synchronized (this) {
			mStates[index] = drawable;
		}
	}

	// API
	public final void setThumbnailScale(int thumbnailScale) {
		synchronized (this) {
			mBitmapCanvas.setThumbnailScale(thumbnailScale);
		}
	}

	// API
	public final void startStream(InetSocketAddress serverAddr, Callback callback) {
		mCallback = callback;
		mStartTime = SystemClock.uptimeMillis();
		mTotalFrames = 0;
		getHolder().addCallback(this);
		setVisibility(View.VISIBLE);
		mMjpegStream.start(serverAddr, true);
	}

	// API
	public final void stopStream() {
		mMjpegStream.stop();
		setVisibility(View.INVISIBLE);
		getHolder().removeCallback(this);
		if (DEBUG) {
			long duration = SystemClock.uptimeMillis() - mStartTime;
			Log.d(TAG, "frames=" + mTotalFrames + ", duration=" + (duration / 1000) + ", fps: " + (double) mTotalFrames
					* 1000 / duration);
		}
	}

	// API
	public final void setAnchorRect(int leftMargin, int topMargin, int rightMargin, int bottomMargin, float bestZoom) {
		if (DEBUG) {
			Log.d(TAG, "setMargins: " + leftMargin + "," + topMargin + "," + rightMargin + "," + bottomMargin + ";"
					+ bestZoom);
		}
		synchronized (this) {
			mBitmapCanvas.setAnchorRect(leftMargin, topMargin, rightMargin, bottomMargin, bestZoom);
		}
	}

	// API
	public final void startMask(int counter) {
		synchronized (this) {
			mMaskCounter = counter;
		}
	}

	private final void drawBitmapSync(Bitmap bitmap) {
		if (mbSurfaceReady) {
			SurfaceHolder holder = getHolder();
			Canvas canvas = holder.lockCanvas();
			if (canvas != null) {
				if (mMaskCounter > 0) {
					mMaskCounter--;
					bitmap = null; // force black screen
				}
				mBitmapCanvas.drawBitmap(canvas, bitmap, false, mStates);
				holder.unlockCanvasAndPost(canvas);
			}
		}
	}

	// called from jpeg decoder thread
	private final void drawBitmap(BitmapBuffer bb) {
		synchronized (this) {
			drawBitmapSync(bb == null ? null : bb.mBitmap);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// Log.d(TAG, "surfaceCreated");
		synchronized (this) {
			mbSurfaceReady = true;
			BitmapBuffer bb = mMjpegStream.getOutputBitmapBuffer(null);
			drawBitmapSync(bb == null ? null : bb.mBitmap);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// Log.d(TAG, "surfaceChanged");
		drawBitmap(mMjpegStream.getOutputBitmapBuffer(null));
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Log.d(TAG, "surfaceDestroyed");
		synchronized (this) {
			mbSurfaceReady = false;
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// Log.d(TAG, "dispatchTouchEvent");
		boolean handled = super.dispatchTouchEvent(ev);
		synchronized (this) {
			handled |= mBitmapCanvas.dispatchTouchEvent(ev, handled, mGesture);
		}
		return handled;
	}

	// move
	private void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		synchronized (this) {
			mBitmapCanvas.scroll(e1, e2, distanceX, distanceY);
		}
	}

	// fling
	private void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		synchronized (this) {
			mBitmapCanvas.fling(e1, e2, velocityX, velocityY);
		}
	}

	private void onDown(MotionEvent e) {
		synchronized (this) {
			mBitmapCanvas.onDown(e);
		}
		if (mCallback != null) {
			// TODO
		}
	}

	private void onSingleTapUp(MotionEvent e) {
		if (mBitmapCanvas.isDoubleClick(e)) {
			synchronized (this) {
				mBitmapCanvas.scale(e, false);
			}
		} else {
			if (mCallback != null) {
				mCallback.onSingleTapUp();
			}
		}
	}

	class MyMjpegStream extends MjpegStream {

		@Override
		protected void onBitmapReadyAsync(MjpegDecoder decoder, MjpegStream stream) {
			BitmapBuffer bb = stream.getOutputBitmapBuffer(decoder);
			if (bb != null) {
				drawBitmap(bb);
				mTotalFrames++;
			}
		}

		@Override
		protected void onEventAsync(MjpegDecoder decoder, MjpegStream stream) {
			BitmapBuffer bb = stream.getOutputBitmapBuffer(decoder);
			if (bb != null) {
				drawBitmap(bb);
			}
		}

		@Override
		protected void onIoErrorAsync(MjpegStream stream, int error) {
			if (mCallback != null) {
				mCallback.onIoErrorAsync(error);
			}
		}

	}

	private class MyGestureListener implements GestureDetector.OnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			MjpegView.this.onDown(e);
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			MjpegView.this.onSingleTapUp(e);
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			MjpegView.this.onScroll(e1, e2, distanceX, distanceY);
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			MjpegView.this.onFling(e1, e2, velocityX, velocityY);
			return true;
		}

	}

}
