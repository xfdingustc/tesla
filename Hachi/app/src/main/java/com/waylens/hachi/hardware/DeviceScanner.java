package com.waylens.hachi.hardware;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

/**
 * Created by Xiaofei on 2015/9/17.
 */
public class DeviceScanner extends Thread {
    private static final String TAG = DeviceScanner.class.getSimpleName();

    private static final String SERVICE_TYPE = "_ccam._tcp.local.";

    static final int SCAN_INTERVAL = 3000;

    public static final String SERVICE_VIDITCAM = "ViditCam";
    public static final String SERVICE_VIDIT_STUDIO = "Vidit Studio";

    @Nullable
    private WifiManager mWifiManager;
    private List<InetAddress> mAddress = new ArrayList<>();
    private List<JmDNS> mdns = new ArrayList<>();
    private WifiManager.MulticastLock mLock;
    private boolean mbRunning;

    private DeviceScannerListener mListener = null;

    public interface DeviceScannerListener {
        void onServiceResoledAsync(DeviceScanner thread, VdtCamera.ServiceInfo serviceInfo);

        void onRescanAsync(DeviceScanner thread);
    }

    public void setListener(DeviceScannerListener listener) {
        mListener = listener;
    }





    public DeviceScanner() {
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
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                if (mWifiManager != null) {
                    unlockWifi();
                }
            } catch (Exception e) {
                Logger.t(TAG).e("Error", e);
            }
        }
    }

    synchronized private void threadLoop() throws InterruptedException {
        while (mbRunning) {

            wait(SCAN_INTERVAL);

            if (!mbRunning) {
                break;
            }

            // TODO - sometimes, the service cannot be found
            // this is a workaround to find the service
            if (mListener != null) {
                mListener.onRescanAsync(this);
            }
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
            Logger.t(TAG).d("serviceAdded: " + event.getName() + ", " + event.getType());
            Logger.t(TAG).d(event.getInfo().toString());
            if (isRunning()) {
                // Vidit Camera, _ccam._tcp.local.
                event.getDNS().requestServiceInfo(event.getType(), event.getName(), 1);
            } else {
                Log.d(TAG, "serviceAdded: not running");
            }
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            Logger.t(TAG).d("serviceRemoved: " + event.getName() + ", " + event.getType());
            Logger.t(TAG).d(event.getInfo().toString());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            Logger.t(TAG).d("serviceResolved: " + event.getName() + ", " + event.getType());
            Logger.t(TAG).d(event.getInfo().toString());
            if (isRunning()) {
                ServiceInfo info = event.getInfo();
                Inet4Address[] addresses = info.getInet4Addresses();
                if (addresses.length > 0) {

                    Logger.t(TAG).d("address: " + addresses[0].toString());
                    String name = event.getName();
                    boolean bIsPcServer = name.equals(SERVICE_VIDIT_STUDIO);
                    String serverName = info.getServer();
                    int index = serverName.indexOf(".local.");
                    if (index >= 0) {
                        serverName = serverName.substring(0, index);
                    }
                    VdtCamera.ServiceInfo serviceInfo = new VdtCamera.ServiceInfo(addresses[0], info
                        .getPort(), serverName,
                        name, bIsPcServer);
                    if (mListener != null) {
                        mListener.onServiceResoledAsync(DeviceScanner.this, serviceInfo);
                    }
                }
            } else {
                Logger.t(TAG).d("serviceResolved: not running");
            }
        }
    };

    ServiceTypeListener mServiceTypeListener = new ServiceTypeListener() {
        @Override
        public void serviceTypeAdded(ServiceEvent event) {
            Logger.t(TAG).d("serviceTypeAdded: " + event.getName() + ", " + event.getType());
        }

        @Override
        public void subTypeForServiceTypeAdded(ServiceEvent event) {
            Logger.t(TAG).d("subTypeForServiceTypeAdded: " + event.getName() + ", " + event.getType());
        }
    };

    private void lockWifi(WifiManager wifiManager) {
        if (mLock == null) {
            Logger.t(TAG).d("--- Lock wifi ---");
            mLock = wifiManager.createMulticastLock(getClass().getName());
            mLock.setReferenceCounted(true);
            mLock.acquire();
        }
    }

    private void unlockWifi() {
        if (mLock != null) {
            mLock.release();
            mLock = null;

            Logger.t(TAG).d("=== Unlock wifi ===");
        }
    }
}
