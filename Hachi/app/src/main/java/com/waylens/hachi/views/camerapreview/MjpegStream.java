package com.waylens.hachi.views.camerapreview;

import android.util.Log;

import com.transee.common.BitmapBuffer;
import com.transee.common.ByteArrayBuffer;
import com.transee.common.MjpegDecoder;
import com.transee.common.MjpegReceiver;
import com.transee.common.SimpleQueue;

import java.net.InetSocketAddress;

abstract public class MjpegStream {

	abstract protected void onBitmapReadyAsync(MjpegDecoder decoder, MjpegStream stream);

	abstract protected void onEventAsync(MjpegDecoder decoder, MjpegStream stream);

	abstract protected void onIoErrorAsync(MjpegStream stream, int error);

	private static final boolean DEBUG = false;
	private static final String TAG = "MjpegStream";

	private boolean mbRunning = false;
	private MjpegReceiver mReceiver;
	private MjpegDecoder mDecoder;

	private final SimpleQueue<ByteArrayBuffer> mFrameQ;
	private final SimpleQueue<BitmapBuffer> mBitmapQ;

	public MjpegStream() {
		mFrameQ = new SimpleQueue<ByteArrayBuffer>();
		mBitmapQ = new SimpleQueue<BitmapBuffer>();
	}

	// API - called from UI thread
	public final void start(InetSocketAddress serverAddr, boolean bLoop) {
		if (mbRunning)
			return;

		mReceiver = new MjpegReceiver(serverAddr, mFrameQ, bLoop) {
			@Override
			public void onIOError(int error) {
				MjpegStream.this.onIoErrorAsync(MjpegStream.this, error);
			}
		};

		mDecoder = new MjpegDecoder(mFrameQ, mBitmapQ) {
			@Override
			public void onBitmapDecodedAsync(MjpegDecoder decoder, boolean isEvent) {
				if (isEvent) {
					onEventAsync(decoder, MjpegStream.this);
				} else {
					onBitmapReadyAsync(decoder, MjpegStream.this);
				}
			}
		};

		mDecoder.start();
		mReceiver.start();

		mbRunning = true;
	}

	// API
	public final BitmapBuffer getOutputBitmapBuffer(MjpegDecoder decoder) {
		if (decoder == null && (decoder = mDecoder) == null) {
			// called from UI thread, and no decoder
			return null;
		}
		return decoder.getBitmapBuffer();
	}

	// API - called from UI thread
	public final void stop() {
		if (!mbRunning)
			return;

		mReceiver.shutdown();
		mReceiver = null;

		mDecoder.shutdown();
		mDecoder = null;

		mbRunning = false;

		if (DEBUG) {
			Log.d(TAG, "buffer: total=" + mFrameQ.getTotalObjects() + ", dropped=" + mFrameQ.getDroppedObjects());
		}
	}

	// API
	public final void postEvent(int event, int delay) {
		mFrameQ.postEvent(event, delay);
	}
}
