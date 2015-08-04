package com.transee.ccam;

import android.os.Handler;
import android.util.Log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

// use SSID + hostString to identify a camera

public class Camera {

	static final boolean DEBUG = false;
	static final String TAG = "Camera";

	static public class ServiceInfo {
		public String ssid;
		public final InetAddress inetAddr;
		public final int port;
		public final String serverName;
		public final String serviceName;
		public final boolean bPcServer;

		public ServiceInfo(InetAddress inetAddr, int port, String serverName, String serviceName, boolean bPcServer) {
			this.ssid = "";
			this.inetAddr = inetAddr;
			this.port = port;
			this.serverName = serverName;
			this.serviceName = serviceName;
			this.bPcServer = bPcServer;
		}
	}

	public interface Callback {
		public void onConnected(Camera camera);

		public void onDisconnected(Camera camera);

		public void onStateChanged(Camera camera);

		public void onBtStateChanged(Camera camera);

		public void onGpsStateChanged(Camera camera);

		public void onWifiStateChanged(Camera camera);

		public void onStartRecordError(Camera camera, int error);

		public void onHostSSIDFetched(Camera camera, String ssid);

		public void onScanBtDone(Camera camera);

		public void onBtDevInfo(Camera camera, int type, String mac, String name);

		public void onStillCaptureStarted(Camera camera, boolean bOneShot);

		public void onStillPictureInfo(Camera camera, boolean bCapturing, int numPictures, int burstTicks);

		public void onStillCaptureDone(Camera camera);
	}

	static public class CallbackImpl implements Callback {
		@Override
		public void onConnected(Camera camera) {
		}

		@Override
		public void onDisconnected(Camera camera) {
		}

		@Override
		public void onStateChanged(Camera camera) {
		}

		@Override
		public void onBtStateChanged(Camera camera) {
		}

		@Override
		public void onGpsStateChanged(Camera camera) {
		}

		@Override
		public void onWifiStateChanged(Camera camera) {
		}

		@Override
		public void onStartRecordError(Camera camera, int error) {
		}

		@Override
		public void onHostSSIDFetched(Camera camera, String ssid) {
		}

		@Override
		public void onScanBtDone(Camera camera) {
		}

		@Override
		public void onBtDevInfo(Camera camera, int type, String mac, String name) {
		}

		@Override
		public void onStillCaptureStarted(Camera camera, boolean bOneShot) {
		}

		@Override
		public void onStillPictureInfo(Camera camera, boolean bCapturing, int numPictures, int burstTicks) {
		}

		@Override
		public void onStillCaptureDone(Camera camera) {
		}
	}

	private ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
	private final Handler mHandler;
	private final ServiceInfo mServiceInfo;
	private final AbsCameraClient mClient;

	private InetSocketAddress mPreviewAddress;

	private CameraState mStates = new CameraState();
	private BtState mBtStates = new BtState();
	private GpsState mGpsStates = new GpsState();
	private WifiState mWifiStates = new WifiState();

	public Camera(Camera.ServiceInfo serviceInfo) {
		mServiceInfo = serviceInfo;
		mHandler = new Handler();
		mClient = serviceInfo.bPcServer ? new NullCameraClient() : new MyCameraClient(serviceInfo.inetAddr,
				serviceInfo.port);
	}

	// API
	public boolean isPcServer() {
		return mServiceInfo.bPcServer;
	}

	// API
	public String getServerName() {
		return mServiceInfo.serverName;
	}

	// API
	public InetAddress getAddress() {
		return mServiceInfo.inetAddr;
	}

	// API
	public void addCallback(Callback callback) {
		mCallbacks.add(callback);
	}

	// API
	public void removeCallback(Callback callback) {
		mCallbacks.remove(callback);
	}

	// API
	public String getSSID() {
		return mServiceInfo.ssid;
	}

	// API
	public String getHostString() {
		return mServiceInfo.inetAddr.getHostAddress();
	}

	// API
	public boolean idMatch(String ssid, String hostString) {
		if (ssid == null || hostString == null)
			return false;
		String myHostString = getHostString();
		if (mServiceInfo.ssid == null || myHostString == null)
			return false;
		return mServiceInfo.ssid.equals(ssid) && myHostString.equals(hostString);
	}

	// API
	public AbsCameraClient getClient() {
		return mClient;
	}

	// API - camera can be null
	static public CameraState getCameraStates(Camera camera) {
		return camera == null ? CameraState.nullState : camera.mStates;
	}

	// API - camera can be null
	static public BtState getBtStates(Camera camera) {
		return camera == null ? BtState.nullState : camera.mBtStates;
	}

	// API - camera can be null
	static public WifiState getWifiStates(Camera camera) {
		return camera == null ? WifiState.nullState : camera.mWifiStates;
	}

	// API
	public InetSocketAddress getInetSocketAddress() {
		return mClient.getInetSocketAddress();
	}

	// API
	public InetSocketAddress getPreviewAddress() {
		return mPreviewAddress;
	}

	private void onCameraConnected() {
		InetSocketAddress addr = mClient.getInetSocketAddress();
		if (addr != null) {
			mPreviewAddress = new InetSocketAddress(addr.getAddress(), 8081);
			for (Callback callback : mCallbacks) {
				callback.onConnected(this);
			}
		}
	}

	private ArrayList<Callback> createCallbackCopy() {
		ArrayList<Callback> c = new ArrayList<Callback>();
		for (Callback callback : mCallbacks) {
			c.add(callback);
		}
		return c;
	}

	private void onCameraDisconnected() {
		// callback may unregister itself; so use a copy
		ArrayList<Callback> c = createCallbackCopy();
		for (Callback callback : c) {
			callback.onDisconnected(this);
		}
	}

	// called on camera thread
	private void initCameraState() {
		if (mClient instanceof CameraClient) {
			CameraClient client = (CameraClient)mClient;
			client.cmd_Cam_getApiVersion();
			client.cmd_fw_getVersion();
			client.cmd_fw_getVersion();
			client.cmd_Cam_get_Name();
			client.cmd_Rec_List_Resolutions(); // see if still capture is supported
			client.cmd_Cam_get_getAllInfor();
			client.cmd_Cam_get_State();
			client.cmd_Network_GetWLanMode();
			client.cmd_Network_GetHostNum();
		}
	}

	// client told us camera state has changed,
	// so synchronize our state with it (on main thread)
	private void syncCameraState() {
		if (mClient.syncState(mStates)) {
			for (Callback callback : mCallbacks) {
				callback.onStateChanged(this);
			}
		}
	}

	private void syncBtState() {
		if (mClient.syncBtState(mBtStates)) {
			for (Callback callback : mCallbacks) {
				callback.onBtStateChanged(this);
			}
		}
	}

	private void syncGpsState() {
		if (mClient.syncGpsState(mGpsStates)) {
			for (Callback callback : mCallbacks) {
				callback.onGpsStateChanged(this);
			}
		}
	}

	private void syncWifiState() {
		if (mClient.syncWifiState(mWifiStates)) {
			for (Callback callback : mCallbacks) {
				callback.onWifiStateChanged(this);
			}
		}
	}

	private void onStartRecordError(int error) {
		for (Callback callback : mCallbacks) {
			callback.onStartRecordError(this, error);
		}
	}

	private void onHostSSIDFetched(String ssid) {
		for (Callback callback : mCallbacks) {
			callback.onHostSSIDFetched(this, ssid);
		}
	}

	private void onScanBtDone() {
		for (Callback callback : mCallbacks) {
			callback.onScanBtDone(this);
		}
	}

	private void onBtDevInfo(int type, String mac, String name) {
		for (Callback callback : mCallbacks) {
			callback.onBtDevInfo(this, type, mac, name);
		}
	}

	private void onStillCaptureStarted(boolean bOneShot) {
		for (Callback callback : mCallbacks) {
			callback.onStillCaptureStarted(this, bOneShot);
		}
	}

	private void onStillPictureInfo(boolean bCapturing, int numPictures, int burst_ticks) {
		for (Callback callback : mCallbacks) {
			callback.onStillPictureInfo(this, bCapturing, numPictures, burst_ticks);
		}
	}

	private void onStillCaptureDone() {
		for (Callback callback : mCallbacks) {
			callback.onStillCaptureDone(this);
		}
	}

	private Runnable mSyncStateAction = new Runnable() {
		@Override
		public void run() {
			if (DEBUG) {
				Log.d(TAG, "onCameraStateChanged");
			}
			syncCameraState();
		}
	};

	private Runnable mSyncBtStateAction = new Runnable() {
		@Override
		public void run() {
			if (DEBUG) {
				Log.d(TAG, "onBtStateChanged");
			}
			syncBtState();
		}
	};

	private Runnable mSyncGpsStateAction = new Runnable() {
		@Override
		public void run() {
			if (DEBUG) {
				Log.d(TAG, "onGpsStateChanged");
			}
			syncGpsState();
		}
	};

	private Runnable mSyncWifiStateAction = new Runnable() {
		@Override
		public void run() {
			if (DEBUG) {
				Log.d(TAG, "onWifiStateChanged");
			}
			syncWifiState();
		}
	};

	class MyCameraClient extends CameraClient {

		public MyCameraClient(InetAddress host, int port) {
			super(host, port);
		}

		@Override
		public void onConnected() {
			initCameraState();
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					onCameraConnected();
				}
			});
		}

		@Override
		public void onDisconnected() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					onCameraDisconnected();
				}
			});
		}

		@Override
		public void onCameraStateChanged() {
			mHandler.post(mSyncStateAction);
		}

		@Override
		public void onBtStateChanged() {
			mHandler.post(mSyncBtStateAction);
		}

		@Override
		public void onGpsStateChanged() {
			mHandler.post(mSyncGpsStateAction);
		}

		@Override
		public void onWifiStateChanged() {
			mHandler.post(mSyncWifiStateAction);
		}

		@Override
		public void onStartRecordError(final int error) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Camera.this.onStartRecordError(error);
				}
			});
		}

		@Override
		public void onHostSSIDFetched(final String ssid) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Camera.this.onHostSSIDFetched(ssid);
				}
			});
		}

		@Override
		public void onScanBtDone() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Camera.this.onScanBtDone();
				}
			});
		}

		@Override
		public void onBtDevInfo(final int type, final String mac, final String name) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Camera.this.onBtDevInfo(type, mac, name);
				}
			});
		}

		@Override
		public void onStillCaptureStarted(final boolean bOneShot) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Camera.this.onStillCaptureStarted(bOneShot);
				}
			});
		}

		@Override
		public void onStillPictureInfo(final boolean bCapturing, final int numPictures, final int burst_ticks) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Camera.this.onStillPictureInfo(bCapturing, numPictures, burst_ticks);
				}
			});
		}

		@Override
		public void onStillCaptureDone() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					Camera.this.onStillCaptureDone();
				}
			});
		}

	}
}
