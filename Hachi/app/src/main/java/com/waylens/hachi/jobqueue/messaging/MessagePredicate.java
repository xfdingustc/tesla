package com.waylens.hachi.jobqueue.messaging;

public interface MessagePredicate {
    boolean onMessage(Message message);
}
