package com.waylens.hachi.comp;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;

import com.transee.common.Timer;
import com.transee.common.VideoView;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.R;
import com.transee.viditcam.actions.DialogBuilder;
import com.transee.viditcam.app.MediaHelper;
import com.transee.viditcam.app.ViditImageButton;
import com.waylens.hachi.ui.activities.BaseActivity;

import java.util.Locale;

abstract public class VdbPlayback {

	static final boolean DEBUG = false;
	static final String TAG = "VdbPlayback";

	protected abstract boolean requestPlayback();

	protected abstract void preparePlayback();

	protected abstract void playbackStarted(long startTimeMs);

	protected abstract void playbackPosChanged(long startTimeMs, int posMs);

	protected abstract void playbackEnded(long startTimeMs, int lengthMs);

	protected abstract boolean shouldFinishPlay();

	protected abstract void showProgress();

	protected abstract void hideProgress();

	protected abstract void onViewDown();

	protected abstract void onViewSingleTapUp();

	protected abstract void onViewDoubleClick();

	public static final int STATE_IDLE = 0;
	public static final int STATE_WAITING_URL = 1;
	public static final int STATE_STARTING = 2;
	public static final int STATE_PLAYING = 3;
	public static final int STATE_PAUSED = 4;
	public static final int STATE_COMPLETED = 5;

	private int mPlaybackState = STATE_IDLE;
	private int mLastPlaybackPos = 0;
	private String mPlaybackUrl;
	private long mStartTimeMs;
	private int mOffsetMs;
	private int mLengthMs;

	private BaseActivity mActivity;
	private VideoView mVideoView;
	private ViditImageButton mPlayButton;
	private Timer mTimer;

	private MediaPlayer mAudioPlayer;
	private Clip.StreamInfo mStreamInfo;
	private String mAudioUrl;
	private int mAudioStartMs;

	public VdbPlayback(BaseActivity activity, View layout) {
		mActivity = activity;
		mVideoView = (VideoView)layout.findViewById(R.id.videoView1);
		mVideoView.setCallback(mVideoVideoCallback);
		hideVideoView();
	}

	public void onInitUI(View toolbar) {
		mPlayButton = (ViditImageButton)toolbar.findViewById(R.id.btnPlay);
		mPlayButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onPlayButtonClick(v);
			}
		});
	}

	public void onSetupUI() {
		if (mActivity.isPortrait()) {
			// mVideoView.setThumbnailScale(3);
			mVideoView.setBackgroundColor(mActivity.getResources().getColor(R.color.imageVideoBackground));
		} else {
			// mVideoView.setThumbnailScale(4);
			mVideoView.setBackgroundColor(Color.BLACK);
		}
	}

	public final boolean isIdle() {
		return mPlaybackState == STATE_IDLE;
	}

	public final boolean isPaused() {
		return mPlaybackState == STATE_PAUSED;
	}

	public final boolean isPlaying() {
		return mPlaybackState == STATE_PLAYING;
	}

	public final boolean imageReady() {
		return mPlaybackState == STATE_PAUSED || mPlaybackState == STATE_COMPLETED;
	}

	private void stopTimer() {
		if (mTimer != null) {
			mTimer.cancel();
		}
	}

	private void startTimer() {
		if (mTimer == null) {
			mTimer = new Timer() {
				@Override
				public void onTimer(Timer timer) {
					onPlaybackTimer();
				}
			};
		}
		mTimer.run(1000);
		mLastPlaybackPos = mOffsetMs;
	}

	// STATE_PLAYING or STATE_PAUSED
	private void updatePlaybackTime() {
		if (!mVideoView.isPlaying()) {
			// may got a big value!
		} else {
			int posMs = mVideoView.getCurrentPosition();
			// Log.w(TAG, "current pos: " + posMs);
			// when playback is end, posMs is 0!
			if (posMs < mLastPlaybackPos) {
				if (DEBUG) {
					Log.d(TAG, "pos: " + posMs);
				}
				posMs = mLengthMs;
			} else {
				mLastPlaybackPos = posMs;
			}
			posMs -= mOffsetMs;
			if (posMs < 0)
				posMs = 0;

			// Log.w(TAG, "playbackPosChanged: " + mStartTimeMs + ", " + posMs);
			playbackPosChanged(mStartTimeMs, posMs);
		}
	}

	private void onPlaybackTimer() {
		if (mPlaybackState == STATE_PLAYING || mPlaybackState == STATE_PAUSED) {
			updatePlaybackTime();
			mTimer.run(1000);
		}
	}

	private void showVideoView() {
		mVideoView.setVisibility(View.VISIBLE);
	}

	private void hideVideoView() {
		mVideoView.setVisibility(View.INVISIBLE);
	}

	// API
	public VideoView getVideoView() {
		return mVideoView;
	}

	// API
	public void playUrl(Clip.StreamInfo streamInfo, String playbackUrl, long startTimeMs, int lengthMs, int offsetMs,
			String audioUrl, int audioStartMs, boolean bMute) {

		mStreamInfo = streamInfo;
		mAudioUrl = audioUrl;
		mAudioStartMs = audioStartMs;

		if (mPlaybackState == STATE_WAITING_URL) {

			if (mAudioUrl != null) {
				if (mAudioPlayer == null) {
					mAudioPlayer = new MediaPlayer();
				}
			}
			try {
				mAudioPlayer.setDataSource(mAudioUrl);
				mAudioPlayer.prepareAsync();
			} catch (Exception ex) {

			}

			mPlaybackUrl = playbackUrl;
			mStartTimeMs = startTimeMs;
			mOffsetMs = offsetMs;
			mLengthMs = lengthMs;
			mPlaybackState = STATE_STARTING;
			showVideoView();
			preparePlayback();

			mVideoView.setVideoPath(mPlaybackUrl);
			mVideoView.startPlayback(offsetMs, false, bMute);
		}
	}

	// API
	public void playbackUrlError() {
		if (mPlaybackState == STATE_WAITING_URL) {
			// todo - should popup error msg
			mPlaybackState = STATE_IDLE;

			hideProgress();

			changeToPlay();

			if (mAudioPlayer != null) {
				mAudioPlayer.stop();
				mAudioPlayer = null;
			}
		}
	}

	// API
	public boolean cancelPlayback() {
		switch (mPlaybackState) {
		default:
		case STATE_IDLE:
		case STATE_WAITING_URL:
			mPlaybackState = STATE_IDLE;
			return false;

		case STATE_STARTING:
		case STATE_PLAYING:
		case STATE_PAUSED:
			if (DEBUG) {
				Log.d(TAG, "force stop");
			}
			forceStop();
			mPlaybackState = STATE_IDLE;
			return true;
		}
	}

	// API
	public void seekTo(int ms) {
		if (mVideoView.seekTo(ms, true)) {
			mPlaybackState = STATE_PAUSED;
			stopTimer();
			onPaused();
		}
	}

	// API
	public Bitmap getCurrentImage() {
		return mVideoView.getVideoImage();
	}

	private final void changeToPlay() {
		mPlayButton.changeImages(R.drawable.btn_play, R.drawable.btn_play_pressed);
		mPlayButton.setClickable(true);
	}

	private final void changeToPause() {
		mPlayButton.changeImages(R.drawable.btn_pause, R.drawable.btn_pause_pressed);
		mPlayButton.setClickable(true);
	}

	private final void changeToWait() {
		// TODO
		mPlayButton.setClickable(false);
	}

	private void forceStop() {
		mVideoView.stopPlayback();
		hideProgress();
		stopTimer();
		hideVideoView();
		changeToPlay();
		if (mAudioPlayer != null) {
			mAudioPlayer.stop();
			mAudioPlayer = null;
		}
	}

	private void onVideoViewStarted(VideoView view) {
		if (mPlaybackState == STATE_STARTING) {
			mPlaybackState = STATE_PLAYING;
			hideProgress();
			changeToPause();
			startTimer();
			playbackStarted(mStartTimeMs);
			if (mAudioPlayer != null) {
				mAudioPlayer.seekTo(mAudioStartMs);
				mAudioPlayer.start();
			}
		}
	}

	private void onVideoViewError(VideoView view, int what, int extra) {
		forceStop();
		mPlaybackState = STATE_IDLE;
		DialogBuilder builder = new DialogBuilder(mActivity);
		if (what == 1 && extra == 0x80000000) {
			String notSupported = mActivity.getResources().getString(R.string.msg_cannot_play);
			if (mStreamInfo != null) {
				String videoInfo = MediaHelper.formatVideoInfo(mActivity, mStreamInfo);
				notSupported += " " + videoInfo;
			}
			builder.setMsg(notSupported);
		} else {
			String fmt = mActivity.getResources().getString(R.string.msg_playback_error);
			String msg = String.format(Locale.US, fmt, what, extra);
			builder.setMsg(msg);
		}
		builder.setButtons(DialogBuilder.DLG_OK);
		builder.show();
	}

	private void onVideoViewCompleted(VideoView view) {
		mPlaybackState = STATE_COMPLETED;
		if (shouldFinishPlay()) {
			forceStop();
			mPlaybackState = STATE_IDLE;
		} else {
			stopTimer();
		}
		playbackEnded(mStartTimeMs, mLengthMs);
	}

	public void preparePlayback(boolean bPause) {
		if (mPlaybackState == STATE_IDLE) {
			if (requestPlayback()) {
				mPlaybackState = STATE_WAITING_URL;
				showProgress();
				changeToWait();
			}
		}
	}

	private void onPaused() {
		changeToPlay();
		if (mAudioPlayer != null) {
			mAudioPlayer.pause();
		}
	}

	public void pausePlayback() {
		if (mVideoView.pause()) {
			mPlaybackState = STATE_PAUSED;
			stopTimer();
			updatePlaybackTime();
			onPaused();
		}
	}

	private void onPlayButtonClick(View v) {
		switch (mPlaybackState) {
		case STATE_IDLE:
			preparePlayback(false);
			break;

		case STATE_STARTING:
			// pausePlayback();
			break;

		case STATE_PLAYING:
			pausePlayback();
			break;

		case STATE_PAUSED:
			// case STATE_COMPLETED:
			if (mAudioPlayer != null) {
				mAudioPlayer.start();
			}
			mVideoView.resumeFromPause();
			changeToPause();
			mPlaybackState = STATE_PLAYING;
			startTimer();
			break;

		default:
			break;
		}
	}

	private final VideoView.Callback mVideoVideoCallback = new VideoView.Callback() {

		@Override
		public void onStarted(VideoView view) {
			VdbPlayback.this.onVideoViewStarted(view);
		}

		@Override
		public void onInfo(VideoView view, int what, int extra) {
			// TODO
		}

		@Override
		public void onError(VideoView view, int what, int extra) {
			VdbPlayback.this.onVideoViewError(view, what, extra);
		}

		@Override
		public void onCompleted(VideoView view) {
			VdbPlayback.this.onVideoViewCompleted(view);
		}

		@Override
		public void onBufferingUpdate(VideoView view, int percent) {
			// TODO
		}

		@Override
		public void onDown() {
			// mPlayButton.performClick();
			VdbPlayback.this.onViewDown();
		}

		@Override
		public void onSingleTapUp() {
			VdbPlayback.this.onViewSingleTapUp();
		}

		@Override
		public void onDoubleClick() {
			VdbPlayback.this.onViewDoubleClick();
		}

		@Override
		public void requestDisplay() {
		}

	};
}
