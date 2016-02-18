package com.waylens.hachi.vdb;

import com.waylens.hachi.utils.DateTime;

public class Clip {
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
        public Object extra; // unique clip id in this cat/type

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

        public ID(int type, int subType, Object extra) {
            this.type = type;
            this.subType = subType;
            this.extra = extra;
        }

        public void setExtra(Object extra) {
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

    public final ID cid;

    public ID realCid;

    public final StreamInfo[] streams;

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

    public Clip(int type, int subType, Object extra, int clipDate, int duration) {
        this(type, subType, extra, 2, clipDate, duration);
    }

    public Clip(int type, int subType, Object extra, int numStreams, int clipDate, int duration) {
        this.cid = new ID(type, subType, extra);
        streams = new StreamInfo[numStreams];
        for (int i = 0; i < numStreams; i++) {
            streams[i] = new StreamInfo();
        }

        this.clipDate = clipDate;
        this.mDurationMs = duration;
    }



    public boolean isLocal() {
        return false;
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


}
