package com.transee.viditcam.actions;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.transee.common.DateTime;
import com.transee.common.Timer;
import com.waylens.hachi.R;

abstract public class SyncDateTime extends DialogBuilder {

	abstract public void onSyncDateTime(long timeMillis, int timezone);

	private Timer mTimer;
	private TextView mTextDate;
	private TextView mTextTime;

	public SyncDateTime(Activity activity) {
		super(activity);
		setTitle(R.string.lable_sync_time);
		setContent(R.layout.dialog_sync_datetime);
		setButtons(DialogBuilder.DLG_OK_CANCEL);
	}

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		mTextDate = (TextView)layout.findViewById(R.id.textView2);
		mTextTime = (TextView)layout.findViewById(R.id.textView3);

		mTimer = new Timer() {
			@Override
			public void onTimer(Timer timer) {
				if (timer == mTimer) {
					updateDateTime();
					mTimer.run(1000);
				}
			}
		};

		updateDateTime();
		mTimer.run(1000);
	}

	@Override
	protected void onDismiss() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
	}

	private void updateDateTime() {
		long timeMillis = System.currentTimeMillis();
		mTextDate.setText(DateTime.getCurrentDate(timeMillis));
		mTextTime.setText(DateTime.getCurrentTime(timeMillis));
	}

	@Override
	protected void onClickPositiveButton() {
		long timeMillis = System.currentTimeMillis();
		int timezone = DateTime.getTimezone();
		onSyncDateTime(timeMillis, timezone);
	}

}
