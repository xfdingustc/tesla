package com.waylens.hachi.VdbImageLoader.core;

import com.waylens.hachi.VdbImageLoader.LoadAndDisplayVdbImageTask;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Created by Xiaofei on 2015/8/14.
 */
public class VdbImageLoaderEngine {

    private final VdbImageLoaderConfiguration mConfiguration;

    private Executor mTaskDistributor;
    private Executor mTaskExecutor;

    public VdbImageLoaderEngine(VdbImageLoaderConfiguration configuration) {
        this.mConfiguration = configuration;
    }



    public void submit(final LoadAndDisplayVdbImageTask task) {
        mTaskDistributor.execute(new Runnable() {
            @Override
            public void run() {
                initExecutorsIfNeed();
                mTaskExecutor.execute(task);
            }
        });
    }

    private void initExecutorsIfNeed() {
        if (((ExecutorService) mTaskExecutor).isShutdown()) {
            mTaskExecutor = createTaskExecutor();
        }

    }

    private Executor createTaskExecutor() {
        return DefaultConfigFactory.createExecutor(mConfiguration.mThreadPoolSize, mConfiguration
            .mThreadPriority, mConfiguration.mTasksProcessingType);
    }
}
