package com.waylens.hachi.ui.entities;


import com.waylens.hachi.bgjob.upload.UploadServer;
import com.waylens.hachi.rest.body.GeoInfo;
import com.waylens.hachi.rest.response.CreateMomentResponse;
import com.waylens.hachi.snipe.utils.DateTime;
import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.urls.UploadUrl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by Richard on 2/19/16.
 */
public class LocalMoment implements Serializable {

    public int playlistId;

    public String title;

    public String description;

    public String[] tags;

    public String accessLevel;

    public int audioID;

    public Map<String, String> gaugeSettings;

    public ArrayList<Segment> mSegments = new ArrayList<>();

    public String mVehicleMaker = null;

    public String mVehicleModel = null;

    public int mVehicleYear = -1;

    public String mVehicleDesc = null;

    public List<Long> mTimingPoints = null;

    public String momentType = null;


    public UploadServer cloudInfo;

    public long momentID;

    public String thumbnailPath;

    private boolean isReadyToUpload;

    public boolean isFbShare;

    public boolean isYoutubeShare;

    public boolean cache;

    public String vin;

    public boolean withGeoTag;

    public boolean withCarInfo;

    public GeoInfo geoInfo;

    public int streamId;

    public LocalMoment(String title, String momentType) {
        this.title = title;
        this.momentType = momentType;
    }

    public LocalMoment(int playlistId, String title, String description, String[] tags,
                       String accessLevel, int audioID, Map<String, String> gaugeSettings,
                       boolean isFbShare, boolean isYoutubeShare, boolean cache, int streamId) {
        this.playlistId = playlistId;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.accessLevel = accessLevel;
        this.audioID = audioID;
        this.gaugeSettings = gaugeSettings;
        this.isFbShare = isFbShare;
        this.isYoutubeShare = isYoutubeShare;
        this.cache = cache;
        this.streamId = streamId;
    }

    public void setFragments(ArrayList<Segment> segments, String thumbnailPath) {
        this.mSegments = segments;
        this.thumbnailPath = thumbnailPath;
    }

    public void updateUploadInfo(CreateMomentResponse response) {
        this.momentID = response.momentID;
        UploadServer uploadServer = response.uploadServer;
        this.cloudInfo = new UploadServer(uploadServer.ip, uploadServer.port, uploadServer.privateKey);
    }

    public void updateUploadInfo(long momentID, String address, int port, String privateKey) {
        this.momentID = momentID;
        cloudInfo = new UploadServer(address, port, privateKey);
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
            //timezone offset has been minus twice, so compensate here.
            return DateTime.toString(clip.getClipDate() + TimeZone.getDefault().getRawOffset(), offset);
        }
    }
}
