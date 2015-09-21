package com.waylens.hachi.hardware;

import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;

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

/**
 * //
 * Created by Xiaofei on 2015/9/17.
 */
public class DeviceScanner extends Thread {
    private static final String TAG = DeviceScanner.class.getSimpleName();

    private static final String SERVICE_TYPE = "_ccam._tcp.local.";

    private static final int SCAN_INTERVAL = 3000;

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
            Logger.t(TAG).e("Error", e);
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



            for (JmDNS dns : mdns) {
                dns.addServiceListener(SERVICE_TYPE, mServiceListener);
            }

            threadLoop();

            for (JmDNS dns : mdns) {
                dns.close();
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();

        } finally {
            if (mWifiManager != null) {
                unlockWifi();
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
                Logger.t(TAG).d("serviceAdded: not running");
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
