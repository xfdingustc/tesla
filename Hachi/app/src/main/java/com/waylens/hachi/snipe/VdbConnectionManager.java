package com.waylens.hachi.snipe;

import java.util.HashMap;

/**
 * Created by Xiaofei on 2015/8/20.
 */
public class VdbConnectionManager {
    private static VdbConnectionManager mSharedManager = null;

    private static HashMap<String, VdbConnection> mConnections = new HashMap<>();

    private VdbConnectionManager() {

    }


    private static VdbConnectionManager getManager() {
        if (mSharedManager == null) {
            mSharedManager = new VdbConnectionManager();
        }

        return mSharedManager;
    }

    public VdbConnection getConnection(String url) {
        VdbConnection connection = mConnections.get(url);
        if (connection == null) {
            connection = new VdbConnection(url);
        }
        return connection;
    }
}
