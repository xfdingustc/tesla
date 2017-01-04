package com.waylens.hachi.uploadqueue.model;

import org.greenrobot.greendao.converter.PropertyConverter;

/**
 * Created by Xiaofei on 2016/12/30.
 */

public enum UploadStatus {

    UPLOADING("Uploading", "Uploading..."),
    PAUSED("Paused", "Paused"),
    WAITING("Waiting", "Queued"),
    FAILED("Failed", "Failed"),
    PAUSED_REQUEST("Paused_request", "Pausing"),
    UPLOAD_REQUEST("Upload_request", "Uploading..."),
    DELETE_REQUEST("Delete_request", "Deleting..."),
    DELETED("Deleted", "Deleted"),
    COMPLETED("Completed", "Saved"),
    MAX_TIRES_DONE("Failed", "Max tries exceded, please delete this file and try again later"),
    SIZE_OVERLOADED("Limit exceed", "Size limit exceeded");

    private String mStatus = null;
    private String mMessage = null;

    UploadStatus(String status, String message) {
        this.mStatus = status;
        this.mMessage = message;
    }


    public String value() {
        return mStatus;
    }


    public String message() {
        return mMessage;
    }

    public static UploadStatus get(int i) {
        return values()[i];
    }

    public static class UploadStatusConverter implements PropertyConverter<UploadStatus, Integer> {

        @Override
        public UploadStatus convertToEntityProperty(Integer databaseValue) {
            return get(databaseValue);
        }

        @Override
        public Integer convertToDatabaseValue(UploadStatus entityProperty) {
            return entityProperty.ordinal();
        }
    }
}
