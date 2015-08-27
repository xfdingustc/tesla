package com.waylens.hachi.snipe;

import android.content.Context;

/**
 * Created by Xiaofei on 2015/8/17.
 */
public class Snipe {
    public static VdbRequestQueue newRequestQueue(Context context, VdbConnection connection) {
        VdbSocket vdbSocket = new BasicVdbSocket();
        VdbRequestQueue queue = new VdbRequestQueue(vdbSocket, connection);
        queue.start();

        return queue;
    }
}