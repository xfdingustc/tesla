package com.waylens.hachi.utils;

import com.waylens.hachi.views.dashboard.ElementView;

import java.util.HashMap;

/**
 * Created by Xiaofei on 2015/9/10.
 */
public class EventBus {
    private HashMap<String, ElementView>  mSubscriber = new HashMap<>();
    public EventBus() {

    }

    public void register(ElementView elementView) {
        if (elementView.getSubscribe() == null) {
            return;
        }

        mSubscriber.put(elementView.getSubscribe(), elementView);

    }

    public void postEvent(String event, Object value) {
        ElementView element = mSubscriber.get(event);
        if (element != null) {
            element.onEvent(value);
        }
    }
}
