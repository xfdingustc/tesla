package com.waylens.hachi.VdbImageLoader.core;

import android.content.Context;

import com.waylens.hachi.VdbImageLoader.QueueProcessingType;

/**
 * Created by Xiaofei on 2015/8/14.
 */
public class VdbImageLoaderConfiguration {
    public final int mThreadPoolSize;
    public final int mThreadPriority;
    public final QueueProcessingType mTasksProcessingType;


    private VdbImageLoaderConfiguration(final Builder builder) {
        this.mThreadPoolSize = builder.mThreadPoolSize;
        this.mThreadPriority = builder.mThreadPriority;
        this.mTasksProcessingType = builder.mTasksProcessingType;
    }


    public static class Builder {
        public static final int DEFAULT_THREAD_POOL_SIZE = 3;
        public static final int DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY - 2;
        public static final QueueProcessingType DEFAULT_TASK_PROCESSING_TYPE =
            QueueProcessingType.QUEUE_PROCESSING_TYPE_FIFO;

        private Context mContext;

        private QueueProcessingType mTasksProcessingType = DEFAULT_TASK_PROCESSING_TYPE;

        private int mThreadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        private int mThreadPriority = DEFAULT_THREAD_PRIORITY;

        public Builder(Context context) {
            this.mContext = context.getApplicationContext();
        }

        public VdbImageLoaderConfiguration build() {
            return new VdbImageLoaderConfiguration(this);
        }
    }
}
