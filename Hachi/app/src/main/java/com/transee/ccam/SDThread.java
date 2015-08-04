package com.transee.ccam;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

// this thread creates and starts JmDNS
abstract public class SDThread extends Thread {

	static final int SCAN_INTERVAL = 3000;

	static final boolean DEBUG = false;
	static final String TAG = "SDThread";

	static final String SERVICE_TYPE = "_ccam._tcp.local.";

	public static final String SERVICE_VIDITCAM = "ViditCam";
	public static final String SERVICE_VIDIT_STUDIO = "Vidit Studio";

	@Nullable
	private WifiManager mWifiManager;
	private ArrayList<InetAddress> mAddress = new ArrayList<InetAddress>();
	private ArrayList<JmDNS> mdns = new ArrayList<JmDNS>();
	private WifiManager.MulticastLock mLock;
	private boolean mbRunning;

	abstract public void onServiceResoledAsync(SDThread thread, Camera.ServiceInfo serviceInfo);

	abstract public void onRescanAsync(SDThread thread);

	public SDThread(Context context) {
		super("ServiceDiscovery");
	}

	// API
	synchronized public void startWork(WifiManager wifiManager) {
		if (!mbRunning) {
			mbRunning = true;
			mWifiManager = wifiManager;
			start();
		}
	}

	// API
	synchronized public void stopWork() {
		if (mbRunning) {
			mbRunning = false;
			interrupt();
			notifyAll();
		}
	}

	// API
	synchronized public boolean isRunning() {
		return mbRunning;
	}

	@Override
	public void run() {
		try {

			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface ni = en.nextElement();
				Enumeration<InetAddress> enumIpAddr = ni.getInetAddresses();
				while (enumIpAddr.hasMoreElements()) {
					InetAddress addr = enumIpAddr.nextElement();
					if (!addr.isLoopbackAddress()) {
						mAddress.add(addr);
					}
				}
			}

		} catch (Exception e) {
			Log.d(TAG, "startWork failed");
			e.printStackTrace();
		}

		if (mWifiManager != null) {
			lockWifi(mWifiManager);
		}

		try {

			for (InetAddress addr : mAddress) {
				JmDNS dns = JmDNS.create(addr, SERVICE_VIDITCAM);
				mdns.add(dns);
			}

			for (InetAddress addr : mAddress) {
				JmDNS dns = JmDNS.create(addr, SERVICE_VIDIT_STUDIO);
				mdns.add(dns);
			}

			try {
				for (JmDNS dns : mdns) {
					dns.addServiceListener(SERVICE_TYPE, mServiceListener);
				}
				threadLoop();
			} finally {
				try {
					for (JmDNS dns : mdns) {
						dns.close();
					}
				} catch (Exception e) {
					Log.e(TAG, "Error", e);
				}
			}

		} catch (IOException e) {
			Log.d(TAG, "exception");
			e.printStackTrace();
		} catch (InterruptedException e) {
			if (DEBUG) {
				Log.d(TAG, "interrupted");
			}
		} catch (Exception e) {
			Log.e("test", "==================", e);
		} finally {
			try {
				if (mWifiManager != null) {
					unlockWifi();
				}
			} catch (Exception e) {
				Log.e(TAG, "Error", e);
			}
		}
	}

	synchronized private void threadLoop() throws InterruptedException {
		while (mbRunning) {

			wait(SCAN_INTERVAL);

			if (!mbRunning)
				break;

			// TODO - sometimes, the service cannot be found
			// this is a workaround to find the service
			onRescanAsync(this);
			if (mdns != null) {
				for (JmDNS dns : mdns) {
					dns.removeServiceListener(SERVICE_TYPE, mServiceListener);
					dns.addServiceListener(SERVICE_TYPE, mServiceListener);
				}
			}
		}
	}

	ServiceListener mServiceListener = new ServiceListener() {
		@Override
		public void serviceAdded(ServiceEvent event) {
			if (DEBUG) {
				Log.d(TAG, "serviceAdded: " + event.getName() + ", " + event.getType());
				Log.d(TAG, event.getInfo().toString());
			}
			if (isRunning()) {
				// Vidit Camera, _ccam._tcp.local.
				event.getDNS().requestServiceInfo(event.getType(), event.getName(), 1);
			} else {
				Log.d(TAG, "serviceAdded: not running");
			}
		}

		@Override
		public void serviceRemoved(ServiceEvent event) {
			if (DEBUG) {
				Log.d(TAG, "serviceRemoved: " + event.getName() + ", " + event.getType());
				Log.d(TAG, event.getInfo().toString());
			}
		}

		@Override
		public void serviceResolved(ServiceEvent event) {
			if (DEBUG) {
				Log.d(TAG, "serviceResolved: " + event.getName() + ", " + event.getType());
				Log.d(TAG, event.getInfo().toString());
			}
			if (isRunning()) {
				ServiceInfo info = event.getInfo();
				Inet4Address[] addresses = info.getInet4Addresses();
				if (addresses.length > 0) {
					if (DEBUG) {
						Log.d(TAG, "address: " + addresses[0].toString());
					}
					String name = event.getName();
					boolean bIsPcServer = name.equals(SERVICE_VIDIT_STUDIO);
					String serverName = info.getServer();
					int index = serverName.indexOf(".local.");
					if (index >= 0) {
						serverName = serverName.substring(0, index);
					}
					Camera.ServiceInfo serviceInfo = new Camera.ServiceInfo(addresses[0], info.getPort(), serverName,
							name, bIsPcServer);
					onServiceResoledAsync(SDThread.this, serviceInfo);
				}
			} else {
				Log.d(TAG, "serviceResolved: not running");
			}
		}
	};

	ServiceTypeListener mServiceTypeListener = new ServiceTypeListener() {
		@Override
		public void serviceTypeAdded(ServiceEvent event) {
			Log.d(TAG, "serviceTypeAdded: " + event.getName() + ", " + event.getType());
		}

		@Override
		public void subTypeForServiceTypeAdded(ServiceEvent event) {
			Log.d(TAG, "subTypeForServiceTypeAdded: " + event.getName() + ", " + event.getType());
		}
	};

	private void lockWifi(WifiManager wifiManager) {
		if (mLock == null) {
			if (DEBUG) {
				Log.d(TAG, "--- Lock wifi ---");
			}
			mLock = wifiManager.createMulticastLock(getClass().getName());
			mLock.setReferenceCounted(true);
			mLock.acquire();
		}
	}

	private void unlockWifi() {
		if (mLock != null) {
			mLock.release();
			mLock = null;
			if (DEBUG) {
				Log.d(TAG, "=== Unlock wifi ===");
			}
		}
	}

}
