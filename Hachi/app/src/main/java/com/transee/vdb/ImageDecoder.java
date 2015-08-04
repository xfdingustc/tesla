package com.transee.vdb;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.LinkedList;

// TODO - recycle bitmaps
public class ImageDecoder extends Thread {

	static public final boolean CORNER = false;

	public interface Callback {
		void onDecodeDoneAsync(Bitmap bitmap, Object tag);
	}

	public static class Request {

		public final byte[] mData;
		public final int mWidth;
		public final int mHeight;
		public final int mCorner;
		public final Object mTag;
		public final Callback mCallback;

		public Request(byte[] data, int width, int height, int corner, Object tag, Callback callback) {
			mData = data;
			mWidth = width;
			mHeight = height;
			mCorner = corner;
			mTag = tag;
			mCallback = callback;
		}
	}

	private final BitmapFactory.Options mOptions;
	private boolean mbThreadWaiting;
	private LinkedList<Request> mRequestQueue;

	public ImageDecoder() {
		super("ImageDecoder");
		mOptions = new BitmapFactory.Options();
		mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
		mOptions.inMutable = true; // TODO
		mRequestQueue = new LinkedList<Request>();
	}

	synchronized private void addRequest(Request request) {
		mRequestQueue.addLast(request);
		if (mbThreadWaiting) {
			mbThreadWaiting = false;
			notifyAll();
		}
	}

	// API
	public void decode(byte[] data, int width, int height, int corner, Object tag, Callback callback) {
		if (!CORNER) {
			corner = 0;
		}
		Request request = new Request(data, width, height, corner, tag, callback);
		addRequest(request);
	}

	synchronized private Request getRequest() throws InterruptedException {
		while (true) {
			if (mRequestQueue.size() > 0) {
				return mRequestQueue.removeFirst();
			}
			mbThreadWaiting = true;
			wait();
		}
	}

	@Override
	public void run() {
		try {
			while (!interrupted()) {
				Request request = getRequest();
				byte[] data = request.mData;
				calcBitmapSize(mOptions, data, request.mWidth, request.mHeight);
				Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, mOptions);
				if (request.mCorner > 0) {
					bitmap = drawCorner(bitmap, request.mCorner);
				}
				if (request.mCallback != null) {
					request.mCallback.onDecodeDoneAsync(bitmap, request.mTag);
				}
			}
		} catch (InterruptedException e) {

		}
	}

	private Bitmap drawCorner(Bitmap bitmap, int corner) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(Color.BLACK);
		canvas.drawRoundRect(new RectF(0, 0, width, height), corner, corner, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, null, new Rect(0, 0, width, height), paint);

		bitmap.recycle();
		return output;
	}

	public static void calcBitmapSize(BitmapFactory.Options options, byte[] data, int width, int height) {
		options.inSampleSize = 1;
		if (width > 0 && height > 0) {
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, options);
			options.inJustDecodeBounds = false;
			options.inSampleSize = calcSampleSize(options.outWidth, options.outHeight, width, height);
		}
	}

	// picWidth / (1<<result) > width
	// picHeight / (1<<result) > height
	public static int calcSampleSize(int picWidth, int picHeight, int width, int height) {
		int inSampleSizePrev = 1;
		int inSampleSize = 1;
		for (;; inSampleSize <<= 1) {
			if (width * inSampleSize > picWidth && height * inSampleSize > picHeight) {
				return inSampleSizePrev;
			}
			inSampleSizePrev = inSampleSize;
		}
	}
}
