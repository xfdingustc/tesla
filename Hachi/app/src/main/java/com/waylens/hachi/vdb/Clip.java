package com.waylens.hachi.vdb;

import android.net.Uri;

import com.transee.common.DateTime;

import java.util.ArrayList;
import java.util.List;

public class Clip {
    // clip categories: one vdb provoides one category
    public static final int CAT_UNKNOWN = 0; // unknown clip type
    public static final int CAT_REMOTE = 1; // vidit camera clips: RemoteClip
    public static final int CAT_LOCAL = 2; // downloaded & downloading clips: LocalClip
    public static final int CAT_NATIVE = 3; // Android native clips: not implemented



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

        public final int cat; // CAT_REMOTE, etc
        public final int type; // depends on cat
        public final int subType; // depends on type
        public Object extra; // unique clip id in this cat/type

        private int hash = -1; // cache hash value

        final private int calcHash() {
            final int prime = 31;
            int result = 1;
            result = prime * result + cat;
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

            if (cat != other.cat || type != other.type || subType != other.subType)
                return false;

            if (extra == null) {
                return other.extra == null;
            } else {
                return extra.equals(other.extra);
            }
        }

        public ID(int cat, int type, int subType, Object extra) {
            this.cat = cat;
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

    // clip length ms
    protected int mDurationMs;

    // clip size in bytes
    public long clipSize = -1;

    // is marked as to be deleted
    public boolean bDeleting;

    public Clip(ID cid, int numStreams) {
        this.cid = cid;
        streams = new StreamInfo[numStreams];
        for (int i = 0; i < numStreams; i++) {
            streams[i] = new StreamInfo();
        }
    }

    // API
    public boolean isLocal() {
        return false;
    }

    public int getDurationMs() {
        return mDurationMs;
    }


    // API
    public final String getDateTimeString() {
        return DateTime.toString(clipDate, 0);
    }

    // API
    public final String getDateString() {
        return DateTime.getDateString(clipDate, 0);
    }

    // API
    public final String getTimeString() {
        return DateTime.getTimeString(clipDate, 0);
    }

    // API
    public final String getWeekDayString() {
        return DateTime.getDayName(clipDate, 0);
    }

    // inherit
    public Clip.StreamInfo getStream(int index) {
        return (index < 0 || index >= streams.length) ? null : streams[index];
    }

    // inherit
    public long getStartTimeMs() {
        return 0;
    }

    // inherit
    public boolean contains(long timeMs) {
        return false;
    }

    // inherit
    public boolean isDownloading() {
        return false;
    }

    // inherit
    public int getDownloadProgress() {
        return -1;
    }


    // inherit
    public String getVdbId() {
        return null;
    }

    public String toString() {
        return "Clip id: " + cid;
    }




}
