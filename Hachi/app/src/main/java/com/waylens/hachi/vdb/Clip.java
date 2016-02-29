package com.waylens.hachi.vdb;

import android.os.Parcel;
import android.os.Parcelable;

import com.waylens.hachi.utils.DateTime;

public class Clip implements Parcelable {
    public static final int TYPE_REAL = -1;
    public static final int TYPE_BUFFERED = 0;
    public static final int TYPE_MARKED = 1;

    // --------------------------------------------------------------
    // CAT_REMOTE:
    // 		type: clipType (buffered 0, marked 1, or plist_id >= 256)
    // 		subType: clipId (0 for plist_id)
    // 		extra: vdbId (for server) or null (for camera)
    // --------------------------------------------------------------

    // --------------------------------------------------------------
    // clip id
    // --------------------------------------------------------------
    public static final class ID {

        public final int type; // depends on cat
        public final int subType; // depends on type
        public String extra; // unique clip id in this cat/type

        private int hash = -1; // cache hash value

        private int calcHash() {
            final int prime = 31;
            int result = 1;
            result = prime * result + type;
            result = prime * result + subType;
            result = prime * result + (extra == null ? 0 : extra.hashCode());
            return result;
        }

        @Override
        public int hashCode() {
            if (hash == -1) {
                hash = calcHash();
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;

            if (obj == null)
                return false;

            if (getClass() != obj.getClass())
                return false;

            ID other = (ID) obj;

            if (type != other.type || subType != other.subType) {
                return false;
            }

            if (extra == null) {
                return other.extra == null;
            } else {
                return extra.equals(other.extra);
            }
        }

        public ID(int type, int subType, String extra) {
            this.type = type;
            this.subType = subType;
            this.extra = extra;
        }

        public void setExtra(String extra) {
            this.extra = extra;
            this.hash = -1;
        }

    }

    // stream info: see StdMedia.java
    public static final class StreamInfo {

        public int version; // 0: invalid; current is 2

        public byte video_coding; // StdMedia
        public byte video_framerate;
        public short video_width;
        public short video_height;

        public byte audio_coding;
        public byte audio_num_channels;
        public int audio_sampling_freq;

        public final boolean valid() {
            return version != 0;
        }

    }

    public class EditInfo {
        public Clip.ID bufferedCid;
        public Clip.ID realCid;
        public long minExtensibleValue;
        public long maxExtensibleValue;
        public long selectedStartValue;
        public long selectedEndValue;
        public long currentPosition;

        public EditInfo() {
            minExtensibleValue = getStartTimeMs();
            maxExtensibleValue = getStartTimeMs() + getDurationMs();
            selectedStartValue = minExtensibleValue;
            selectedEndValue = maxExtensibleValue;
            bufferedCid = cid;
            realCid = cid;
        }
        public int getSelectedLength() {
            return (int) (selectedEndValue - selectedStartValue);
        }


    }

    public ID cid;

    public ID realCid;

    public StreamInfo[] streams;

    public int index; // index in ClipSet

    // date when the clip is created
    public int clipDate;

    public int gmtOffset;

    private long mStartTimeMs;

    // clip length ms
    protected int mDurationMs;

    // clip size in bytes
    public long clipSize = -1;

    // is marked as to be deleted
    public boolean bDeleting;

    public EditInfo editInfo;


    public Clip(int type, int subType, String extra, int clipDate, int duration) {
        this(type, subType, extra, 2, clipDate, duration);
    }

    public Clip(int type, int subType, String extra, int numStreams, int clipDate, int duration) {
        this.cid = new ID(type, subType, extra);
        streams = new StreamInfo[numStreams];
        for (int i = 0; i < numStreams; i++) {
            streams[i] = new StreamInfo();
        }

        this.clipDate = clipDate;
        this.mDurationMs = duration;
        this.editInfo = new EditInfo();
    }


    public int getDurationMs() {
        return mDurationMs;
    }


    public final String getDateTimeString() {
        return DateTime.toString(clipDate, 0);
    }

    public String getDateString() {
        return DateTime.getDateString(clipDate, 0);
    }

    public final String getTimeString() {
        return DateTime.getTimeString(clipDate, 0);
    }

    public final String getWeekDayString() {
        return DateTime.getDayName(clipDate, 0);
    }

    public Clip.StreamInfo getStream(int index) {
        return (index < 0 || index >= streams.length) ? null : streams[index];
    }

    public long getStartTimeMs() {
        return mStartTimeMs;
    }

    public void setStartTimeMs(long startTime) {
        mStartTimeMs = startTime;
    }

    public boolean contains(long timeMs) {
        return timeMs >= mStartTimeMs && timeMs < mStartTimeMs + mDurationMs;
    }

    public boolean isDownloading() {
        return false;
    }

    public int getDownloadProgress() {
        return -1;
    }


    // inherit
    public String getVdbId() {
        return (String) cid.extra;
    }

    public String toString() {
        return "Clip id: " + cid;
    }

    public long getStandardClipDate() {
        return (clipDate - gmtOffset) * 1000l;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        write(dest, cid);
        write(dest, realCid);
        write(dest, streams);
        write(dest, editInfo);
        dest.writeInt(index);
        dest.writeInt(clipDate);
        dest.writeInt(gmtOffset);
        dest.writeLong(mStartTimeMs);
        dest.writeInt(mDurationMs);
        dest.writeLong(clipSize);
        if (bDeleting) {
            dest.writeByte((byte) 1);
        } else {
            dest.writeByte((byte) 0);
        }
    }

    void write(Parcel dest, Clip.ID id) {
        if (id != null) {
            dest.writeInt(0); // means id is not null
            dest.writeInt(id.type);
            dest.writeInt(id.subType);
            if (id.extra != null) {
                dest.writeInt(0); // extra is not null
                dest.writeString(id.extra);
            } else {
                dest.writeInt(-1); // extra is null
            }
        } else {
            dest.writeInt(-1); //means id is null
        }
    }

    void write(Parcel dest, StreamInfo[] streamInfos) {
        if (streamInfos == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(streamInfos.length);
            for (StreamInfo streamInfo : streamInfos) {
                dest.writeInt(streamInfo.version);
                dest.writeByte(streamInfo.video_coding);
                dest.writeByte(streamInfo.video_framerate);
                dest.writeInt(streamInfo.video_width);
                dest.writeInt(streamInfo.video_height);
                dest.writeByte(streamInfo.audio_coding);
                dest.writeByte(streamInfo.audio_num_channels);
                dest.writeInt(streamInfo.audio_sampling_freq);
            }
        }
    }

    private void write(Parcel dest, EditInfo editInfo) {
        if (editInfo == null) {
            dest.writeInt(-1);
        } else {
            dest.writeLong(editInfo.minExtensibleValue);
            dest.writeLong(editInfo.maxExtensibleValue);
            dest.writeLong(editInfo.selectedStartValue);
            dest.writeLong(editInfo.selectedEndValue);
            dest.writeLong(editInfo.currentPosition);
        }
    }

    private Clip(Parcel in) {
        cid = readID(in);
        realCid = readID(in);
        streams = readStreams(in);
        editInfo = readEditInfo(in);
        index = in.readInt();
        clipDate = in.readInt();
        gmtOffset = in.readInt();
        mStartTimeMs = in.readLong();
        mDurationMs = in.readInt();
        clipSize = in.readLong();
        if (in.readByte() == 1) {
            bDeleting = true;
        } else {
            bDeleting = false;
        }
    }

    Clip.ID readID(Parcel in) {
        if (in.readInt() != 0) {
            return null;
        }
        int type = in.readInt();
        int subType = in.readInt();
        int hasExtra = in.readInt();
        String extra;
        if (hasExtra == 0) {
            extra = in.readString();
        } else {
            extra = null;
        }
        return new Clip.ID(type, subType, extra);
    }

    StreamInfo[] readStreams(Parcel in) {
        int length = in.readInt();
        if (length == -1) {
            return null;
        }
        StreamInfo[] infos = new StreamInfo[length];
        for (int i = 0; i < length; i++) {
            StreamInfo streamInfo = new StreamInfo();

            streamInfo.version = in.readInt();
            streamInfo.video_coding = in.readByte();
            streamInfo.video_framerate = in.readByte();
            streamInfo.video_width = (short) in.readInt();
            streamInfo.video_height = (short) in.readInt();
            streamInfo.audio_coding = in.readByte();
            streamInfo.audio_num_channels = in.readByte();
            streamInfo.audio_sampling_freq = in.readInt();
            infos[i] = streamInfo;
        }
        return infos;
    }

    private EditInfo readEditInfo(Parcel in) {
        EditInfo editInfo = new EditInfo();
        editInfo.minExtensibleValue = in.readLong();
        editInfo.maxExtensibleValue = in.readLong();
        editInfo.selectedStartValue = in.readLong();
        editInfo.selectedEndValue = in.readLong();
        editInfo.currentPosition = in.readLong();
        return editInfo;
    }

    public static final Parcelable.Creator<Clip> CREATOR = new Parcelable.Creator<Clip>() {

        @Override
        public Clip createFromParcel(Parcel source) {
            return new Clip(source);
        }

        @Override
        public Clip[] newArray(int size) {
            return new Clip[size];
        }
    };
}
