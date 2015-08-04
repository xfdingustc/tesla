package com.transee.camera;

import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

public final class AndroidCamera implements Camera.PreviewCallback, Camera.AutoFocusCallback {

	private static final boolean DEBUG = false;
	private static final String TAG = "AndroidCamera";

	public static final int MIN_EXPLOSURE = 0;
	public static final int NORMAL_EXPLOSURE = 1;
	public static final int MAX_EXPLOSURE = 2;

	private Camera mCamera;
	private int mCameraId;
	private Camera.Parameters mCameraParams;
	private int mMinExplosure;
	private int mMaxExplosure;
	private boolean mPreviewing;
	private AndroidCamera.FrameCallback mFrameCallback;
	private AndroidCamera.AutoFocusCallback mAutoFocusCallback;

	public interface FrameCallback {
		void onPreviewFrame(int width, int height, byte[] data);
	}

	public interface AutoFocusCallback {
		void onAutoFocus(boolean success);
	}

	synchronized public void openDevice(SurfaceHolder holder) throws IOException {
		if (mCamera == null) {
			OpenCameraInterface interf = new OpenCamera().build();
			mCamera = interf.open();
			mCameraId = interf.getCameraId();
			if (mCamera == null) {
				throw new IOException();
			}
			mCameraParams = mCamera.getParameters();
			mMinExplosure = mCameraParams.getMinExposureCompensation();
			mMaxExplosure = mCameraParams.getMaxExposureCompensation();
			if (DEBUG) {
				Log.d(TAG, "explosure: " + mMinExplosure + "," + mMaxExplosure);
				Log.d(TAG, "autofocus: " + mCameraParams.getFocusMode());
				Log.d(TAG, "focus mode: " + mCameraParams.getFocusMode());
			}
		}
		mCamera.setPreviewDisplay(holder);
	}

	synchronized public void autoRotate(WindowManager windowManager) {
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(mCameraId, info);

		int rotation = windowManager.getDefaultDisplay().getRotation();
		int degrees = 0;

		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		// back-facing camera
		degrees = (info.orientation - degrees + 360) % 360;
		mCamera.setDisplayOrientation(degrees);
	}

	synchronized public void closeDevice() {
		if (mCamera != null) {
			stopPreview();

			mCamera.release();
			mCamera = null;

			mFrameCallback = null;
			mAutoFocusCallback = null;
		}
	}

	/*
	@SuppressLint("NewApi")
	synchronized public void setFocusArea(Rect rect, int weight) {
		if (mCamera != null) {
			Camera.Area area = new Camera.Area(rect, weight);
			List<Camera.Area> areas = new ArrayList<Camera.Area>();
			areas.add(area);
			mCameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			// mCameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			mCameraParams.setFocusAreas(areas);
			mCamera.setParameters(mCameraParams);
		}
	}
	*/

	synchronized public void startPreview(SurfaceHolder surfaceHolder, WindowManager windowManager, boolean isPortrait) {
		if (mCamera != null && !mPreviewing) {
			autoRotate(windowManager);

			boolean bChanged = false;

			if (DEBUG) {
				List<Camera.Size> sizes = mCameraParams.getSupportedPreviewSizes();
				for (Camera.Size size : sizes) {
					Log.d(TAG, "preview: " + size.width + "," + size.height + " : " + (double) size.width / size.height);
				}
				Camera.Size size = mCameraParams.getPreviewSize();
				Log.d(TAG, "prefered: " + size.width + "," + size.height);
				size = mCameraParams.getPreviewSize();
				Log.d(TAG, "current: " + size.width + "," + size.height);
			}

			Rect rect = surfaceHolder.getSurfaceFrame();
			int surfaceWidth = rect.width();
			int surfaceHeight = rect.height();
			if (isPortrait) {
				// TODO - is it same with the camera?
				int tmp = surfaceWidth;
				surfaceWidth = surfaceHeight;
				surfaceHeight = tmp;
			}

			if (DEBUG) {
				Log.d(TAG, "find optimal preview size for " + surfaceWidth + "," + surfaceHeight);
				Log.d(TAG, "apect ratio: " + (double) surfaceWidth / surfaceHeight);
			}

			Camera.Size optimalSize = getOptimalPreviewSize(surfaceWidth, surfaceHeight);

			if (DEBUG) {
				Log.d(TAG, "optimal preview size: " + optimalSize.width + "," + optimalSize.height);
			}
			/*
						int min = params.getMinExposureCompensation();
						int max = params.getMaxExposureCompensation();
						if (DEBUG) {
							int curr = params.getExposureCompensation();
							Log.d(TAG, "explosure: " + min + "," + max + "," + curr);
						}

						if (min < max) {
							params.setExposureCompensation(max);
							bChanged = true;
						}
			*/
			if (optimalSize != null) {
				mCameraParams.setPreviewSize(optimalSize.width, optimalSize.height);
				bChanged = true;
			}

			if (bChanged) {
				mCamera.setParameters(mCameraParams);
			}

			mCamera.startPreview();
			mPreviewing = true;
		}
	}

	private final Camera.Size getOptimalPreviewSize(int w, int h) {
		List<Camera.Size> sizes = mCameraParams.getSupportedPreviewSizes();
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double)w / h;
		if (sizes == null)
			return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Camera.Size size : sizes) {
			double ratio = (double)size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
			// if (size.width >= 640)
			// break;
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}

		return optimalSize;
	}

	synchronized public void stopPreview() {
		if (mCamera != null && mPreviewing) {
			mCamera.stopPreview();
			mPreviewing = false;
		}
	}

	synchronized public void autoFocus(AndroidCamera.AutoFocusCallback callback) {
		if (mCamera != null && mPreviewing) {
			mAutoFocusCallback = callback;
			// param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			mCamera.autoFocus(this);
		}
	}

	synchronized public boolean setAutoExplosure(int value) {
		if (mMinExplosure < mMaxExplosure) {
			switch (value) {
			case MIN_EXPLOSURE:
				value = mMinExplosure;
				break;
			default:
			case NORMAL_EXPLOSURE:
				value = (mMinExplosure + mMaxExplosure) / 2;
				break;
			case MAX_EXPLOSURE:
				value = mMaxExplosure;
				break;
			}
			if (DEBUG) {
				Log.d(TAG, "set auto explosure: " + value);
			}
			mCameraParams.setExposureCompensation(value);
			mCamera.setParameters(mCameraParams);
			return true;
		}
		return false;
	}

	synchronized public void requestPreviewFrame(AndroidCamera.FrameCallback callback) {
		if (mCamera != null && mPreviewing) {
			mFrameCallback = callback;
			mCamera.setOneShotPreviewCallback(this);
		}
	}

	@Override
	synchronized public void onAutoFocus(boolean success, Camera camera) {
		if (mCamera != null && mPreviewing) {
			if (mAutoFocusCallback != null) {
				mAutoFocusCallback.onAutoFocus(success);
			}
		}
	}

	@Override
	synchronized public void onPreviewFrame(byte[] data, Camera camera) {
		if (mCamera != null && mPreviewing) {
			if (mFrameCallback != null) {
				Camera.Size size = mCameraParams.getPreviewSize();
				if (DEBUG) {
					Log.d(TAG, "preview size: " + size.width + "*" + size.height);
				}
				mFrameCallback.onPreviewFrame(size.width, size.height, data);
			}
		}
	}
}
