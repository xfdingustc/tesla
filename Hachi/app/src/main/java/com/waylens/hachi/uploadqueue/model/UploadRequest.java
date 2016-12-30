package com.waylens.hachi.uploadqueue.model;

import com.waylens.hachi.ui.entities.LocalMoment;

import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * Created by Xiaofei on 2016/12/30.
 */

@Entity
public class UploadRequest {
    @Id(autoincrement = true)
    private Long id;

    private String key;

    private String userId;

    private String title;

    private boolean uploading;

    private int progress;

    @Convert(converter = LocalMoment.LocalMomentConverter.class, columnType = byte[].class)
    private LocalMoment localMoment;

    @Convert(converter = UploadStatus.UploadStatusConverter.class, columnType = Integer.class)
    private UploadStatus status;

    @Convert(converter = UploadError.UploadErrorConverter.class, columnType = Integer.class)
    public UploadError currentError = UploadError.NO_ERROR;





    @Generated(hash = 447153448)
    public UploadRequest(Long id, String key, String userId, String title, boolean uploading,
            int progress, LocalMoment localMoment, UploadStatus status,
            UploadError currentError) {
        this.id = id;
        this.key = key;
        this.userId = userId;
        this.title = title;
        this.uploading = uploading;
        this.progress = progress;
        this.localMoment = localMoment;
        this.status = status;
        this.currentError = currentError;
    }

    @Generated(hash = 153365228)
    public UploadRequest() {
    }





    public String getKey() {
        return key;
    }

    public void setStatus(UploadStatus status) {
        this.status = status;
    }

    public UploadStatus getStatus() {
        return status;
    }




    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    public UploadError getCurrentError() {
        return this.currentError;
    }

    public void setCurrentError(UploadError currentError) {
        this.currentError = currentError;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }



    public Long getId() {
        return this.id;
    }



    public void setId(Long id) {
        this.id = id;
    }



    public LocalMoment getLocalMoment() {
        return this.localMoment;
    }



    public void setLocalMoment(LocalMoment localMoment) {
        this.localMoment = localMoment;
    }

    public boolean getUploading() {
        return this.uploading;
    }

    public void setUploading(boolean uploading) {
        this.uploading = uploading;
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }



}
