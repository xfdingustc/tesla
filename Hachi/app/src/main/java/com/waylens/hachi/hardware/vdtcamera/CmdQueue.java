package com.waylens.hachi.hardware.vdtcamera;

import android.os.SystemClock;

import java.util.LinkedList;

class CmdQueue<CmdType> {

	public static class CmdResult {
		public Object request;
		public int scheduleType;
	}

	private final LinkedList<CmdType> mRequestQueue = new LinkedList<>();
	private boolean mbThreadWaiting = false;

	private final int mMaxScheduler;
	private long[] mScheduleTime;
	private int mScheduleFlags;

	public CmdQueue(int maxScheduler) {
		mMaxScheduler = maxScheduler;
		mScheduleTime = new long[maxScheduler];
		mScheduleFlags = 0;
	}

	synchronized public void postRequest(CmdType request) {
		mRequestQueue.addLast(request);
		if (mbThreadWaiting) {
			mbThreadWaiting = false;
			notifyAll();
		}
	}

	synchronized public void schedule(int index, int delay) {
		long scheduleTime = SystemClock.uptimeMillis() + delay;
		if ((mScheduleFlags & (1 << index)) == 0 || mScheduleTime[index] > scheduleTime) {
			// not scheduled, or too late - reschedule
			mScheduleFlags |= (1 << index);
			mScheduleTime[index] = scheduleTime;
			if (mbThreadWaiting) {
				mbThreadWaiting = false;
				notifyAll();
			}
		}
	}

	private int getPendingIndex() {
		long minTime = Long.MAX_VALUE;
		int index = -1;
		for (int i = 0; i < mMaxScheduler; i++) {
			if ((mScheduleFlags & (1 << i)) != 0) {
				if (minTime > mScheduleTime[i]) {
					minTime = mScheduleTime[i];
					index = i;
				}
			}
		}
		return index;
	}

	synchronized public void getRequest(CmdResult cmdResult) throws InterruptedException {

		while (true) {
			if (mRequestQueue.size() > 0) {
				cmdResult.request = mRequestQueue.removeFirst();
				cmdResult.scheduleType = -1;
				return;
			}

			if (mScheduleFlags != 0) {
				int index = getPendingIndex();
				if (index < 0) {
					// false alarm
					mScheduleFlags = 0;
				} else {
					long currTime = SystemClock.uptimeMillis();
					if (currTime >= mScheduleTime[index]) {
						mScheduleFlags &= ~(1 << index);
						cmdResult.request = null;
						cmdResult.scheduleType = index;
						return;
					}
					mbThreadWaiting = true;
					wait(mScheduleTime[index] - currTime);
					continue;
				}
			}

			mbThreadWaiting = true;
			wait();
		}
	}
}
