package com.waylens.hachi.service.download;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Xiaofei on 2016/9/2.
 */
public class Downloadable implements Parcelable {

    private int mProgress;
    private long mCurrentFileSize;
    private long mTotalFileSize;


    public Downloadable() {

    }

    protected Downloadable(Parcel in) {
        this.mProgress = in.readInt();
        this.mCurrentFileSize = in.readLong();
        this.mTotalFileSize = in.readLong();
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {
        this.mProgress = progress;
    }

    public long getCurrentFileSize() {
        return mCurrentFileSize;
    }

    public void setCurrentFileSize(long currentFileSize) {
        this.mCurrentFileSize = currentFileSize;
    }

    public long getTotalFileSize() {
        return mTotalFileSize;
    }

    public void setTotalFileSize(long totalFileSize) {
        this.mTotalFileSize = totalFileSize;
    }




    public static final Creator<Downloadable> CREATOR = new Creator<Downloadable>() {
        @Override
        public Downloadable createFromParcel(Parcel in) {
            return new Downloadable(in);
        }

        @Override
        public Downloadable[] newArray(int size) {
            return new Downloadable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mProgress);
        parcel.writeLong(this.mCurrentFileSize);
        parcel.writeLong(this.mTotalFileSize);
    }
}
