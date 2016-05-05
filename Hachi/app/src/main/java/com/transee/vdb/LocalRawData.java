package com.transee.vdb;

public class LocalRawData {

	static {
		LibLoader.load();
	}

	public LocalRawData() {
		native_init();
	}

	@Override
	protected void finalize() throws Throwable {
		native_finalize();
		super.finalize();
	}

	public final boolean load(String filename) {
		native_load(filename);
		return mNativeContext != 0;
	}

	public final void unload() {
		if (mNativeContext != 0) {
			native_unload();
		}
	}

	public final byte[] read(int clip_time_ms, int data_types) {
		return mNativeContext == 0 ? null : native_read(clip_time_ms, data_types);
	}

	public final byte[] readBlock(int clip_time_ms, int length_ms, int data_type) {
		return mNativeContext == 0 ? null : native_read_block(clip_time_ms, length_ms, data_type);
	}

	private int mNativeContext;

	private native void native_init();

	private native void native_finalize();

	private native void native_load(String filename);

	private native void native_unload();

	private native byte[] native_read(int clip_time_ms, int data_types);

	private native byte[] native_read_block(int clip_time_ms, int length_ms, int data_type);

}
