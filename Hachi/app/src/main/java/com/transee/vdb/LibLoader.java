package com.transee.vdb;

public class LibLoader {

	static Object mLock = new Object();
	static boolean mbLoaded = false;

	static void load() {
		synchronized (mLock) {
			if (!mbLoaded) {
				mbLoaded = true;
				System.loadLibrary("avfmedia");
			}
		}
	}

}
