package com.waylens.hachi.snipe;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.waylens.hachi.hardware.vdtcamera.VdtCamera;
import com.waylens.hachi.hardware.vdtcamera.VdtCameraManager;
import com.waylens.hachi.snipe.toolbox.SetOptionsRequest;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public class Snipe {


    private static Map<VdtCamera, VdbRequestQueue> mVdbRequestQueueMap = new HashMap<>();

    private static EventBus mEventBus = EventBus.getDefault();


    // TODO: We will add multi camera support later, we need implement different request queue for
    //       different vdt camera;
//    public static VdbRequestQueue newRequestQueue(Context context, VdtCamera camera) {
//        VdbRequestQueue requestQueue = mVdbRequestQueueMap.get(camera);
//        if (requestQueue != null) {
//            return requestQueue;
//        }
//
//        VdbSocket vdbSocket = new BasicVdbSocket(camera.getVdbConnection());
//        requestQueue = new VdbRequestQueue(vdbSocket);
//        requestQueue.start();
//        mVdbRequestQueueMap.put(camera, requestQueue);
//
//        return requestQueue;
////        mVdbConnection = camera.getVdbConnection();
////        return newRequestQueue();
//    }

    public static void init() {
        VdtCameraManager manager = VdtCameraManager.getManager();
//        mEventBus.register(this);
//        manager.addCallback(mCallback);
    }





}
