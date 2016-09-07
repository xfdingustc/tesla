package com.waylens.hachi.service.upload.rest.body;

import com.waylens.hachi.ui.entities.LocalMoment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaofei on 2016/9/7.
 */
public class InitUploadBody {
    public String background_music;
    public int have_thumbnail;

    public List<UploadFragment> fragments;

    public static class UploadFragment {
        public String guid;
        public String clip_capture_time;
        public long begin_time;
        public long offset;
        public long duration;
        public double[] frame_rate;
        public int[] resolution;
        public int data_type;
    }


    public static InitUploadBody fromLocalMoment(LocalMoment moment) {
        InitUploadBody uploadBody = new InitUploadBody();
        uploadBody.background_music = "background music";
        uploadBody.have_thumbnail = 1;
        uploadBody.fragments = new ArrayList<>();
        for (LocalMoment.Segment segment : moment.mSegments) {
            UploadFragment fragment = new UploadFragment();
            fragment.guid = segment.clip.getVdbId();
            fragment.clip_capture_time = segment.getClipCaptureTime();
            fragment.begin_time = segment.uploadURL.realTimeMs;
            fragment.offset = 0;
            fragment.duration = segment.uploadURL.lengthMs;
            fragment.frame_rate = new double[2];
            fragment.resolution = new int[2];
            fragment.resolution[0] = 0;
            fragment.resolution[1] = (segment.clip.streams[1].video_width << 16) + segment.clip.streams[1].video_height;
            fragment.data_type = segment.dataType;

            uploadBody.fragments.add(fragment);
        }

        return uploadBody;

    }
}
