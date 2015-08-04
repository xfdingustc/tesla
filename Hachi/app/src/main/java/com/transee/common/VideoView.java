package com.transee.common;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;

public class VideoView extends ViewGroup {

	public interface Callback {
		public void onStarted(VideoView view);

		public void onCompleted(VideoView view);

		public void onError(VideoView view, int what, int extra);

		public void onInfo(VideoView view, int what, int extra);

		public void onBufferingUpdate(VideoView view, int percent);

		public void onDown();

		public void onSingleTapUp();

		public void onDoubleClick();

		public void requestDisplay();
	}

	static final boolean DEBUG = false;
	static final String TAG = "VideoView";

	public static final int DBL_CLICK_LENGTH = 400; // ms
	static final boolean USE_TEXTUREVIEW = true;

	public static final int STATE_ERROR = -1;
	public static final int STATE_IDLE = 0;
	public static final int STATE_PREPARING = 1;
	public static final int STATE_PREPARED = 2;
	public static final int STATE_PLAYING = 3;
	public static final int STATE_PAUSED = 4;
	public static final int STATE_COMPLETED = 5;

	private Uri mUri;
	private MediaPlayer mMediaPlayer;
	private SurfaceView mSurfaceView;
	private TextureView mTextureView;
	private boolean mbSurfaceCreated;
	private int mPlaybackStartCounter;
	private int mStartThreshold;

	private int mSurfaceWidth;
	private int mSurfaceHeight;

	private int mVideoWidth;
	private int mVideoHeight;

	private int mStartPosition;
	private int mCurrState = STATE_IDLE;
	private int mTargetState = STATE_IDLE;
	private boolean mbMute = false;
	private int mSeekMs = -1;
	private int mPendingSeekings;
	private int mCurrentBufferPercentage;

	private GestureDetector mGesture;
	private Callback mCallback;

	private int mAnchorLeft;
	private int mAnchorTop;
	private int mAnchorRight;
	private int mAnchorBottom;

	private long mLastTapUpTime = -1;

	private Rect mSurfaceRect = new Rect();

	public VideoView(Context context) {
		super(context);
		initView();
	}

	public VideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public VideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {
		mGesture = new GestureDetector(getContext(), new MyGestureListener());
		// need 3 frames to avoid black screen on Huawei P6
		// mStartThreshold = 1;
		// if (Build.BRAND.equals("Meizu")) {
		mStartThreshold = 3;
		// }
	}

	private void createView() {
		if (DEBUG) {
			Log.d(TAG, "createView");
		}
		if (USE_TEXTUREVIEW) {
			mTextureView = new TextureView(getContext());
			mTextureView.setVisibility(View.INVISIBLE);
			mTextureView.setSurfaceTextureListener(mSTListener);
			addView(mTextureView);
		} else {
			mSurfaceView = new SurfaceView(getContext());
			mSurfaceView.setVisibility(View.INVISIBLE);
			mSurfaceView.getHolder().addCallback(mSHCallback);
			addView(mSurfaceView);
		}
	}

	private void destroyView() {
		if (DEBUG) {
			Log.d(TAG, "destroyView");
		}
		View view = null;
		if (mTextureView != null) {
			view = mTextureView;
			mTextureView = null;
		} else if (mSurfaceView != null) {
			view = mSurfaceView;
			mSurfaceView = null;
		}
		if (view != null) {
			view.setVisibility(View.INVISIBLE);
			removeView(view);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
		int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
		if (DEBUG) {
			Log.d(TAG, "onMeasure: " + width + "," + height);
		}
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (DEBUG) {
			Log.d(TAG, "onLayout: " + l + "," + t + ", " + r + "," + b);
		}
		layoutSurfaceView(l, t, r, b);
	}

	private void layoutSurfaceView(int l, int t, int r, int b) {
		if (mVideoWidth != 0 && mVideoHeight != 0) {
			int width;
			int height;
			int marginX;
			int marginY;
			int w = r - l;
			int h = b - t;
			if (w * mVideoHeight > h * mVideoWidth) {
				// view is wider
				width = h * mVideoWidth / mVideoHeight;
				height = h;
				marginX = (w - width) / 2;
				marginY = 0;
			} else {
				width = w;
				height = w * mVideoHeight / mVideoWidth;
				marginX = 0;
				marginY = (h - height) / 2;
			}

			performLayoutSurfaceView(marginX, marginY, marginX + width, marginY + height);

		} else {

			performLayoutSurfaceView(l, t, r, b);
		}
	}

	// API
	public final boolean canAnimate() {
		return USE_TEXTUREVIEW;
	}

	// API
	public final void setAnchorRect(int leftMargin, int topMargin, int rightMargin, int bottomMargin, float bestZoom) {
		if (leftMargin != mAnchorLeft || topMargin != mAnchorTop || rightMargin != mAnchorRight
				|| bottomMargin != mAnchorBottom) {
			mAnchorLeft = leftMargin;
			mAnchorTop = topMargin;
			mAnchorRight = rightMargin;
			mAnchorBottom = bottomMargin;
			performLayoutSurfaceView();
		}
	}

	// API
	public Bitmap getVideoImage() {
		if (mTextureView != null && mPlaybackStartCounter > 0) {
			return mTextureView.getBitmap(mVideoWidth, mVideoHeight);
		}
		return null;
	}

	// API
	public void setVolume(float scale) {
		if (mMediaPlayer != null) {
			mMediaPlayer.setVolume(scale, scale);
		}
	}

	private final void performLayoutSurfaceView() {
		layoutSurfaceView(getLeft(), getTop(), getRight(), getBottom());
	}

	private void performLayoutSurfaceView(int left, int top, int right, int bottom) {
		if (DEBUG) {
			Log.d(TAG, "performLayoutSurfaceView: " + left + "," + top + ", " + right + "," + bottom);
		}

		int width = getWidth();
		int height = getHeight();

		// adjust x position
		int tmp = right - left;
		int dx = 0;
		int lenx = width - (mAnchorLeft + mAnchorRight);
		if (tmp <= lenx) {
			dx = mAnchorLeft + (lenx - tmp) / 2 - left;
		} else if (left > mAnchorLeft) {
			dx = mAnchorLeft - left;
		} else if (right + mAnchorRight < width) {
			dx = width - (right + mAnchorRight);
		}
		left += dx;
		right += dx;

		// adjust y position
		tmp = bottom - top;
		int dy = 0;
		int leny = height - (mAnchorTop + mAnchorBottom);
		if (tmp <= leny) {
			dy = mAnchorTop + (leny - tmp) / 2 - top;
		} else if (top > mAnchorTop) {
			dy = mAnchorTop - top;
		} else if (bottom + mAnchorBottom < height) {
			dy = height - (bottom + mAnchorBottom);
		}
		top += dy;
		bottom += dy;

		mSurfaceRect.set(left, top, right, bottom);

		if (mTextureView != null) {
			mTextureView.layout(left, top, right, bottom);
		} else if (mSurfaceView != null) {
			mSurfaceView.layout(left, top, right, bottom);
		}
	}

	// API
	public final Rect getVideoRect() {
		// TODO
		return mSurfaceRect;
	}

	// API
	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	// API
	public int getState() {
		return mCurrState;
	}

	// API
	public void setVideoPath(String path) {
		if (DEBUG) {
			Log.d(TAG, "setVideoPath " + path);
		}
		mUri = Uri.parse(path);
	}

	// API
	public void stopPlayback() {
		if (DEBUG) {
			Log.d(TAG, "stopPlayback");
		}
		releaseMediaPlayer(true);
		destroyView();
		mSurfaceWidth = 0;
		mSurfaceHeight = 0;
		mVideoWidth = 0;
		mVideoHeight = 0;
	}

	// API
	public void startPlayback(int offsetMs, boolean bPause, boolean bMute) {
		createView();
		mPlaybackStartCounter = 0;
		mTargetState = bPause ? STATE_PAUSED : STATE_PLAYING;
		mSeekMs = offsetMs;
		mbMute = bMute;
		if (DEBUG) {
			Log.d(TAG, "startPlayback");
		}
		tryStartPlayback("startPlayback");
	}

	private final void onPlaybackStarted() {
		mPlaybackStartCounter++;
		if (mPlaybackStartCounter == 1) {
			if (mTargetState == STATE_PAUSED) {
				mMediaPlayer.pause();
			}
		}
		if (mPlaybackStartCounter == mStartThreshold) {
			if (mTextureView != null) {
				mStartPosition = mMediaPlayer.getCurrentPosition();
				if (mCallback != null) {
					mCallback.onStarted(VideoView.this);
				}
			}
		}
	}

	private void performSetKeepScreenOn(boolean bSet) {
		if (getKeepScreenOn() != bSet) {
			if (DEBUG) {
				Log.d(TAG, "setKeepScreenOn: " + bSet);
			}
			setKeepScreenOn(bSet);
		}
	}

	private void tryStartPlayback(String reason) {
		if (DEBUG) {
			Log.d(TAG, "=== tryStartPlayback(" + reason + ") ===");
		}

		if (mTargetState != STATE_PLAYING && mTargetState != STATE_PAUSED) {
			if (DEBUG) {
				Log.d(TAG, "tryStartPlayback, state=" + mTargetState + " ===");
			}
			return;
		}

		if (mUri == null) {
			if (DEBUG) {
				Log.d(TAG, "tryStartPlayback: no url ===");
			}
			return;
		}

		if (!mbSurfaceCreated) {
			if (DEBUG) {
				Log.d(TAG, "tryStartPlayback(1): set SurfaceView ===");
			}
			if (mTextureView != null) {
				mTextureView.setVisibility(View.VISIBLE);
			} else if (mSurfaceView != null) {
				mSurfaceView.setVisibility(View.VISIBLE);
			}
			// requestLayout();
			return;
		}

		if (mMediaPlayer == null) {
			if (DEBUG) {
				Log.d(TAG, "tryStartPlayback(2): openVideo ===");
			}
			openVideo();
			return;
		}

		if (mCurrState != STATE_PREPARED && mCurrState != STATE_PLAYING && mCurrState != STATE_PAUSED) {
			if (DEBUG) {
				Log.d(TAG, "tryStartPlayback: not prepared ===");
			}
			return;
		}

		if (mCurrState == STATE_PREPARED) {
			if (mTextureView != null) {
				if (DEBUG) {
					Log.d(TAG, "tryStartPreview: performLayoutSurfaceView");
				}
				performLayoutSurfaceView();
			} else if (mSurfaceView != null) {
				if (mVideoWidth != mSurfaceWidth || mVideoHeight != mSurfaceHeight) {
					if (DEBUG) {
						Log.d(TAG, "tryStartPlayback, surface: " + mSurfaceView.getWidth() + "x"
								+ mSurfaceView.getHeight());
						Log.d(TAG, "tryStartPlayback, video=" + mVideoWidth + "x" + mVideoHeight + "; surface="
								+ mSurfaceWidth + "," + mSurfaceHeight + "; setFixedSize ===");
					}
					mSurfaceView.getHolder().setFixedSize(mVideoWidth, mVideoHeight);
					return;
				}
			}
		}

		if (DEBUG) {
			Log.d(TAG, "tryStartPlayback: run");
		}

		performSetKeepScreenOn(true);
		mCurrState = STATE_PLAYING;

		mMediaPlayer.start();

		// TODO
		if (mSeekMs > 0) {
			mMediaPlayer.seekTo(mSeekMs);
		}
	}

	private void performSeek() {
		if (mSeekMs >= 0 && mPendingSeekings <= 1) {
			if (DEBUG) {
				Log.d(TAG, "performSeek");
			}
			mMediaPlayer.seekTo(mSeekMs);
			mSeekMs = -1;
			mPendingSeekings++;
		}
	}

	// API
	public boolean pause() {
		if (isInPlaybackState()) {
			if (mMediaPlayer.isPlaying()) {
				if (DEBUG) {
					Log.d(TAG, "pause MediaPlayer");
				}
				mMediaPlayer.pause();
				mCurrState = STATE_PAUSED;
				performSetKeepScreenOn(false);
			}
			mTargetState = STATE_PAUSED;
			return true;
		}
		mTargetState = STATE_PAUSED;
		return false;
	}

	// API
	public void resumeFromPause() {
		mTargetState = STATE_PLAYING;
		tryStartPlayback("resumeFromPause");
	}

	// API
	public void suspend() {
		releaseMediaPlayer(false);
	}

	// API - resume from suspend
	public void resume() {
		tryStartPlayback("resume");
	}

	// API
	public int getDuration() {
		if (isInPlaybackState()) {
			return mMediaPlayer.getDuration();
		}
		return 0;
	}

	// API
	public int getCurrentPosition() {
		if (isInPlaybackState()) {
			int rval = mMediaPlayer.getCurrentPosition();
			// Log.w(TAG, " get current pos: " + rval + ", mSeekMs: " + mSeekMs);
			return rval - mStartPosition;
		}
		return 0;
	}

	// API
	public boolean canSeek() {
		if (mMediaPlayer == null)
			return false;
		if (mCurrState != STATE_PLAYING && mCurrState != STATE_PAUSED && mCurrState != STATE_COMPLETED)
			return false;
		return true;
	}

	// API
	// returns true: state changed to STATE_PAUSED
	public boolean seekTo(int ms, boolean bPause) {
		boolean result = false;
		if (canSeek()) {
			if (bPause && mCurrState == STATE_PLAYING) {
				pause();
				result = true;
			}
			if (mCurrState == STATE_COMPLETED) {
				mCurrState = STATE_PAUSED;
				result = true;
			}
			mSeekMs = ms;
			performSeek();
		}
		return result;
	}

	// API
	public boolean isPlaying() {
		return isInPlaybackState() && mMediaPlayer.isPlaying();
	}

	// API
	public int getBufferPercentage() {
		return mCurrentBufferPercentage;
	}

	private void openVideo() {

		// copied from android VideoView
		Intent intent = new Intent("com.android.music.musicservicecommand");
		intent.putExtra("command", "pause");
		getContext().sendBroadcast(intent);

		try {

			if (DEBUG) {
				Log.d(TAG, "create MediaPlayer");
			}

			mMediaPlayer = new MediaPlayer();

			mMediaPlayer.setOnPreparedListener(mPreparedListener);
			mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
			mMediaPlayer.setOnCompletionListener(mCompletionListener);
			mMediaPlayer.setOnErrorListener(mErrorListener);
			mMediaPlayer.setOnInfoListener(mInfoListener);
			mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
			mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);

			if (mbMute) {
				mMediaPlayer.setVolume(0, 0);
			}

			mPendingSeekings = 0;
			mSeekMs = -1;

			//mMediaPlayer.setDataSource(getContext(), mUri);
			Log.e("test", "Uri: " + mUri.toString());
			mMediaPlayer.setDataSource(mUri.toString());
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			if (mTextureView != null) {
				Surface surface = new Surface(mTextureView.getSurfaceTexture());
				mMediaPlayer.setSurface(surface);
			} else if (mSurfaceView != null) {
				mMediaPlayer.setDisplay(mSurfaceView.getHolder());
			}
			mMediaPlayer.prepareAsync();

			mCurrState = STATE_PREPARING;
			mCurrentBufferPercentage = 0;

		} catch (IOException ex) {

			mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);

		} catch (IllegalArgumentException ex) {

			mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);

		}
	}

	private final void releaseMediaPlayer(boolean bClearTargetState) {
		if (mMediaPlayer != null) {
			if (DEBUG) {
				Log.d(TAG, "release MediaPlayer");
			}
			// TODO flyme mx3 sometimes suspends at reset() or release()
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mCurrState = STATE_IDLE;
			performSetKeepScreenOn(false);
		}
		if (bClearTargetState) {
			mTargetState = STATE_IDLE;
		}
	}

	private boolean isInPlaybackState() {
		return mMediaPlayer != null
				&& (mCurrState == STATE_PREPARED || mCurrState == STATE_PLAYING || mCurrState == STATE_PAUSED || mCurrState == STATE_COMPLETED);
	}

	private void onMediaPlayerPrepared(MediaPlayer mp) {

		mVideoWidth = mp.getVideoWidth();
		mVideoHeight = mp.getVideoHeight();
		mCurrState = STATE_PREPARED;

		if (DEBUG) {
			Log.d(TAG, "onPrepared, " + mVideoWidth + "x" + mVideoHeight);
		}

		if (mSurfaceView != null) {
			if (mCallback != null) {
				mCallback.onStarted(VideoView.this);
			}
		}

		tryStartPlayback("onMediaPlayerPrepared");
	}

	private void onMediaPlayerVideoSizeChanged(MediaPlayer mp, int width, int height) {
		if (mVideoWidth != width || mVideoHeight != height) {
			mVideoWidth = width;
			mVideoHeight = height;
			if (DEBUG) {
				Log.d(TAG, "onVideoSizeChanged, " + width + "," + height + " this: " + mVideoWidth + ", "
						+ mVideoHeight);
			}
			requestLayout(); // onPrepared may report a false size
			tryStartPlayback("onMediaPlayerVideoSizeChanged");
		}
	}

	private void onMediaPlayerCompletion(MediaPlayer mp) {
		mCurrState = STATE_COMPLETED;
		mTargetState = STATE_COMPLETED;
		if (DEBUG) {
			Log.d(TAG, "onCompletion");
		}
		if (mCallback != null) {
			mCallback.onCompleted(VideoView.this);
		}
	}

	private void onMediaPlayerError(MediaPlayer mp, int what, int extra) {
		mCurrState = STATE_ERROR;
		mTargetState = STATE_ERROR;
		if (DEBUG) {
			Log.d(TAG, "onError");
		}
		if (mCallback != null) {
			mCallback.onError(VideoView.this, what, extra);
		}
	}

	private final MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
		@Override
		public void onPrepared(MediaPlayer mp) {
			VideoView.this.onMediaPlayerPrepared(mp);
		}
	};

	private final MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
		@Override
		public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
			VideoView.this.onMediaPlayerVideoSizeChanged(mp, width, height);
		}
	};

	private final MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer mp) {
			onMediaPlayerCompletion(mp);
		}
	};

	private final MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			onMediaPlayerError(mp, what, extra);
			return true;
		}
	};

	private final MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {
		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			if (DEBUG) {
				Log.d(TAG, ">>> onInfo " + what + "," + extra);
			}
			if (mCallback != null) {
				mCallback.onInfo(VideoView.this, what, extra);
			}
			return true;
		}
	};

	private final MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			mCurrentBufferPercentage = percent;
			if (DEBUG) {
				Log.d(TAG, "onBufferingUpdate " + percent);
			}
			if (mCallback != null) {
				mCallback.onBufferingUpdate(VideoView.this, percent);
			}
		}
	};

	private final MediaPlayer.OnSeekCompleteListener mSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {
		@Override
		public void onSeekComplete(MediaPlayer mp) {
			if (DEBUG) {
				Log.d(TAG, "onSeekComplete");
			}
			if (mPendingSeekings > 0) {
				mPendingSeekings--;
				performSeek();
			}
		}
	};

	private void onSurfaceCreated() {
		if (DEBUG) {
			Log.d(TAG, "surfaceCreated");
		}
		mbSurfaceCreated = true;
		tryStartPlayback("onSurfaceCreated");
	}

	private void forceRefresh() {
		if (mTextureView != null) {
			// for texture view to update after rotation
			boolean opaque = mTextureView.isOpaque();
			mTextureView.setOpaque(!opaque);
			mTextureView.setOpaque(opaque);
		}
	}

	private void onSurfaceChanged(int width, int height) {
		if (DEBUG) {
			Log.d(TAG, "surfaceChanged " + width + "x" + height + "; video: " + mVideoWidth + "x" + mVideoHeight);
		}
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		if (mCurrState == STATE_PAUSED || mCurrState == STATE_COMPLETED) {
			forceRefresh();
		} else if (mCurrState != STATE_PLAYING) {
			tryStartPlayback("onSurfaceChanged");
		}
	}

	private void onSurfaceDestroyed() {
		if (DEBUG) {
			Log.d(TAG, "surfaceDestroyed");
		}
		if (mTextureView != null) {
			mTextureView.getSurfaceTexture().release();
		}
		mbSurfaceCreated = false;
		releaseMediaPlayer(true);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean handled = super.dispatchTouchEvent(ev);
		handled |= mGesture.onTouchEvent(ev);
		return handled;
	}

	private boolean isDoubleClick(MotionEvent e) {
		long time = SystemClock.uptimeMillis();
		if (mLastTapUpTime < 0 || time >= mLastTapUpTime + DBL_CLICK_LENGTH) {
			mLastTapUpTime = time;
			return false;
		} else {
			mLastTapUpTime = -1;
			return true;
		}
	}

	private boolean onDown(MotionEvent e) {
		if (mCallback != null) {
			mCallback.onDown();
		}
		return true;
	}

	private void onSingleTapUp(MotionEvent e) {
		if (isDoubleClick(e)) {
			// mBitmapCanvas.scale(e, false);
			if (mCallback != null) {
				mCallback.onDoubleClick();
			}
		} else {
			if (mCallback != null) {
				mCallback.onSingleTapUp();
			}
		}
	}

	private final TextureView.SurfaceTextureListener mSTListener = new TextureView.SurfaceTextureListener() {
		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			VideoView.this.onPlaybackStarted();
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
			VideoView.this.onSurfaceChanged(width, height);
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
			VideoView.this.onSurfaceDestroyed();
			return true;
		}

		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
			VideoView.this.onSurfaceCreated();
		}
	};

	private final SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			VideoView.this.onSurfaceCreated();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			VideoView.this.onSurfaceChanged(width, height);
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			VideoView.this.onSurfaceDestroyed();
		}

	};

	private class MyGestureListener implements GestureDetector.OnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			return VideoView.this.onDown(e);
		}

		@Override
		public void onShowPress(MotionEvent e) {
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			VideoView.this.onSingleTapUp(e);
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return true;
		}

	}

}
