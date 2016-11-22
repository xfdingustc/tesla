package com.waylens.hachi.bgjob.export.statejobqueue;

/**
 * Created by lshw on 16/11/25.
 */



import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.waylens.hachi.jobqueue.Params;
import com.waylens.hachi.jobqueue.log.JqLog;
import com.waylens.hachi.jobqueue.network.NetworkUtil;
import com.waylens.hachi.jobqueue.timer.Timer;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Base class for all of your jobs.
 */
@SuppressWarnings("deprecation")
abstract public class Job implements Serializable {
    private static final long serialVersionUID = 3L;
    @SuppressWarnings("WeakerAccess")
    public static final int DEFAULT_RETRY_LIMIT = 20;
    static final String SINGLE_ID_TAG_PREFIX = "job-single-id:";
    // set either in constructor or by the JobHolder
    private transient String id;
    // values set from params
    @NetworkUtil.NetworkStatus
    transient int requiredNetworkType;
    // values set after job is covered by a JobHolder
    private transient String groupId;
    private transient boolean persistent;
    private transient Set<String> readonlyTags;

    private transient int currentRunCount;
    /**package**/ transient int priority;
    private transient long delayInMs;
    private transient long deadlineInMs;
    private transient boolean cancelOnDeadline;
    /*package*/ transient volatile boolean cancelled;

    // set when job is loaded
    private transient Context applicationContext;

    private transient volatile boolean sealed;

    // set when job is loaded
    private transient volatile boolean isDeadlineReached;


    protected Job(Params params) {
        this.id = UUID.randomUUID().toString();
        this.persistent = params.isPersistent();
        this.groupId = params.getGroupId();
        this.priority = params.getPriority();
        this.delayInMs = Math.max(0, params.getDelayMs());
        this.deadlineInMs = Math.max(0, params.getDeadlineMs());
        this.cancelOnDeadline = params.shouldCancelOnDeadline();
        final String singleId = params.getSingleId();
        if (params.getTags() != null || singleId != null) {
            final Set<String> tags = params.getTags() != null ? params.getTags() : new HashSet<String>();
            if (singleId != null) {
                final String tagForSingleId = createTagForSingleId(singleId);
                tags.add(tagForSingleId);
                if (this.groupId == null) {
                    this.groupId = tagForSingleId;
                }
            }
            this.readonlyTags = Collections.unmodifiableSet(tags);
        }
        if (deadlineInMs > 0 && deadlineInMs < delayInMs) {
            throw new IllegalArgumentException("deadline cannot be less than the delay. It" +
                    " does not make sense. deadline:" + deadlineInMs + "," +
                    "delay:" + delayInMs);
        }

    }

    abstract public String getId();

    /**
     * used by {@link JobManager} to assign proper priority at the time job is added.
     * @return priority (higher = better)
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * used by {@link JobManager} to assign proper delay at the time job is added.
     * This field is not persisted!
     * @return delay in ms
     */
    public final long getDelayInMs() {
        return delayInMs;
    }

    /**
     * Returns a readonly set of tags attached to this Job.
     *
     * @return Set of Tags. If tags do not exists, returns null.
     */
    @Nullable
    public final Set<String> getTags() {
        return readonlyTags;
    }

    public void setSealed(boolean state) {
        sealed = state;
    }

    /**
     * Whether we should add this job to disk or non-persistent queue
     *
     * @return True if this job should be persistent between app restarts
     */
    public final boolean isPersistent() {
        return persistent;
    }

    /**
     * Called when the job is added to disk and committed.
     * This means job will eventually run. This is a good time to update local database and dispatch events.
     * <p>
     * Changes to this class will not be preserved if your job is persistent !!!
     * <p>
     * Also, if your app crashes right after adding the job, {@code onRun} might be called without an {@code onAdded} call
     * <p>
     * Note that this method is called on JobManager's thread and will block any other action so
     * it should be fast and not make any web requests (File IO is OK).
     */
    public void onAdded() {

    };

    /**
     * The actual method that should to the work.
     * It should finish w/o any exception. If it throws any exception,
     * {@link #shouldReRunOnThrowable(Throwable, int, int)} will be called to
     * decide either to dismiss the job or re-run it.
     *
     * @throws Throwable Can throw and exception which will mark job run as failed
     */
    public void onRun(int state) throws Throwable {

    }


    public final int getCurrentRunCount() {
        return currentRunCount;
    }

    /**
     * Some jobs may require being run synchronously. For instance, if it is a job like sending a comment, we should
     * never run them in parallel (unless they are being sent to different conversations).
     * By assigning same groupId to jobs, you can ensure that that type of jobs will be run in the order they were given
     * (if their priority is the same).
     *
     * @return The groupId of the job or null if it is not grouped
     */
    public final String getRunGroupId() {
        return groupId;
    }

    /**
     * Some jobs only need a single instance to be queued to run. For instance, if a user has made several changes
     * to a resource while offline, you can save every change locally during {@link #onAdded()}, but
     * only update the resource remotely once with the latest changes.
     *
     * @return The single instance id of the job or null if it is not a single instance job
     */
    public final String getSingleInstanceId() {
        if (readonlyTags != null) {
            for (String tag : readonlyTags) {
                if (tag.startsWith(SINGLE_ID_TAG_PREFIX)) {
                    return tag;
                }
            }
        }
        return null;
    }

    private String createTagForSingleId(String singleId) {
        return SINGLE_ID_TAG_PREFIX + singleId;
    }

    /**
     * By default, jobs will be retried {@code DEFAULT_RETRY_LIMIT}  times.
     * If job fails this many times, onCancel will be called w/o calling {@link #shouldReRunOnThrowable(Throwable, int, int)}
     *
     * @return The number of times the job should be re-tried before being cancelled automatically
     */
    protected int getRetryLimit() {
        return DEFAULT_RETRY_LIMIT;
    }

    /**
     * Returns true if job is cancelled. Note that if the job is already running when it is cancelled,
     * this flag is still set to true but job is NOT STOPPED (e.g. JobManager does not interrupt
     * the thread).
     * If you have a long job that may be cancelled, you can check this field and handle it manually.
     * <p>
     * Note that, if your job returns successfully from {@link #onRun()} method, it will be considered
     * as successfully completed, thus will be added to {@link CancelResult#getFailedToCancel()}
     * list. If you want this job to be considered as cancelled, you should throw an exception.
     * You can also use {@link #assertNotCancelled()} method to do it.
     * <p>
     * Calling this method outside {@link #onRun()} method has no meaning since {@link #onRun()} will not
     * be called if the job is cancelled before it is called.
     *
     * @return true if the Job is cancelled
     */
    public final boolean isCancelled() {
        return cancelled;
    }

    /**
     * Convenience method that checks if job is cancelled and throws a RuntimeException if it is
     * cancelled.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public void assertNotCancelled() {
        if (cancelled) {
            throw new RuntimeException("job is cancelled");
        }
    }

    /*package*/ void setApplicationContext(Context context) {
        this.applicationContext = context;
    }

    /*package*/ void setDeadlineReached(boolean deadlineReached) {
        isDeadlineReached = deadlineReached;
    }

    /**
     * Convenience method to get the application context in a Job.
     * <p>
     * This context is set when job is added to a JobManager.
     *
     * @return The application context
     */
    @SuppressWarnings("WeakerAccess")
    public Context getApplicationContext() {
        return applicationContext;
    }

    /**
     * Returns true if the job's deadline is reached.
     * <p>
     * Note that this method is safe to access only when the job is running. Value is undefined
     * if it is called outside the {@link #onRun()} method.
     *
     * @return true if job reached its deadline, false otherwise
     */
    public boolean isDeadlineReached() {
        return isDeadlineReached;
    }

    /**
     * Returns whether job requires a network connection to be run or not.
     *
     * @return True if job requires a network to be run, false otherwise.
     */
    @SuppressWarnings("unused")
    public final boolean requiresNetwork() {
        return requiredNetworkType >= NetworkUtil.METERED;
    }

    /**
     * Returns whether job requires a unmetered network connection to be run or not.
     *
     * @return True if job requires a unmetered network to be run, false otherwise.
     */
    @SuppressWarnings("unused")
    public final boolean requiresUnmeteredNetwork() {
        return requiredNetworkType >= NetworkUtil.UNMETERED;
    }

    /**package**/ long getDeadlineInMs() {
        return deadlineInMs;
    }

    /**package**/ boolean shouldCancelOnDeadline() {
        return cancelOnDeadline;
    }
}
