package com.waylens.hachi.uploadqueue.db;

import android.content.Context;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.app.Hachi;
import com.waylens.hachi.uploadqueue.model.DaoSession;
import com.waylens.hachi.uploadqueue.model.UploadError;
import com.waylens.hachi.uploadqueue.model.UploadRequest;
import com.waylens.hachi.uploadqueue.model.UploadRequestDao;
import com.waylens.hachi.uploadqueue.model.UploadStatus;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

/**
 * Created by Xiaofei on 2017/1/3.
 */

public class UploadQueueDBAdapter {
    private static final String TAG = UploadQueueDBAdapter.class.getSimpleName();
    private static UploadQueueDBAdapter mSharedAdapter = null;
    private UploadRequestDao mUploadRequestDao;

    private UploadQueueDBAdapter() {
        DaoSession daoSession = Hachi.getDaoSession();
        mUploadRequestDao = daoSession.getUploadRequestDao();
    }

    public static UploadQueueDBAdapter getInstance() {
        if (mSharedAdapter == null) {
            synchronized (UploadQueueDBAdapter.class) {
                if (mSharedAdapter == null) {
                    mSharedAdapter = new UploadQueueDBAdapter();
                }
            }
        }

        return mSharedAdapter;
    }

    public boolean updateRequest(UploadRequest request) {
        mUploadRequestDao.save(request);
        return true;
    }

    public boolean insert(UploadRequest request) {
        mUploadRequestDao.insert(request);
        return true;
    }

    public void delete(UploadRequest request) {
        mUploadRequestDao.delete(request);
    }

    public boolean isInUploadQueue(String key, String userId) {
        QueryBuilder<UploadRequest> qb = mUploadRequestDao.queryBuilder();
        qb.where(UploadRequestDao.Properties.Key.eq(key), UploadRequestDao.Properties.UserId.eq(userId));
        return !qb.list().isEmpty();
    }

    public List<UploadRequest> listUploadRequestByUserId(String userId) {
        QueryBuilder<UploadRequest> qb = mUploadRequestDao.queryBuilder();
        qb.where(UploadRequestDao.Properties.UserId.eq(userId));
        return qb.list();
    }

    public void updateUploadStatus() {
        QueryBuilder<UploadRequest> qb = mUploadRequestDao.queryBuilder();
        qb.whereOr(UploadRequestDao.Properties.Status.eq(UploadStatus.UPLOADING.ordinal()), UploadRequestDao.Properties.Status.eq(UploadStatus.UPLOAD_REQUEST.ordinal()));
        List<UploadRequest> requestList = qb.list();
        Logger.t(TAG).d("request list size: " + requestList.size());
        for (UploadRequest request : requestList) {
            request.setStatus(UploadStatus.WAITING);
            mUploadRequestDao.save(request);
        }

    }
}
