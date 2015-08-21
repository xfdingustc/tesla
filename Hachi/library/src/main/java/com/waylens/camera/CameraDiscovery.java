package com.waylens.camera;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Use Android Network Service Discovery API to list available Cameras.
 * <p/>
 * 1) Call static method discoverCameras(Context, Callback) to start the discovery service.
 * 2) You app will be notified with callback when new camera is found. (If there are 2 cameras nearby,
 * the callback will be called twice.
 * 3) Stop the discovery service by calling stopDiscovery()
 * <p/>
 * Created by Richard on 7/30/15.
 */
public class CameraDiscovery {
    private static final String TAG = "CameraDiscovery";

    private static final String SERVICE_TYPE = "_ccam._tcp";

    NsdManager mNsdManager;
    NsdManager.DiscoveryListener mDiscoveryListener;
    AtomicBoolean mIsStarted = new AtomicBoolean(false);

    private static CameraDiscovery _INSTANCE = new CameraDiscovery();

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

        if (mDiscoveryListener != null) {
            stopDiscoveryImpl();
        }

        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mIsStarted.set(false);
                callback.onError(errorCode);
                Logger.t(TAG).d("onStartDiscoveryFailed: " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Logger.t(TAG).d("onStopDiscoveryFailed: " + errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                mIsStarted.set(true);
                Logger.t(TAG).d("onDiscoveryStarted: " + serviceType);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                mIsStarted.set(false);
                Logger.t(TAG).d("onDiscoveryStopped: " + serviceType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                mIsStarted.set(true);
                Logger.t(TAG).d("onServiceFound");
                mNsdManager.resolveService(serviceInfo, createResolveListener(callback));
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                mIsStarted.set(false);
                Logger.t(TAG).e("onServiceLost: " + serviceInfo.getServiceName() + ":" +
                    serviceInfo.getHost());
            }
        };
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    private NsdManager.ResolveListener createResolveListener(final Callback callback) {
        return new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Logger.t(TAG).d("onResolveFailed: " + serviceInfo.getServiceName() + " : Error Code:" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                callback.onCameraFound(serviceInfo);
            }
        };
    }


    public void stopDiscoveryImpl() {
        try {
            mIsStarted.set(false);
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        } catch (IllegalArgumentException e) {
            Logger.t(TAG).d("", e);
        }
    }

    public interface Callback {
        void onCameraFound(NsdServiceInfo cameraService);

        void onError(int errorCode);
    }
}