package com.waylens.hachi.snipe;

import android.os.Handler;
import android.os.Looper;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public class VdbRequestQueue {
    private static final String TAG = VdbRequestQueue.class.getSimpleName();

    private VdbConnection mVdbConnection;

    public interface RequestFinishedListener<T> {
        void onRequestFinished(VdbRequest<T> vdbRequest);
    }

    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 1;

    private final Set<VdbRequest<?>> mCurrentVdbRequests = new HashSet<VdbRequest<?>>();

    private final PriorityBlockingQueue<VdbRequest<?>> mVideoDatabaseQueue = new
        PriorityBlockingQueue<VdbRequest<?>>();


    private final VdbSocket mVdbSocket;
    private final ResponseDelivery mDelivery;

    private VdbDispatcher[] mVdbDispatchers;

    private List<RequestFinishedListener> mFinishedListeners = new ArrayList<>();

    public VdbRequestQueue(VdbSocket vdbSocket, VdbConnection connection) {
        this(vdbSocket, DEFAULT_NETWORK_THREAD_POOL_SIZE, connection);
    }

    public VdbRequestQueue(VdbSocket vdbSocket, int threadPoolSize, VdbConnection connection) {
        this(vdbSocket, threadPoolSize,
            new ExecutorDelivery(new Handler(Looper.getMainLooper())), connection);
    }

    public VdbRequestQueue(VdbSocket vdbSocket, int threadPoolSize, ResponseDelivery
        delivery, VdbConnection connection) {
        this.mVdbSocket = vdbSocket;
        this.mVdbDispatchers = new VdbDispatcher[threadPoolSize];
        this.mDelivery = delivery;
        this.mVdbConnection = connection;
    }


    public void start() {
        stop();

        for (int i = 0; i < mVdbDispatchers.length; i++) {
            VdbDispatcher vdbDispatcher = new VdbDispatcher(mVideoDatabaseQueue, mVdbSocket, mDelivery);
            mVdbDispatchers[i] = vdbDispatcher;
            vdbDispatcher.start();
        }
    }

    private void stop() {

    }

    public VdbConnection getConnection() {
        return mVdbConnection;
    }

    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }

    public <T> VdbRequest<T> add(VdbRequest<T> vdbRequest) {
        vdbRequest.setRequestQueue(this);
        synchronized (mCurrentVdbRequests) {
            mCurrentVdbRequests.add(vdbRequest);
        }

        vdbRequest.setSequence(getSequenceNumber());
        vdbRequest.addMarker("add-to-queue");


        mVideoDatabaseQueue.add(vdbRequest);

        return vdbRequest;
    }

    <T> void finish(VdbRequest<T> vdbRequest) {
        synchronized (mCurrentVdbRequests) {
            mCurrentVdbRequests.remove(vdbRequest);
        }
        synchronized (mFinishedListeners) {
            for (RequestFinishedListener<T> listener : mFinishedListeners) {
                listener.onRequestFinished(vdbRequest);
            }
        }
    }
}
