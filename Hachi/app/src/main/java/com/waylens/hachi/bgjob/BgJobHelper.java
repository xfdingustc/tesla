package com.waylens.hachi.bgjob;

import com.birbit.android.jobqueue.JobManager;
import com.waylens.hachi.bgjob.social.DeleteMomentJob;
import com.waylens.hachi.bgjob.social.FollowJob;
import com.waylens.hachi.bgjob.social.LikeJob;
import com.waylens.hachi.bgjob.social.ReportJob;
import com.waylens.hachi.bgjob.social.RepostJob;
import com.waylens.hachi.bgjob.timelapse.TimeLapseJob;
import com.waylens.hachi.bgjob.upload.PictureUploadJob;
import com.waylens.hachi.bgjob.upload.UploadCachedMomentJob;
import com.waylens.hachi.bgjob.upload.CacheMomentJob;
import com.waylens.hachi.rest.body.ReportMomentBody;
import com.waylens.hachi.ui.entities.LocalMoment;
import com.xfdingustc.snipe.vdb.Clip;

/**
 * Created by Xiaofei on 2016/7/22.
 */
public class BgJobHelper {

    public static void deleteMoment(long momentId) {
        JobManager jobManager = BgJobManager.getManager();
        DeleteMomentJob job = new DeleteMomentJob(momentId);
        jobManager.addJobInBackground(job);
    }

    public static void addLike(long momentId, boolean isCancel) {
        JobManager jobManager = BgJobManager.getManager();
        LikeJob job = new LikeJob(momentId, isCancel);
        jobManager.addJobInBackground(job);
    }


    public static void reportMoment(long momentId, String reportReason) {
        JobManager jobManager = BgJobManager.getManager();
        ReportMomentBody reportMomentBody = new ReportMomentBody();
        reportMomentBody.momentID = momentId;
        reportMomentBody.reason = reportReason;
        reportMomentBody.detail = "";

        ReportJob job = new ReportJob(reportMomentBody, ReportJob.REPORT_TYPE_MOMENT);
        jobManager.addJobInBackground(job);
    }


    public static void followUser(String userId, boolean follow) {
        JobManager jobManager = BgJobManager.getManager();
        FollowJob job = new FollowJob(userId, follow);
        jobManager.addJobInBackground(job);
    }


    public static void repost(long momentId, String provider) {
        JobManager jobManager = BgJobManager.getManager();
        RepostJob job = new RepostJob(momentId, provider);
        jobManager.addJobInBackground(job);
    }

    public static void timeLapse(Clip clip, int speed) {
        JobManager jobManager = BgJobManager.getManager();
        TimeLapseJob timeLapseJob = new TimeLapseJob(clip, speed);
        jobManager.addJobInBackground(timeLapseJob);

    }

    public static void uploadMoment(LocalMoment moment) {
        JobManager jobManager = BgJobManager.getManager();
        CacheMomentJob uploadMomentJob = new CacheMomentJob(moment);
        jobManager.addJobInBackground(uploadMomentJob);
    }

    public static void uploadCachedMoment(LocalMoment moment) {
        JobManager jobManager = BgJobManager.getManager();
        UploadCachedMomentJob job = new UploadCachedMomentJob(moment);
        jobManager.addJobInBackground(job);

    }

    public static void uploadPictureMoment(String title, String pictureUrl) {
        JobManager jobManager = BgJobManager.getManager();
        PictureUploadJob job = new PictureUploadJob(title, pictureUrl);
        jobManager.addJobInBackground(job);

    }


}
