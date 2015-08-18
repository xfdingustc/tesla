package com.waylens.hachi.VdbImageLoader.core;

import com.waylens.hachi.VdbImageLoader.QueueProcessingType;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public class DefaultConfigFactory {
    public static Executor createExecutor(int threadPoolSize, int threadPriority,
                                          QueueProcessingType processingType) {
        boolean lifo = processingType == QueueProcessingType.QUEUE_PROCESSING_TYPE_LIFO;
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        return new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, TimeUnit.MILLISECONDS,
            taskQueue, createThreadFactory(threadPriority, "vdbil-pool-"));
    }

    private static ThreadFactory createThreadFactory(int threadPriority, String
        threadNamePrefix) {
        return new DefaultThreadFactory(threadPriority, threadNamePrefix);
    }


    private static class DefaultThreadFactory implements ThreadFactory {

        private static final AtomicInteger mPoolNumber = new AtomicInteger(1);

        private final int mThreadPritory;
        private final AtomicInteger mThreadNumber = new AtomicInteger(1);
        private final ThreadGroup mThreadGroup;
        private final String mNamePrefix;

        public DefaultThreadFactory(int threadPritory, String threadNamePrefix) {
            this.mThreadPritory = threadPritory;
            this.mThreadGroup = Thread.currentThread().getThreadGroup();
            this.mNamePrefix = threadNamePrefix + mPoolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(mThreadGroup, r, mNamePrefix + mThreadNumber
                .getAndIncrement(), 0);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            thread.setPriority(mThreadPritory);
            return thread;
        }
    }


}
