package com.transee.common;

import android.os.Handler;

abstract public class Timer {

	abstract public void onTimer(Timer timer);

	private Handler mHandler;
	private Runnable mStockRunnable;
	private Runnable mRunnable;

	public int tag;

	public Timer() {
		mHandler = new Handler();
	}

	public boolean isRunning() {
		return mRunnable != null;
	}

	public void run(int interval) {
		cancel();
		if (mStockRunnable == null) {
			mStockRunnable = new Runnable() {
				@Override
				public void run() {
					if (mRunnable != null) {
						mRunnable = null;
						onTimer(Timer.this);
					}
				}
			};
		}
		mRunnable = mStockRunnable;
		mHandler.postDelayed(mRunnable, interval);
	}

	public void cancel() {
		if (mRunnable != null) {
			mHandler.removeCallbacks(mRunnable);
			mRunnable = null;
		}
	}
}
