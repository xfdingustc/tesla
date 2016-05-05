package com.waylens.hachi.bgjob.upload;

import com.waylens.hachi.utils.ToStringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Xiaofei on 2016/4/27.
 */
public class CloudInfo implements Serializable {
    private String mAddress;
    private int mPort;
    private String mPrivateKey;

    public static CloudInfo parseFromJson(JSONObject object) {
        JSONObject uploadServer = null;
        try {
            uploadServer = object.getJSONObject("uploadServer");
            String ipAddr = uploadServer.optString("ip");
            int port = uploadServer.optInt("port");
            String privKey = uploadServer.optString("privateKey");
            return new CloudInfo(ipAddr, port, privKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public CloudInfo(String address, int port, String privateKey) {
        this.mAddress = address;
        this.mPort = port;
        this.mPrivateKey = privateKey;
    }

    public String getAddress() {
        return mAddress;
    }

    public int getPort() {
        return mPort;
    }

    public String getPrivateKey() {
        return mPrivateKey;
    }

    @Override
    public String toString() {
        return ToStringUtils.getString(this);
    }
}
