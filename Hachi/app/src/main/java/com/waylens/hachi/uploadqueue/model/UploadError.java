package com.waylens.hachi.uploadqueue.model;

import android.support.annotation.StringRes;

import com.waylens.hachi.R;

import org.greenrobot.greendao.converter.PropertyConverter;

/**
 * Created by Xiaofei on 2016/12/30.
 */

public enum UploadError {
    NO_ERROR(0),
    NETWORK_WEAK(R.string.error_msg_network_not_reachable),
    CONNECTION_TIMEOUT(R.string.error_msg_connection_timeout),
    CLOUD_STORAGE_NOT_AVAILABLE(R.string.error_msg_storage_full),
    UNABLE_TO_UPLOAD_FILE(R.string.error_msg_unable_to_upload_file);


    private int errorValue;

    UploadError(int value) {
        this.errorValue = value;
    }

    public @StringRes int getValue() {
        return errorValue;
    }

    public static class UploadErrorConverter implements PropertyConverter<UploadError, Integer> {

        @Override
        public UploadError convertToEntityProperty(Integer databaseValue) {
            return values()[databaseValue];
        }

        @Override
        public Integer convertToDatabaseValue(UploadError entityProperty) {
            return entityProperty.ordinal();
        }
    }
}
