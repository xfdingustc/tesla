package com.transee.viditcam.app;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.transee.camera.AndroidCamera;

import com.transee.common.BeepManager;
import com.transee.common.Utils;
import com.transee.common.ViewfinderView;
import com.waylens.hachi.R;
import com.transee.viditcam.actions.DialogBuilder;
import com.transee.viditcam.actions.ShowScanResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddCameraActivity extends BaseActivity {

	static final boolean DEBUG = false;
	static final String TAG = "AddCameraActivity";

	private ViewfinderView mViewfinder;
	private SurfaceView mSurfaceView;
	private AndroidCamera mAndroidCamera;
	//private BarcodeDecoder mBarcodeDecoder;
	private SurfaceHolder mSurfaceHolder;
	private MySurfaceHolderCallback mSurfaceHolderCallback;
	private boolean mbSurfaceReady;
	private long mAutoFocusTime; // tick when last focused
	private Pattern mPattern;
	private String mWifiName;
	private String mWifiPassword;

	@Override
	protected void requestContentView() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_add_camera);
	}

	@Override
	protected void onCreateActivity(Bundle savedInstanceState) {
		mViewfinder = (ViewfinderView)findViewById(R.id.viewfinderView1);

		mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
		mSurfaceHolder = mSurfaceView.getHolder();

		mSurfaceHolderCallback = new MySurfaceHolderCallback();
		mSurfaceHolder.addCallback(mSurfaceHolderCallback);

		ImageButton button;
		View.OnClickListener onClickButton = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onExplosureButtonClicked(v);
			}
		};

		button = (ImageButton)findViewById(R.id.imageButton1);
		button.setOnClickListener(onClickButton);
		button = (ImageButton)findViewById(R.id.imageButton2);
		button.setOnClickListener(onClickButton);
		button = (ImageButton)findViewById(R.id.imageButton3);
		button.setOnClickListener(onClickButton);

		toggleFullScreen();
	}

	@Override
	protected void onDestroyActivity() {
		mSurfaceHolder.removeCallback(mSurfaceHolderCallback);
	}

	@Override
	protected void onStartActivity() {
		//mBarcodeDecoder = new MyBarcodeDecoder();
		//mBarcodeDecoder.start();
		checkSurface();
	}

	@Override
	protected void onStopActivity() {
		closeAndroidCamera();
//		if (mBarcodeDecoder != null) {
//			mBarcodeDecoder.interrupt();
//			mBarcodeDecoder = null;
//		}
	}

	@Override
	protected void onReleaseUI() {
	}

	@Override
	protected void onInitUI() {
	}

	@Override
	protected void onSetupUI() {
	}

	@Override
	public void onBackPressed() {
		finish();
		// ThisApp.slideOutToRight(this, false);
	}

	private void checkSurface() {
		if (mbSurfaceReady) {
			openAndroidCamera();
		} else {
			closeAndroidCamera();
		}
	}

	private void openAndroidCamera() {
		try {
			if (mAndroidCamera == null) {
				mAndroidCamera = new AndroidCamera();
			}

			mAndroidCamera.openDevice(mSurfaceHolder);

			/*
			 * int width = mSurfaceView.getWidth(); int height =
			 * mSurfaceView.getHeight(); if (isPortrait()) { int tmp = width;
			 * width = height; height = tmp; } Rect rect =
			 * getViewfinderRectOnPreview(width, height);
			 */
			mAndroidCamera.startPreview(mSurfaceHolder, getWindowManager(), isPortrait());
			// mAndroidCamera.setFocusArea(rect, 1);

			startAutoFocus();

		} catch (Exception e) {
			Log.w(TAG, "cannot open camera");
			DialogBuilder builder = new DialogBuilder(this);
			builder.setMsg(R.string.msg_cannot_open_android_camera);
			builder.setButtons(DialogBuilder.DLG_OK);
			builder.show();
		}
	}

	private AndroidCamera.AutoFocusCallback mAutoFocusCallback = new AndroidCamera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(final boolean success) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onCameraAutoFocus(success);
				}
			});
		}
	};

	private void startAutoFocus() {
		if (mAndroidCamera != null) {
			mAndroidCamera.autoFocus(mAutoFocusCallback);
			mAutoFocusTime = System.currentTimeMillis();
		}
	}

	private void requestPreviewData() {
		if (mAndroidCamera != null) {
			mAndroidCamera.requestPreviewFrame(mFrameCallback);
		}
	}

	private void onExplosureButtonClicked(View view) {
		int step;
		switch (view.getId()) {
		case R.id.imageButton1:
			step = AndroidCamera.MIN_EXPLOSURE;
			break;
		default:
		case R.id.imageButton2:
			step = AndroidCamera.NORMAL_EXPLOSURE;
			break;
		case R.id.imageButton3:
			step = AndroidCamera.MAX_EXPLOSURE;
			break;
		}
		if (mAndroidCamera != null) {
			mAndroidCamera.setAutoExplosure(step);
		}
	}

	private void onCameraAutoFocus(boolean success) {
		if (mAndroidCamera != null) {
			if (success) {
				requestPreviewData();
			} else {
				if (DEBUG) {
					Log.d(TAG, "autoFocus failed");
				}
				startAutoFocus();
			}
		}
	}

	private Rect getViewfinderRectOnPreview(int cameraWidth, int cameraHeight) {
		Rect rect = new Rect();

		// get the rect that's relative to the SurfaceView
		mViewfinder.getRect(rect);
		Utils.translateRect(mViewfinder, rect, true);
		Utils.translateRect(mSurfaceView, rect, false);

		// in portrait mode, the image is still as in landscale mode (dataWidth
		// > dataHeight)
		// TODO : verify all devices
		int width = mSurfaceView.getWidth();
		int height = mSurfaceView.getHeight();

		rect.left -= 32;
		if (rect.left < 0)
			rect.left = 0;
		rect.top -= 32;
		if (rect.top < 0)
			rect.top = 0;
		rect.right += 32;
		if (rect.right > width)
			rect.right = width;
		rect.bottom += 32;
		if (rect.bottom > height)
			rect.bottom = height;

		if (isPortrait()) {
			int tmp;

			tmp = rect.left;
			rect.left = rect.top;
			rect.top = tmp;

			tmp = rect.right;
			rect.right = rect.bottom;
			rect.bottom = tmp;

			tmp = width;
			width = height;
			height = tmp;
		}

		rect.left = rect.left * cameraWidth / width;
		rect.right = rect.right * cameraWidth / width;
		rect.top = rect.top * cameraHeight / height;
		rect.bottom = rect.bottom * cameraHeight / height;

		return rect;
	}

	private void onCameraPreviewFrame(int dataWidth, int dataHeight, byte[] data) {
//		if (mBarcodeDecoder != null) {
//			Rect rect = getViewfinderRectOnPreview(dataWidth, dataHeight);
//			mBarcodeDecoder.requestDecode(data, dataWidth, dataHeight, rect);
//		}
	}

	private boolean shouldRedoFocus() {
		int elapsed = (int)(System.currentTimeMillis() - mAutoFocusTime);
		return elapsed >= 2000;
	}

	private void onBarcodeDecodeError() {
		if (DEBUG) {
			Log.d(TAG, "decode error");
		}
//		if (mBarcodeDecoder != null && mAndroidCamera != null) {
//			if (!shouldRedoFocus()) {
//				requestPreviewData();
//			} else {
//				startAutoFocus();
//			}
//		}
	}

	private boolean parseWifiInfo(String result) {
		if (mPattern == null) {
			// <a>AP</a><p>pass<p>
			mPattern = Pattern.compile("<a>(\\w*)</a>.*<p>(\\w*)</?p>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		}
		Matcher matcher = mPattern.matcher(result);
		if (matcher.find() && matcher.groupCount() == 2) {
			mWifiName = matcher.group(1);
			mWifiPassword = matcher.group(2);
			return true;
		}
		return false;
	}

	private void onClickOK() {
		Bundle b = new Bundle();
		b.putString("wifiName", mWifiName);
		b.putString("wifiPassword", mWifiPassword);
		Intent intent = new Intent();
		intent.putExtras(b);
		setResult(RESULT_OK, intent);
		finish();
	}

	private void showResult(String result, boolean bDecoded) {
		ShowScanResult action = new ShowScanResult(this, mWifiName, mWifiPassword, result, bDecoded) {
			@Override
			public void onClickYes() {
				onClickOK();
			}

			@Override
			public void onDismiss(ShowScanResult action) {
				if (!action.isYesClicked()) {
					requestPreviewData();
				}
			}
		};
		action.show();
	}

	private void onBarcodeDecodeDone(String result) {
		if (DEBUG) {
			Log.d(TAG, "decode done: " + result);
		}
//		if (mBarcodeDecoder != null && mAndroidCamera != null) {
//			if (parseWifiInfo(result)) {
//				showResult(result, true);
//				BeepManager.play(this, R.raw.beep1, false);
//			} else {
//				showResult(result, false);
//				BeepManager.play(this, R.raw.beep2, false);
//			}
//		}
	}

	AndroidCamera.FrameCallback mFrameCallback = new AndroidCamera.FrameCallback() {
		@Override
		public void onPreviewFrame(final int width, final int height, final byte[] data) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onCameraPreviewFrame(width, height, data);
				}
			});
		}
	};

	private void closeAndroidCamera() {
		if (mAndroidCamera != null) {
			mAndroidCamera.stopPreview();
			mAndroidCamera.closeDevice();
			mAndroidCamera = null;
		}
	}

//	class MyBarcodeDecoder extends BarcodeDecoder {
//
//		@Override
//		public void onDecodeError(final BarcodeDecoder decoder) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					if (decoder == mBarcodeDecoder) {
//						onBarcodeDecodeError();
//					}
//				}
//			});
//		}
//
//		@Override
//		public void onDecodeDone(final BarcodeDecoder decoder, final String result) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					if (decoder == mBarcodeDecoder) {
//						onBarcodeDecodeDone(result);
//					}
//				}
//			});
//		}
//
//	}

	class MySurfaceHolderCallback implements SurfaceHolder.Callback {

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			mbSurfaceReady = true;
			checkSurface();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			mbSurfaceReady = false;
			checkSurface();
		}

	}
}
