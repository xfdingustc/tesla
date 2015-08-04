package com.transee.ccam;

import android.graphics.Rect;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.transee.common.IRecyclable;
import com.transee.common.SimpleQueue;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

abstract public class BarcodeDecoder extends Thread {

	static final boolean DEBUG = false;
	static final String TAG = "BarcodeDecoder";

	static class DecodeCommand implements IRecyclable {

		byte[] mData;
		int mDataWidth;
		int mDataHeight;
		int mLeft;
		int mTop;
		int mWidth;
		int mHeight;

		public DecodeCommand(byte data[], int dataWidth, int dataHeight, Rect rect) {
			mData = data;
			mDataWidth = dataWidth;
			mDataHeight = dataHeight;
			mLeft = rect.left;
			mTop = rect.top;
			mWidth = rect.width();
			mHeight = rect.height();
		}

		@Override
		public void recycle() {
		}
	}

	abstract public void onDecodeError(BarcodeDecoder decoder);

	abstract public void onDecodeDone(BarcodeDecoder decoder, String result);

	private final SimpleQueue<DecodeCommand> mCommandQueue;
	private final Map<DecodeHintType, Object> mHints;
	private final MultiFormatReader mMultiFormatReader;

	public BarcodeDecoder() {
		mCommandQueue = new SimpleQueue<DecodeCommand>();

		mHints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);

		Collection<BarcodeFormat> formats = EnumSet.noneOf(BarcodeFormat.class);
		formats.addAll(EnumSet.of(BarcodeFormat.QR_CODE));
		mHints.put(DecodeHintType.POSSIBLE_FORMATS, formats);

		mMultiFormatReader = new MultiFormatReader();
		mMultiFormatReader.setHints(mHints);
	}

	// API
	public void requestDecode(byte data[], int dataWidth, int dataHeight, Rect rect) {
		DecodeCommand cmd = new DecodeCommand(data, dataWidth, dataHeight, rect);
		mCommandQueue.putObject(cmd);
	}

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				DecodeCommand cmd = mCommandQueue.waitForObject();
				decode(cmd);
			}
		} catch (InterruptedException e) {
			if (DEBUG) {
				Log.d(TAG, "interrupted");
			}
		}
	}

	private void decode(DecodeCommand cmd) {

		if (DEBUG) {
			Log.d(TAG, "data: " + cmd.mDataWidth + ", " + cmd.mDataHeight);
			Log.d(TAG, "rect: " + cmd.mLeft + "," + cmd.mTop + "," + cmd.mWidth + "," + cmd.mHeight);
		}

		PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(cmd.mData, cmd.mDataWidth, cmd.mDataHeight,
				cmd.mLeft, cmd.mTop, cmd.mWidth, cmd.mHeight, false);

		// PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(cmd.mData, cmd.mDataWidth, cmd.mDataHeight, 0,
		// 0, cmd.mDataWidth, cmd.mDataHeight, false);

		Result rawResult = null;
		if (source != null) {
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			try {
				rawResult = mMultiFormatReader.decodeWithState(bitmap);
			} catch (ReaderException re) {
				// continue
			} finally {
				mMultiFormatReader.reset();
			}
		}

		if (rawResult == null) {
			onDecodeError(this);
		} else {
			String result = rawResult.toString();
			if (DEBUG) {
				Log.d(TAG, "result: " + result);
			}
			onDecodeDone(this, result);
		}
	}
}
