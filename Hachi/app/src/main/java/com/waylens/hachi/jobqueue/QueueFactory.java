package com.waylens.hachi.jobqueue;


import com.waylens.hachi.jobqueue.config.Configuration;

public interface QueueFactory {
    JobQueue createPersistentQueue(Configuration configuration, long sessionId);
    JobQueue createNonPersistent(Configuration configuration, long sessionId);
}