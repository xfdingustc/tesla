package com.waylens.hachi.jobqueue.messaging;

public abstract class MessageQueueConsumer {
    abstract public void handleMessage(Message message);
    abstract public void onIdle();
    public void onStart() {

    }
}
