package com.transee.vdb;

import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

abstract public class HttpRemuxer {

	static final boolean DEBUG = false;
	static final String TAG = "HttpRemuxer";

	static protected AtomicInteger mNumInstances = new AtomicInteger();

	abstract public void onEventAsync(HttpRemuxer remuxer, int event, int arg1, int arg2);

	public static final int EVENT_FINISHED = 0;
	public static final int EVENT_ERROR = 1;
	public static final int EVENT_PROGRESS = 2;

	private String mOutputFile;
	private final int mSessionCounter;
	private boolean mbError;

	static {
		LibLoader.load();
	}

	public HttpRemuxer(int sessionCounter) {
		native_init();
		mSessionCounter = sessionCounter;
		int num = mNumInstances.incrementAndGet();
		if (DEBUG) {
			Log.d(TAG, "create HttpRemuxer, total " + num);
		}
	}

	public final int setIframeOnly(boolean bIframeOnly) {
		return native_set_iframe_only(bIframeOnly);
	}

	// http://192.168.110.1:8085/clip/0/154711d3/0/360360-1001-0-0.ts,0,-1;
	public final int run(RemuxerParams params, String outputFile) {
		String inputFile = params.getInputFile();
		String inputMime = params.getInputMime();
		String outputFormat = params.getOutputFormat();
		int duration_ms = params.getDurationMs();
		mOutputFile = outputFile;
		boolean bMuteAudio = params.getDisableAudio();
		String audioFileName = params.getAudioFileName();
		if (bMuteAudio || (audioFileName != null && audioFileName.length() > 0)) {
			native_setAudio(bMuteAudio, audioFileName, "mp3");
		}
		return native_run(inputFile, inputMime, outputFile, outputFormat, duration_ms);
	}

	public final String getOutputFile() {
		return mOutputFile;
	}

	public final int getSessionCounter() {
		return mSessionCounter;
	}

	public final void setError(boolean bError) {
		mbError = bError;
	}

	public final boolean isError() {
		return mbError;
	}

	public final void release() {
		native_release();
		int num = mNumInstances.decrementAndGet();
		if (DEBUG) {
			Log.d(TAG, "release HttpRemuxer, total " + num);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		native_finalize();
		super.finalize();
	}

	protected void notify(int event, int arg1, int arg2) {
		onEventAsync(this, event, arg1, arg2);
	}

	// used by native
	private int mNativeContext;

	private native void native_init();

	private native void native_release();

	private native int native_set_iframe_only(boolean bIframeOnly);

	private native int native_setAudio(boolean bDisableAudio, String audioFileName, String audioFormat);

	private native int native_run(String inputFile, String inputMime, String outputFile, String outputFormat,
			int duration_ms);

	private native void native_finalize();
}
