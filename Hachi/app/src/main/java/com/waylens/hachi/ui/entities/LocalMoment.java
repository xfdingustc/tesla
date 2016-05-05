package com.waylens.hachi.ui.entities;


import com.waylens.hachi.bgjob.upload.CloudInfo;
import com.waylens.hachi.utils.DateTime;
import com.waylens.hachi.vdb.Clip;
import com.waylens.hachi.vdb.urls.UploadUrl;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Richard on 2/19/16.
 */
public class LocalMoment implements Serializable {

    public String title;

    public String[] tags;

    public String accessLevel;

    public int audioID;

    public String gaugeSettings;

    public ArrayList<Segment> mSegments;


    public CloudInfo cloudInfo;

    public long momentID;

    public String thumbnailPath;

    private boolean isReadyToUpload;

    public LocalMoment(String title, String[] tags, String accessLevel, int audioID, JSONObject gaugeSettings) {
        this.title = title;
        this.tags = tags;
        this.accessLevel = accessLevel;
        this.audioID = audioID;
        this.gaugeSettings = gaugeSettings == null ? null : gaugeSettings.toString();
    }

    public void setFragments(ArrayList<Segment> segments, String thumbnailPath) {
        this.mSegments = segments;
        this.thumbnailPath = thumbnailPath;
    }

    public void updateUploadInfo(long momentID, String address, int port, String privateKey) {
        this.momentID = momentID;
        cloudInfo = new CloudInfo(address, port, privateKey);
    }

    public boolean isPrepared() {
        return isReadyToUpload;
    }

    public void setPrepared(boolean isPrepared) {
        isReadyToUpload = isPrepared;
    }


    public static class Segment implements Serializable {

        public Clip clip;
        public UploadUrl uploadURL;
        public int dataType;


        public Segment(Clip clip, UploadUrl uploadURL, int dataType) {
            this.clip = clip;
            this.uploadURL = uploadURL;
            this.dataType = dataType;
        }

        public String getClipCaptureTime() {
            long offset = uploadURL.realTimeMs - clip.getStartTimeMs();
            return DateTime.toString(clip.getDate(), offset);
        }
    }
}
