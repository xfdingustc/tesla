package com.waylens.hachi.bgjob.export.statejobqueue;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.bgjob.export.statejobqueue.UploadMomentJob.JobCallback;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by lshw on 16/11/22.
 */

public class CacheUploadMomentService extends JobService implements JobCallback {
    private static final String TAG = CacheUploadMomentService.class.getSimpleName();
    private static final int JOB_ID = 0x1000;

    private boolean running;

    private PersistentQueue queue;

    private ThreadPoolExecutor executor;


    public static void scheduleJob(Context context) {
        ComponentName jobService = new ComponentName(context.getApplicationContext().getPackageName(),
            CacheUploadMomentService.class.getCanonicalName());
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, jobService).setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
            .setPeriodic(10000).build();
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int scheduledId = jobScheduler.schedule(jobInfo);
        Logger.t(TAG).d(scheduledId > 0 ? "schedule successfully" : "schedule failed");

    }

    public static void launch(Activity activity) {
        Intent startServiceIntent = new Intent(activity, CacheUploadMomentService.class);
        activity.startService(startServiceIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //((Hachi) getApplication())
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        queue = PersistentQueue.getPersistentQueue();
        Logger.t(TAG).d("Service starting!");
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        executeNext();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Job job = (CacheUploadMomentJob) intent.getSerializableExtra("job");
            Logger.t(TAG).d("start do command");
            if (job != null) {
                job.setSealed(true);
                StateJobHolder stateJobHolder = new StateJobHolder(job.getId(), StateJobHolder.INITIAL_STATE, null, job);
                queue.insert(stateJobHolder);
            }
        }
        executeNext();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }


    private void executeNext() {
        if (running) {
            return;
        }
        StateJobHolder stateJobHolder = queue.peekJob();
        if (stateJobHolder != null) {
            running = true;
            try {
                Job job = stateJobHolder.getJob();
                if (job instanceof CacheUploadMomentJob) {
                    ((CacheUploadMomentJob) job).setJobCallback(this);
                } else if (job instanceof UploadPictureJob) {
                    ((UploadPictureJob) job).setJobCallback(this);
                } else if (job instanceof UploadTimelapseJob) {
                    ((UploadTimelapseJob) job).setJobCallback(this);
                }
                executor.execute(new Worker(job, stateJobHolder.getJobState()));
            } catch (Throwable throwable) {
                Logger.t(TAG).d("exception occur!");
                throwable.printStackTrace();
            }
        } else {
            Logger.t(TAG).d("No more task");
            stopSelf();
        }
    }

    @Override
    public void onSuccess(UploadMomentJob job) {
        Logger.t(TAG).d("onSuccess");
        running = false;
        //queue.delete();
        String jobId = job.getId();
        Logger.t(TAG).d("Job id = " + jobId);
        queue.delete(jobId);
        queue.setCurrentJob(null);
        executeNext();
    }

    @Override
    public void updateJob(UploadMomentJob job) {
        Logger.t(TAG).d("updateJob");
        String jobId = job.getId();
        StateJobHolder jobHolder = queue.findJobById(jobId);
        jobHolder.setJob(job);
        jobHolder.setJobState(StateJobHolder.MIDDLE_STATE);
        queue.updateJob(jobHolder);
    }

    @Override
    public void onFailure(UploadMomentJob job) {
        Logger.t(TAG).d("onFailure");
        running = false;
        String jobId = job.getId();
        StateJobHolder jobHolder = queue.findJobById(jobId);
        jobHolder.setJobState(-1);
        queue.updateJob(jobHolder);
        executeNext();
    }

    @Override
    public void updateProgress(UploadMomentJob job) {
        Logger.t(TAG).d("update progress");
        String jobId = job.getId();
        StateJobHolder jobHolder = queue.findJobById(jobId);
        jobHolder.setJob(job);
        queue.updateJobProgress(jobHolder);
    }

    public class Worker implements Runnable {
        int state;
        Job job;

        public Worker(Job job, int state) {
            this.job = job;
            this.state = state;
        }

        @Override
        public void run() {
            try {
                Logger.t(TAG).d("run job in worker");
                job.onRun(state);
            } catch (Throwable e) {
                e.printStackTrace();
                executeNext();
            }
        }
    }
}
