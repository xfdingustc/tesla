package com.waylens.hachi.views.dashboard.eventbus;



import java.util.HashMap;

/**
 * Created by Xiaofei on 2015/9/10.
 */
public class EventBus {

    public interface EventSubscriber {
        String getSubscribe();

        void onEvent(Object value);
    }

    private HashMap<String, EventSubscriber>  mSubscriber = new HashMap<>();
    public EventBus() {

    }

    public void register(EventSubscriber subscriber) {
        if (subscriber.getSubscribe() == null) {
            return;
        }

        mSubscriber.put(subscriber.getSubscribe(), subscriber);

    }

    public void postEvent(String event, Object value) {
        EventSubscriber subscriber = mSubscriber.get(event);
        if (subscriber != null) {
            subscriber.onEvent(value);
        }
    }
}
