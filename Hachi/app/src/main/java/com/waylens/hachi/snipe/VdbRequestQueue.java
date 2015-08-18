package com.waylens.hachi.snipe;

import android.os.Handler;
import android.os.Looper;

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
    private final String mHost;

    private VdbConnection mVdbConnection;

    public interface RequestFinishedListener<T> {
        void onRequestFinished(VdbRequest<T> vdbRequest);
    }

    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 3;

    private final Set<VdbRequest<?>> mCurrentVdbRequests = new HashSet<VdbRequest<?>>();

    private final PriorityBlockingQueue<VdbRequest<?>> mVideoDatabaseQueue = new
        PriorityBlockingQueue<VdbRequest<?>>();


    private final VideoDatabase mVideoDatabase;
    private final ResponseDelivery mDelivery;

    private VdbDispatcher[] mVdbDispatchers;

    private List<RequestFinishedListener> mFinishedListeners = new ArrayList<>();

    public VdbRequestQueue(VideoDatabase videoDatabase, String host) {
        this(videoDatabase, DEFAULT_NETWORK_THREAD_POOL_SIZE, host);
    }

    public VdbRequestQueue(VideoDatabase videoDatabase, int threadPoolSize, String host) {
        this(videoDatabase, threadPoolSize,
            new ExecutorDelivery(new Handler(Looper.getMainLooper())), host);
    }

    public VdbRequestQueue(VideoDatabase videoDatabase, int threadPoolSize, ResponseDelivery
        delivery, String host) {
        this.mVideoDatabase = videoDatabase;
        this.mVdbDispatchers = new VdbDispatcher[threadPoolSize];
        this.mDelivery = delivery;
        this.mHost = host;

        mVdbConnection = new VdbConnection(mHost);
    }


    public void start() {
        stop();

        for (int i = 0; i < mVdbDispatchers.length; i++) {
            VdbDispatcher vdbDispatcher = new VdbDispatcher(mVideoDatabaseQueue, mVideoDatabase, mDelivery);
            mVdbDispatchers[i] = vdbDispatcher;
            vdbDispatcher.start();
        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mVdbConnection.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private void stop() {

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
