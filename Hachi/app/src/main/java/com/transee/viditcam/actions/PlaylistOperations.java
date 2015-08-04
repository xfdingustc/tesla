package com.transee.viditcam.actions;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.Button;
import android.widget.PopupWindow;

import com.transee.common.Utils;
import com.waylens.hachi.R;
import com.transee.viditcam.app.BaseActivity;
import com.transee.viditcam.app.VdbEditor;

abstract public class PlaylistOperations {

	abstract public void onSelectAudio();

	private final BaseActivity mActivity;
	private final VdbEditor mEditor;

	private View mLayout;
	private PopupWindow mWindow;

	static final int CMD_MOVE_LEFT = 0;
	static final int CMD_MOVE_RIGHT = 1;
	static final int CMD_REMOVE = 2;
	static final int CMD_TOGGLE_MUTE = 3;
	static final int CMD_SELECT_AUDIO = 4;

	@SuppressLint("InflateParams")
	public PlaylistOperations(BaseActivity activity, VdbEditor editor) {
		mActivity = activity;
		mEditor = editor;

		mLayout = LayoutInflater.from(activity).inflate(R.layout.menu_playlist_operations, null);
		mLayout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		mWindow = activity.createPopupWindow(mLayout);

		setButton(R.id.button1, CMD_MOVE_LEFT, mEditor.canMoveClipLeft());
		setButton(R.id.button2, CMD_MOVE_RIGHT, mEditor.canMoveClipRight());
		setButton(R.id.button3, CMD_REMOVE, true);
		Button button = setButton(R.id.button4, CMD_TOGGLE_MUTE, true);
		if (mEditor.bMuteAudio) {
			button.setText(R.string.menu_restore_audio);
		} else {
			// button.setText(R.string.menu_mute_audio);
		}
		setButton(R.id.button5, CMD_SELECT_AUDIO, true);
	}

	private View.OnClickListener mOnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			int cmd = (Integer)v.getTag();
			mWindow.dismiss();
			switch (cmd) {
			case CMD_MOVE_LEFT:
				mEditor.requestMoveClipLeft();
				break;
			case CMD_MOVE_RIGHT:
				mEditor.requestMoveClipRight();
				break;
			case CMD_REMOVE:
				mEditor.requestRemoveClip();
				break;
			case CMD_TOGGLE_MUTE:
				mEditor.requestToggleMute();
				break;
			case CMD_SELECT_AUDIO:
				onSelectAudio();
				break;
			}
		}
	};

	private Button setButton(int resId, int cmd, boolean bEnabled) {
		Button button = (Button)mLayout.findViewById(resId);
		button.setTag(cmd);
		if (!bEnabled) {
			button.setEnabled(false);
		} else {
			button.setOnClickListener(mOnClick);
		}
		return button;
	}

	public void show(View anchor) {
		int[] location = new int[2];
		anchor.getLocationInWindow(location);
		int screenWidth = Utils.getScreenWidth(mActivity);
		if (location[0] + mWindow.getWidth() > screenWidth) {
			location[0] = screenWidth - mWindow.getWidth();
		}
		mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, location[0], location[1] - mWindow.getHeight());
	}

}
