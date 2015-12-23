package com.waylens.hachi.utils;

import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Richard on 12/23/15.
 */
public class TimeMonitor {
    static ConcurrentHashMap<String, Monitor> monitors = new ConcurrentHashMap<>();

    public static void startMonitor(String name) {
        Monitor monitor = monitors.get(name);
        if (monitor == null) {
            monitor = new Monitor(name);
            monitors.put(name, monitor);
        }
        monitor.start();
    }

    public static void showMonitor(String name) {
        monitors.get(name).showMonitor();
    }

    public static void reset(String name) {
        monitors.remove(name);
    }

    static class Monitor {
        long mStart;
        String name;
        int count;
        int total;

        public Monitor(String name) {
            this.name = name;
        }

        public void start() {
            mStart = System.currentTimeMillis();
        }

        public void showMonitor() {
            long delta = System.currentTimeMillis() - mStart;
            total += delta;
            Log.e("TimeMonitor", String.format("%s: %d[%d], total[%d]", name, count++, delta, total));
            start();
        }
    }
}
