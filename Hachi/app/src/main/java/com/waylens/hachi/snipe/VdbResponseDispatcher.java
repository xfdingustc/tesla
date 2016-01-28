package com.waylens.hachi.snipe;

import android.util.Log;

import com.orhanobut.logger.Logger;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Richard on 12/27/15.
 */
public class VdbResponseDispatcher extends Thread {
    private static final String TAG = VdbResponseDispatcher.class.getSimpleName();

    private final ConcurrentHashMap<Integer, VdbRequest<?>> mVdbRequestQueue;
    private final ConcurrentHashMap<Integer, VdbMessageHandler<?>> mMessageHandlers;
    private final VdbSocket mVdbSocket;
    private final ResponseDelivery mDelivery;
    private volatile boolean mQuit = false;

    public VdbResponseDispatcher(ConcurrentHashMap<Integer, VdbRequest<?>> vdbRequestQueue,
                                 ConcurrentHashMap<Integer, VdbMessageHandler<?>> messageHandlers,
                                 VdbSocket socket, ResponseDelivery delivery) {
        super("VdbResponseDispatcher");
        mVdbRequestQueue = vdbRequestQueue;
        mMessageHandlers = messageHandlers;
        mVdbSocket = socket;
        mDelivery = delivery;
    }

    public void quit() {
        mQuit = true;
        interrupt();
    }

    @Override
    public void run() {
        while (true) {
            if (mQuit) {
                return;
            }
            VdbAcknowledge vdbAcknowledge = null;
            try {
                vdbAcknowledge = mVdbSocket.retrieveAcknowledge();
            } catch (Exception e) {
                if (mQuit) {
                    return;
                }
                continue;
            }

            VdbRequest<?> vdbRequest;
            if (vdbAcknowledge.isMessageAck()) {
                if ((vdbRequest = mMessageHandlers.get(vdbAcknowledge.getMsgCode())) == null) {
                    Logger.t(TAG).e("MessageCode: " + vdbAcknowledge.getMsgCode());
                    continue;
                }
            } else {
                vdbRequest = mVdbRequestQueue.get(vdbAcknowledge.getUser1());
                if (vdbRequest == null || vdbRequest.getVdbCommand().getCommandCode() != vdbAcknowledge.getMsgCode()) {
                    Logger.t(TAG).e(String.format("Fatal Error:msgCode[%d], " +  "cmdCode[%d], seq[%d]",
                            vdbAcknowledge.getMsgCode(), vdbAcknowledge.getUser1(), vdbAcknowledge.getUser1()));
                    continue;
                }
            }

            vdbRequest.addMarker("vdb-complete");

            if (vdbAcknowledge.notModified && vdbRequest.hasHadResponseDelivered()) {
                vdbRequest.finish("not-modified", true);
                continue;
            }

            VdbResponse<?> vdbResponse = vdbRequest.parseVdbResponse(vdbAcknowledge);
            vdbRequest.addMarker("vdb-fromBinary-complete");

            vdbRequest.markDelivered();
            mDelivery.postResponse(vdbRequest, vdbResponse);
        }
    }
}
