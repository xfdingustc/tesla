package com.waylens.hachi.snipe;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.toolbox.SetOptionsRequest;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public class Snipe {
    private static VdbConnection mVdbConnection;

    private static volatile VdbRequestQueue _REQUEST_QUEUE_SINGLETON;

    private static EventBus mEventBus = EventBus.getDefault();

    public static VdbRequestQueue newRequestQueue() {
        if (_REQUEST_QUEUE_SINGLETON == null) {
            synchronized (VdbRequestQueue.class) {
                if (_REQUEST_QUEUE_SINGLETON == null) {
                    VdbSocket vdbSocket = new BasicVdbSocket();
                    _REQUEST_QUEUE_SINGLETON = new VdbRequestQueue(vdbSocket);
                    _REQUEST_QUEUE_SINGLETON.start();
                }
            }
        }
        return _REQUEST_QUEUE_SINGLETON;
    }

    public static VdbRequestQueue newRequestQueue(Context context) {
        if (_REQUEST_QUEUE_SINGLETON == null) {
            synchronized (VdbRequestQueue.class) {
                if (_REQUEST_QUEUE_SINGLETON == null) {
                    VdbSocket vdbSocket = new BasicVdbSocket();
                    _REQUEST_QUEUE_SINGLETON = new VdbRequestQueue(vdbSocket);
                    _REQUEST_QUEUE_SINGLETON.start();
                }
            }
        }
        return _REQUEST_QUEUE_SINGLETON;
    }

    // TODO: We will add multi camera support later, we need implement different request queue for
    //       different vdt camera;
    public static VdbRequestQueue newRequestQueue(Context context, VdtCamera camera) {
        mVdbConnection = camera.getVdbConnection();
        return newRequestQueue();
    }

    public static void init() {
        VdtCameraManager manager = VdtCameraManager.getManager();
//        mEventBus.register(this);
//        manager.addCallback(mCallback);
    }

//    @Subscribe
//
//    private static VdtCameraManager.Callback mCallback = new VdtCameraManager.Callback() {
//        @Override
//        public void onCameraConnecting(VdtCamera vdtCamera) {
//
//        }
//
//        @Override
//        public void onCameraConnected(VdtCamera vdtCamera) {
//
//        }
//
//        @Override
//        public void onCameraVdbConnected(VdtCamera vdtCamera) {
//            mVdbConnection = vdtCamera.getVdbConnection();
//            //setOptions();
//        }
//
//        @Override
//        public void onCameraDisconnected(VdtCamera vdtCamera) {
//
//        }
//
//        @Override
//        public void onCameraStateChanged(VdtCamera vdtCamera) {
//
//        }
//
//        @Override
//        public void onWifiListChanged() {
//
//        }
//    };

    public static VdbConnection getVdbConnect() {
        return mVdbConnection;
    }

    /**
     * Still cannot play HLS smoothly on Samsung Galaxy Note 3, even set the
     * HLS segment duration as 2 seconds. Think about use ExoPlayer to resolve
     * this issue. [Richard]
     */
//    static void setOptions() {
//        if (!"samsung".equalsIgnoreCase(Build.MANUFACTURER)) {
//            return;
//        }
//        VdbRequestQueue queue = Snipe.newRequestQueue();
//        queue.add(new SetOptionsRequest(new VdbResponse.Listener<Integer>() {
//            @Override
//            public void onResponse(Integer response) {
//                Log.e("test", "Response: " + response);
//            }
//        }, new VdbResponse.ErrorListener() {
//            @Override
//            public void onErrorResponse(SnipeError error) {
//                Log.e("test", "setOptions Error", error);
//            }
//        }, SetOptionsRequest.VDB_OPTION_HLS_SEGMENT_LENGTH, 2000));
//    }

}
