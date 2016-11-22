package com.waylens.hachi.bgjob.export.statejobqueue;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.jobqueue.JobHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by lshw on 16/11/17.
 */

public class PersistentQueue {
    public static final String TAG = PersistentQueue.class.getSimpleName();
    private static PersistentQueue mSharedPersistentQueue = null;

    private DbOpenHelper dbOpenHelper;
    private SQLiteDatabase db;
    private FileStorage jobStorage;
    private JobSerializer jobSerializer;
    private List<StateJobHolder> cachedJobList;
    private Context context;

    public StateJobHolder getCurrentJob() {
        return currentJob;
    }

    public void setCurrentJob(StateJobHolder currentJob) {
        this.currentJob = currentJob;
    }

    private StateJobHolder currentJob = null;

    private PersistentQueue(Context context, String tag, JobSerializer serializer) {
        this.context = context;
        jobStorage = new FileStorage(context, "jobs_" + tag);
        dbOpenHelper = new DbOpenHelper(context, ("db_" + tag));
        db = dbOpenHelper.getWritableDatabase();
        jobSerializer = serializer;
        cleanupFiles();
    }

    public static PersistentQueue getPersistentQueue() {
        Context context = Hachi.getContext();
        JobSerializer jobSerializer = new JavaSerializer();
        if (mSharedPersistentQueue == null) {
            synchronized (PersistentQueue.class) {
                if (mSharedPersistentQueue == null) {
                    mSharedPersistentQueue = new PersistentQueue(context, TAG, jobSerializer);
                    mSharedPersistentQueue.cachedJobList = new ArrayList<>();
                    mSharedPersistentQueue.cachedJobList.addAll(mSharedPersistentQueue.findAllJobs());
                    if (mSharedPersistentQueue.cachedJobList.size() != 0) {
                        mSharedPersistentQueue.currentJob = mSharedPersistentQueue.cachedJobList.get(0);
                    }
                }
            }
        }
        return mSharedPersistentQueue;
    }

    private void startService() {
        context.startService(new Intent(context, CacheUploadMomentJob.class));
    }

    public List<StateJobHolder> getAllJobs() {
        if (cachedJobList == null) {
            cachedJobList = new ArrayList<>();
            cachedJobList.addAll(mSharedPersistentQueue.findAllJobs());
        } else {
            cachedJobList.clear();
            cachedJobList.addAll(findAllJobs());
        }
        return cachedJobList;
    }
    public int getJobCount() {
        if (cachedJobList != null) {
            return cachedJobList.size();
        } else {
            return 0;
        }
    }

    public StateJobHolder peekJob() {
        if (cachedJobList.size() == 0) {
            Logger.t(TAG).d("peek job empty");
            return null;
        } else {
            if (currentJob == null) {
                currentJob = cachedJobList.get(0);
                Logger.t(TAG).d("peek job not empty");
                return currentJob;
            } else {
                int index = cachedJobList.indexOf(currentJob);
                currentJob = cachedJobList.get((index + 1) % cachedJobList.size());
                return currentJob;
            }
        }
    }

    public void updateJob(StateJobHolder jobHolder) {
        persistJobToDisk(jobHolder);
        ContentValues values = new ContentValues();
        values.put("jobState", jobHolder.getJobState());
        values.put("jobTemp", jobHolder.getJobTemp());
        db.update(DbOpenHelper.DATABASE_NAME, values, "jobId = ?", new String[]{ jobHolder.getJobId() });
    }

    public void updateJobProgress(StateJobHolder jobHolder) {
        for (StateJobHolder jobHolderItem : cachedJobList) {
            if (jobHolderItem.getJobId().equals(jobHolder.getJobId())) {
                jobHolderItem.setJob(jobHolder.getJob());
            }
        }
    }

    public boolean insert(StateJobHolder jobHolder) {
        Logger.t(TAG).d("insert job id = " + jobHolder.getJobId());
        cachedJobList.add(jobHolder);
        persistJobToDisk(jobHolder);
        ContentValues values = new ContentValues();
        values.put("jobId", jobHolder.getJobId());
        values.put("jobState", jobHolder.getJobState());
        values.put("jobTemp", jobHolder.getJobTemp());
        long insertId = db.insert(DbOpenHelper.DATABASE_NAME, null, values);
        Logger.t(TAG).d("insertId = " + insertId);
        jobHolder.setInsertId(insertId);
        //startService();
        return true;
    }

    private void cleanupFiles() {
        Cursor cursor = db.rawQuery(DbOpenHelper.LOAD_ALL_IDS_QUERY, null);
        Set<String> jobIds = new HashSet<>();
        try {
            while (cursor.moveToNext()) {
                jobIds.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        jobStorage.truncateExcept(jobIds);
    }

    public void delete(String id) {
        for (StateJobHolder jobHolder : cachedJobList) {
            if (jobHolder.getJobId().equals(id)) {
                cachedJobList.remove(jobHolder);
            }
        }
        db.beginTransaction();
        try {
            SQLiteStatement stmt = dbOpenHelper.getDeleteStatement();
            stmt.clearBindings();
            stmt.bindString(1, id);
            stmt.execute();
            db.setTransactionSuccessful();
            jobStorage.delete(id);
        } finally {
            db.endTransaction();
        }
    }

    public StateJobHolder findJobById(@NonNull String id) {
        Logger.t(TAG).d("id = " + id);
        Cursor cursor = db.rawQuery(DbOpenHelper.FIND_BY_ID_QUERY, new String[]{id});
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return createJobHolderFromCursor(cursor);
        } catch (Exception e) {
            Logger.t(TAG).d("invalid job on findJobById");
            return null;
        } finally {
            cursor.close();
        }

    }

    public List<StateJobHolder> findAllJobs() {
        List<StateJobHolder> jobs = new ArrayList<>();
        Cursor cursor = db.rawQuery(DbOpenHelper.LOAD_ALL_JOBS, new String[0]);
        try {
            while (cursor.moveToNext()) {
                jobs.add(createJobHolderFromCursor(cursor));
            }
        } catch (Exception e) {
            Logger.t(TAG).d("");
        } finally {
            cursor.close();
        }
        return jobs;
    }

    private StateJobHolder createJobHolderFromCursor(Cursor cursor) throws Exception{
        String jobId = cursor.getString(1);
        Logger.t(TAG).d("cusor jobId = " + jobId);
        Job job;
        try {
            Logger.t(TAG).d("read buffer = " + new String(jobStorage.load(jobId)));
            job = safeDeserialize(jobStorage.load(jobId));
        } catch (IOException e) {
            Logger.t(TAG).d("error");
            throw new Exception();
        }
        if (job == null) {
            throw new Exception();
        }
        return new StateJobHolder(cursor.getLong(0), cursor.getString(1), cursor.getInt(2), cursor.getString(3), job);
    }

    private Job safeDeserialize(byte[] bytes) {
        try {
            return jobSerializer.deserialize(bytes);
        } catch (Throwable t) {
            Logger.t(TAG).d("job deserialize error");
        }
        return null;
    }

    private void persistJobToDisk(@NonNull StateJobHolder jobHolder) {
        try {
            jobStorage.save(jobHolder.getJobId(), jobSerializer.serialize(jobHolder.getJob()));
        } catch (IOException e) {
            throw new RuntimeException("cannot save job to disk", e);
        }
    }

    public UploadMomentJob getUploadingJob(int index) {
        if (index >= 0 && index < cachedJobList.size()) {
            return (UploadMomentJob) cachedJobList.get(index).getJob();
        } else {
            return null;
        }
    }


    public static class JavaSerializer implements JobSerializer {

        @Override
        public byte[] serialize(Object object) throws IOException {
            if (object == null) {
                return null;
            }
            ByteArrayOutputStream bos = null;
            try {
                bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(object);
                // Get the bytes of the serialized object
                return bos.toByteArray();
            } finally {
                if (bos != null) {
                    bos.close();
                }
            }
        }

        @Override
        public <T extends Job> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new ByteArrayInputStream(bytes));
                //noinspection unchecked
                return (T) in.readObject();
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    public interface JobSerializer {
        byte[] serialize(Object object) throws IOException;
        <T extends Job> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException;
    }

}
