package com.waylens.hachi.bgjob;

import com.birbit.android.jobqueue.JobManager;
import com.waylens.hachi.bgjob.social.DeleteMomentJob;
import com.waylens.hachi.bgjob.social.FollowJob;
import com.waylens.hachi.bgjob.social.ReportJob;
import com.waylens.hachi.rest.body.ReportMomentBody;

/**
 * Created by Xiaofei on 2016/7/22.
 */
public class BgJobHelper {

    public static void deleteMoment(long momentId) {
        JobManager jobManager = BgJobManager.getManager();
        DeleteMomentJob job = new DeleteMomentJob(momentId);
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


}
