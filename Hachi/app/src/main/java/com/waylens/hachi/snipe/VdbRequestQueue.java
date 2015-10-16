package com.waylens.hachi.snipe;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Request;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VdbRequestQueue
 * Created by Xiaofei on 2015/8/17.
 */
public class VdbRequestQueue {
    private static final String TAG = VdbRequestQueue.class.getSimpleName();

    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 1;

    private static final int MAX_PENDING_REQUEST_COUNT = 4;

    private VdbConnection mVdbConnection;

    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    private final Set<VdbRequest<?>> mCurrentVdbRequests = new HashSet<VdbRequest<?>>();

    private final PriorityBlockingQueue<VdbRequest<?>> mVideoDatabaseQueue = new
            PriorityBlockingQueue<VdbRequest<?>>();

    private final CircularQueue<VdbRequest<?>> mIgnorableRequestQueue = new CircularQueue<>(1);

    private AtomicInteger mPendingRequestCount = new AtomicInteger();

    private final VdbSocket mVdbSocket;
    private final ResponseDelivery mDelivery;

    private VdbDispatcher[] mVdbDispatchers;

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
        /*
        for (int i = 0; i < mVdbDispatchers.length; i++) {
            if (mVdbDispatchers[i] != null) {
                mVdbDispatchers[i].quit();
            }
        }
        if (mVdbReceiver != null) {
            mVdbReceiver.quit();
        } */
    }

    public VdbConnection getConnection() {
        return mVdbConnection;
    }

    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }

    public <T> VdbRequest<T> add(VdbRequest<T> vdbRequest) {
        return add(vdbRequest, false);
    }

    private <T> VdbRequest<T> add(VdbRequest<T> vdbRequest, boolean isPendingRequest) {
        if (vdbRequest.isIgnorable()) {
            if (mPendingRequestCount.get() >= MAX_PENDING_REQUEST_COUNT) {
                if (!isPendingRequest) {
                    mIgnorableRequestQueue.offer(vdbRequest);
                }
                return vdbRequest;
            } else {
                int count = mPendingRequestCount.incrementAndGet();
                Log.e("test", "Pending Count1: " + count);
            }
        }

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

        if (vdbRequest.isIgnorable()) {
            int count = mPendingRequestCount.decrementAndGet();
            Log.e("test", "Pending Count2: " + count);
            VdbRequest request = mIgnorableRequestQueue.poll();
            if (request != null) {
                add(request, true);
            }
        }
    }

    public void cancelAll(final Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancelAll with a null tag");
        }
        cancelAll(new RequestFilter() {
            @Override
            public boolean apply(VdbRequest<?> request) {
                return request.getTag().equals(tag);
            }
        });
    }

    public void cancelAll(RequestFilter filter) {
        synchronized (mCurrentVdbRequests) {
            for (VdbRequest<?> request : mCurrentVdbRequests) {
                if (filter.apply(request)) {
                    request.cancel();
                }
            }
        }
    }

    public interface RequestFilter {
        public boolean apply(VdbRequest<?> request);
    }
}
