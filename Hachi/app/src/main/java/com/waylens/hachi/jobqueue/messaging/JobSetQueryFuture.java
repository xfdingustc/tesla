package com.waylens.hachi.jobqueue.messaging;

import android.support.annotation.NonNull;

import com.waylens.hachi.jobqueue.IntCallback;
import com.waylens.hachi.jobqueue.Job;
import com.waylens.hachi.jobqueue.JobHolder;
import com.waylens.hachi.jobqueue.JobSetCallback;
import com.waylens.hachi.jobqueue.log.JqLog;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by lshw on 16/11/9.
 */

public class JobSetQueryFuture<T extends Message & JobSetCallback.MessageWithCallback>
        implements Future<Set<JobHolder>>, JobSetCallback {
    final MessageQueue messageQueue;
    volatile Set<JobHolder> result = null;
    final CountDownLatch latch = new CountDownLatch(1);
    final T message;

    JobSetQueryFuture(MessageQueue messageQueue, T message) {
        this.messageQueue = messageQueue;
        this.message = message;
        message.setCallback(this);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return latch.getCount() == 0;
    }

    Set<JobHolder> getSafe() {
        try {
            return get();
        } catch (Throwable t) {
            JqLog.e(t, "message is not complete");
        }
        throw new RuntimeException("cannot get the result of the JobManager query");
    }

    @Override
    public Set<JobHolder> get() throws InterruptedException, ExecutionException {
        messageQueue.post(message);
        latch.await();
        return result;
    }

    @Override
    public Set<JobHolder> get(long timeout, @NonNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        messageQueue.post(message);
        latch.await(timeout, unit);
        return result;
    }

    @Override
    public void onResult(Set<JobHolder> result) {
        this.result = result;
        latch.countDown();
    }
}