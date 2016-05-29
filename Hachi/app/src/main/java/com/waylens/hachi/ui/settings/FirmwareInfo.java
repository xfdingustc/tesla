package com.waylens.hachi.ui.settings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Xiaofei on 2016/5/13.
 */
public class FirmwareInfo {

    private String mName;
    private String mUrl;
    private String mVersion;
    private int mSize;
    private String mMd5;
    private String mDescription;

    public static FirmwareInfo fromJson(JSONObject object) {
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        try {
            firmwareInfo.mName = object.getString("name");
            firmwareInfo.mVersion = object.getString("version");
            firmwareInfo.mUrl = object.getString("url");
            firmwareInfo.mSize = object.getInt("size");
            firmwareInfo.mMd5 = object.getString("md5");
            JSONObject description = object.getJSONObject("description");
            firmwareInfo.mDescription = description.getString("en");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  firmwareInfo;
    }

    public String getName() {
        return mName;
    }

    public String getVersion() {
        return mVersion;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getMd5() {
        return mMd5;
    }

    public String getDescription() {
        return mDescription;
    }
}
