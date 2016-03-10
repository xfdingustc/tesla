package com.waylens.hachi.hardware;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Xiaofei on 2016/3/10.
 */
public class CameraDiscovery {
    private static final String TAG = "CameraDiscovery";

    private static final String SERVICE_TYPE = "_ccam._tcp";

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;

    private static CameraDiscovery _INSTANCE = new CameraDiscovery();
    private AtomicBoolean mIsStarted = new AtomicBoolean(false);

    /**
     * Discovery the available cameras
     *
     * @param context
     * @param callback
     */
    public static void discoverCameras(Context context, final Callback callback) {
        _INSTANCE.discoverCamerasImpl(context, callback);
    }

    public static boolean isStarted() {
        return _INSTANCE.mIsStarted.get();
    }

    public static void stopDiscovery() {
        _INSTANCE.stopDiscoveryImpl();
    }

    private void discoverCamerasImpl(Context context, final Callback callback) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mIsStarted.set(false);
                callback.onError(errorCode);
                Log.d(TAG, "onStartDiscoveryFailed: " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "onStopDiscoveryFailed: " + errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                mIsStarted.set(true);
                Log.d(TAG, "onDiscoveryStarted: " + serviceType);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                mIsStarted.set(false);
                Log.d(TAG, "onDiscoveryStopped: " + serviceType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                mIsStarted.set(true);
                Log.e(TAG, "onServiceFound");
                mNsdManager.resolveService(serviceInfo, createResolveListener(callback));
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                mIsStarted.set(false);
                Log.e(TAG, "onServiceLost");
            }
        };
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    private NsdManager.ResolveListener createResolveListener(final Callback callback) {
        return new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "onResolveFailed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                callback.onCameraFound(serviceInfo);
            }
        };
    }


    public void stopDiscoveryImpl() {
        if (mNsdManager != null && mIsStarted.get()) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
    }

    public interface Callback {
        void onCameraFound(NsdServiceInfo cameraService);

        void onError(int errorCode);
    }
}
