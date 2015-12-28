package com.waylens.hachi.snipe;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Richard on 12/27/15.
 */
public class VdbResponseDispatcher extends Thread {

    private final ConcurrentHashMap<Integer, VdbRequest<?>> mVdbRequestQueue;
    private final ConcurrentHashMap<Integer, WeakReference<VdbMessageHandler<?>>> mMessageHandlers;
    private final VdbSocket mVdbSocket;
    private final ResponseDelivery mDelivery;
    private volatile boolean mQuit = false;

    public VdbResponseDispatcher(ConcurrentHashMap<Integer, VdbRequest<?>> vdbRequestQueue,
                                 ConcurrentHashMap<Integer, WeakReference<VdbMessageHandler<?>>> messageHandlers,
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
                WeakReference<VdbMessageHandler<?>> reference = mMessageHandlers.get(vdbAcknowledge.getMsgCode());
                if (reference == null || (vdbRequest = reference.get()) == null) {
                    Log.e("richard", "MessageCode: " + vdbAcknowledge.getMsgCode());
                    continue;
                }
            } else {
                vdbRequest = mVdbRequestQueue.get(vdbAcknowledge.getUser1());
                if (vdbRequest == null || vdbRequest.getVdbCommand().getCommandCode() != vdbAcknowledge.getMsgCode()) {
                    Log.e("richard", String.format("Fatal Error:msgCode[%d], cmdCode[%d], seq[%d]",
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
            vdbRequest.addMarker("vdb-parse-complete");

            vdbRequest.markDelivered();
            mDelivery.postResponse(vdbRequest, vdbResponse);
        }
    }
}
