package com.transee.vdb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

public class DownloadAdmin {

	public void downloadInfoChanged(DownloadAdmin admin) {
	}

	public void downloadInfoReady(DownloadAdmin admin) {
	}

	public void downloadInfoNotify(DownloadAdmin admin, int reason, int state, DownloadService.Item item, int progress) {
	}

	static final String TAG = "DownloadAdmin";

	private final Context mContext;
	private DownloadService mDownloadService;
	private boolean mDownloadServiceBound;
	private final DownloadService.DownloadInfo mDownloadInfo = new DownloadService.DownloadInfo();
	private Handler mHandler = new Handler();

	public DownloadAdmin(Context context) {
		mContext = context;
		bindDownloadService();
	}

	public void release() {
		unbindDownloadService();
		mHandler.removeCallbacks(null);
	}

	public DownloadService.DownloadInfo getDownloadInfo() {
		return mDownloadInfo;
	}

	private void bindDownloadService() {
		if (!mDownloadServiceBound) {
			Intent intent = new Intent(mContext, DownloadService.class);
			mContext.bindService(intent, mConnection, 0);
			mDownloadServiceBound = true;
		}
	}

	private void unbindDownloadService() {
		if (mDownloadServiceBound) {
			mContext.unbindService(mConnection);
			mDownloadServiceBound = false;
		}
	}

	private void onServiceStateChanged(int reason, int state, DownloadService.Item item, int progress) {
		mDownloadInfo.state = state;
		switch (reason) {
		case DownloadService.REASON_ITEM_PROGRESS:
			mDownloadInfo.percent = progress;
			downloadInfoChanged(this);
			break;
		case DownloadService.REASON_DOWNLOAD_ERROR:
		case DownloadService.REASON_ITEM_ADDED:
		case DownloadService.REASON_DOWNLOAD_STARTED:
		case DownloadService.REASON_DOWNLOAD_FINISHED:
			if (mDownloadService != null) {
				mDownloadService.getDownloadInfo(mDownloadInfo);
				downloadInfoChanged(this);
			}
			break;
		default:
			break;
		}
		downloadInfoNotify(this, reason, state, item, progress);
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			DownloadService.LocalBinder binder = (DownloadService.LocalBinder)service;
			mDownloadService = binder.getService();
			mDownloadService.addCallback(mServiceCallback);
			mDownloadService.getDownloadInfo(mDownloadInfo);
			downloadInfoChanged(DownloadAdmin.this);
			downloadInfoReady(DownloadAdmin.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mDownloadService.removeCallback(mServiceCallback);
			mDownloadService = null;
		}

	};

	private DownloadService.Callback mServiceCallback = new DownloadService.Callback() {
		@Override
		public void onStateChangedAsync(final DownloadService service, final int reason, final int state,
				final DownloadService.Item item, final int progress) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (mHandler != null && service == mDownloadService) {
						onServiceStateChanged(reason, state, item, progress);
					}
				}
			});
		}
	};

}
