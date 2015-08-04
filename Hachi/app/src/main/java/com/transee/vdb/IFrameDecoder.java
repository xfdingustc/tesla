package com.transee.vdb;

import android.graphics.Bitmap;

public class IFrameDecoder {

	static {
		LibLoader.load();
	}

	public IFrameDecoder() {
		native_init();
	}

	public Bitmap decode(String filename, int pos_ms) {
		return native_decode(filename, pos_ms);
	}

	protected Bitmap createBitmap(int width, int height) {
		return Bitmap.createBitmap(width, height, Bitmap.Config.valueOf("ARGB_8888"));
	}

	native private void native_init();

	native private Bitmap native_decode(String filename, int pos_ms);
}
