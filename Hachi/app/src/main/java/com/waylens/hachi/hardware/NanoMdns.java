package com.waylens.hachi.hardware;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

import android.content.Context;
import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;

abstract public class NanoMdns {

	final static String TAG = "NanoMdns";

	static final String SERVICE_VIDITCAM = "Vidit Camera";
	static final String SERVICE_VIDIT_STUDIO = "Vidit Studio";

	abstract public void onServiceResoledAsync(NanoMdns mdns, VdtCamera.ServiceInfo serviceInfo);

	private final static String MDNS_GROUP = "224.0.0.251";
	private final static int MDNS_PORT = 5353;

	private final WifiManager mWifiManager;
	private final WifiManager.MulticastLock mLock;
	private int mSessionCounter = 0;

	ArrayList<SDThread> mThreadList = new ArrayList<SDThread>();

	public NanoMdns(Context context) {
		mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		mLock = mWifiManager.createMulticastLock("NanoMdns");
		mLock.setReferenceCounted(true);
	}

	public void startWork() {
		stopAll();
		startAll();
	}

	public void stopWork() {
		stopAll();
	}

	public boolean verify(VdtCamera.ServiceInfo serviceInfo) {
		if (serviceInfo.sessionCounter != mSessionCounter) {
			Log.d(TAG, "session changed, " + serviceInfo.sessionCounter + ", " + mSessionCounter);
			return false;
		}
		return true;
	}

	private void startAll() {
		try {
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface ni = en.nextElement();
				if (!ni.isLoopback()) {
					Enumeration<InetAddress> enumIpAddr = ni.getInetAddresses();
					while (enumIpAddr.hasMoreElements()) {
						InetAddress addr = enumIpAddr.nextElement();
						if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
							startOne(ni, addr);
						}
					}
				}
			}
		} catch (Exception e) {
			Log.d(TAG, "startWork failed");
			e.printStackTrace();
		}
	}

	private void startOne(NetworkInterface ni, InetAddress addr) {
		SDThread thread = new SDThread(ni, addr);
		mThreadList.add(thread);
		thread.start();
	}

	private void stopAll() {
		mSessionCounter++;
		for (SDThread thread : mThreadList) {
			thread.shutdown();
		}
		mThreadList.clear();
	}

	class SDThread extends Thread {

		private final NetworkInterface mNetworkInterface;
		private final int mMyCounter;
		private InetAddress mGroup = null;
		private MulticastSocket _mSocket = null; // used by caller
		private MulticastSocket mSocket = null; // used by thread
		private boolean mbLocked = false;
		private byte[] mData;
		private int mIndex;
		protected int mNumFound = 0;

		public SDThread(NetworkInterface ni, InetAddress addr) {
			mMyCounter = mSessionCounter;
			mNetworkInterface = ni;
			setName("NanoMdns");
		}

		@Override
		public void run() {
			try {
				runOnce();
			} catch (Exception e) {
				Log.d(TAG, e.getMessage());
			}

			closeSocket();

			if (mbLocked) {
				mLock.release();
			}
		}

		public void shutdown() {
			interrupt();
			closeSocket();
		}

		private synchronized void openSocket() throws IOException {
			mSocket = new MulticastSocket(MDNS_PORT);
			_mSocket = mSocket;
			Log.d(TAG, "socket buffer: " + mSocket.getReceiveBufferSize());
		}

		private synchronized void closeSocket() {
			if (_mSocket != null) {
				// mSocket.leaveGroup(mGroup);
				_mSocket.close();
				_mSocket = null;
			}
		}

		private void runOnce() throws IOException {

			openSocket();

			// setup socket
			mSocket.setNetworkInterface(mNetworkInterface);
			mSocket.setLoopbackMode(true);
			mSocket.setTimeToLive(16);

			// join multicast group
			InetAddress group = InetAddress.getByName(MDNS_GROUP);
			mSocket.joinGroup(group);
			mGroup = group;

			// lock wifi
			mLock.acquire();
			mbLocked = true;

			mData = new byte[256];

			// send query
			createDatagram();
			DatagramPacket packet = new DatagramPacket(mData, mIndex, mGroup, MDNS_PORT);
			mSocket.send(packet);

			while (!isInterrupted()) {
				mSocket.setSoTimeout(3000);
				packet.setData(mData);
				try {
					mSocket.receive(packet);
				} catch (SocketTimeoutException e) {
					Log.e(TAG, "SocketTimeoutException");
					createDatagram();
					packet.setData(mData, 0, mIndex);
					mSocket.send(packet);
					continue;
				}
				mIndex = 0;
				//mIndex = packet.getLength();
				parsePacket();
			}
		}

		private void createDatagram() {
			mIndex = 0;

			write16(0); // id
			write16(0); // flags
			write16(2); // QDCOUNT
			write16(0); // ANCOUNT
			write16(0); // NSCOUNT
			write16(0); // ARCOUNT

			writeQuery(SERVICE_VIDIT_STUDIO);
			writeQuery(SERVICE_VIDITCAM);
		}

		private void writeQuery(String name) {
			writeString(name);
			writeString("_ccam");
			writeString("_tcp");
			writeString("local");
			mData[mIndex++] = 0;

			//write16(12);
			//write16(1);
			write16(33);
			write16(0x8001);
		}

		private void parsePacket() {
			Log.d(TAG, "[ parsePacket ]");

			if (read16() != 0) {
				// transaction id
				Log.d(TAG, "transaction id");
				return;
			}
			int tmp = read16();
			if (tmp != 0x8400) {
				//Log.d(TAG, "flags: " + Integer.toHexString(tmp));
				if (tmp != 0x8000)
					return;
			}
			if (read16() != 0) {
				// QDCOUNT
				Log.d(TAG, "QDCOUNT");
				return;
			}
			int an = read16();
			if (an <= 0) {
				Log.d(TAG, "ANCOUNT");
				return;
			}
			mIndex += 4; // NS, AR

			String serviceName = null;
			int ttl = -1;
			int port = -1;
			int ipv4 = 0;

			for (int i = 0; i < an; i++) {
				String name = parseName();
				//Log.d(TAG, "name: " + name);
				int qtype = read16();
				mIndex += 2; // flags
				ttl = read32();
				int length = read16();
				switch (qtype) {
				case 33: // server selection
					mIndex += 4; // priority, weight
					port = read16();
					mIndex += length - 6;
					serviceName = name;
					break;
				case 1: // IPv4 address
					ipv4 = read32();
					mIndex += length - 4;
					break;
				default:
					mIndex += length;
					break;
				}
			}

			Log.d(TAG, "port: " + port + ", ttl: " + ttl + ", ip: " + Integer.toHexString(ipv4));

			if (serviceName != null && port > 0 && ipv4 != 0) {
				if (ttl <= 0) {
					// TODO
				} else {
					InetAddress inetAddress = null;
					byte[] addr = new byte[4];
					addr[0] = (byte)((ipv4 >> 24) & 0xFF);
					addr[1] = (byte)((ipv4 >> 16) & 0xFF);
					addr[2] = (byte)((ipv4 >> 8) & 0xFF);
					addr[3] = (byte)(ipv4 & 0xFF);
					try {
						inetAddress = InetAddress.getByAddress(addr);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}

					if (inetAddress != null) {
						int i = serviceName.indexOf('.');
						if (i > 0) {
							String domain = serviceName.substring(i, serviceName.length());
							if (domain.equals("._ccam._tcp.local")) {
								serviceName = serviceName.substring(0, i);
								boolean bIsPcServer = serviceName.equals(SERVICE_VIDIT_STUDIO);
								String serverName = "Transee"; // TODO
								VdtCamera.ServiceInfo info = new VdtCamera.ServiceInfo(inetAddress, port, serverName,
										serviceName, bIsPcServer);
								info.sessionCounter = mMyCounter;
								onServiceResoledAsync(NanoMdns.this, info);
								mNumFound++;
							}
						}
					}
				}
			}
		}

		private String parseName() {
			StringBuilder sb = new StringBuilder();
			int mOldIndex = -1;
			while (true) {
				int n = mData[mIndex++];
				if (n == 0) {
					break;
				}
				if ((n & 0xC0) == 0xC0) {
					int pos = ((n & 0x30) << 8) | mData[mIndex++];
					if (mOldIndex < 0) {
						mOldIndex = mIndex;
					}
					mIndex = pos;
					continue;
				}
				if (sb.length() > 0) {
					sb.append('.');
				}
				for (int i = 0; i < n; i++) {
					// TODO: handle utf8
					sb.append((char)mData[mIndex++]);
				}
			}
			if (mOldIndex > 0) {
				mIndex = mOldIndex;
			}
			return sb.toString();
		}

		private final void write16(int value) {
			mData[mIndex++] = (byte)((value >> 8) & 0xFF);
			mData[mIndex++] = (byte)value;
		}

		private final void writeString(String s) {
			int n = s.length();
			mData[mIndex++] = (byte)n;
			for (int i = 0; i < n; i++) {
				mData[mIndex++] = (byte)s.charAt(i);
			}
		}

		private final int read32() {
			int result = ((int)mData[mIndex++] & 0xFF) << 24;
			result |= ((int)mData[mIndex++] & 0xFF) << 16;
			result |= ((int)mData[mIndex++] & 0xFF) << 8;
			result |= (int)mData[mIndex++] & 0xFF;
			return result;
		}

		private final int read16() {
			int result = ((int)mData[mIndex++] & 0xFF) << 8;
			result |= (int)mData[mIndex++] & 0xFF;
			return result;
		}
	}
}
