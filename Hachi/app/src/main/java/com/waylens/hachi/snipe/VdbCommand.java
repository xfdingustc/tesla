package com.waylens.hachi.snipe;

import android.util.Log;

import com.orhanobut.logger.Logger;
import com.transee.vdb.Clip;
import com.transee.vdb.ClipPos;

/**
 * Created by Xiaofei on 2015/8/19.
 */
public class VdbCommand {
    private static final String TAG = VdbCommand.class.getSimpleName();
    private static final int VDB_CMD_SIZE = 160;
    private byte[] mCmdBuffer = new byte[VDB_CMD_SIZE];
    private int mSendIndex = 0;
    private int mCommndCode;

    private VdbCommand() {

    }

    private final void writeCmdCode(int code, int tag) {
        writeCmdCode(code, tag, 0, 0);
    }

    private final void writeCmdCode(int code, int tag, int user1, int user2) {
        mSendIndex = 0;
        writei32(code);
        writei32(tag);
        writei32(user1); // user1
        writei32(user2); // user2
    }


    public byte[] getCmdBuffer() {
        return mCmdBuffer;
    }

    public int getCommandCode() {
        return mCommndCode;
    }

    private final void writei32(int value) {
        mCmdBuffer[mSendIndex] = (byte)(value);
        mSendIndex++;
        mCmdBuffer[mSendIndex] = (byte)(value >> 8);
        mSendIndex++;
        mCmdBuffer[mSendIndex] = (byte)(value >> 16);
        mSendIndex++;
        mCmdBuffer[mSendIndex] = (byte)(value >> 24);
        mSendIndex++;
    }

    private final void writei64(long value) {
        writei32((int) value);
        writei32((int) (value >> 32));
    }

    private final void writeVdbId(String vdbId) {
        if (vdbId == null) {
            return;
        }
        int length = vdbId.length();
        // 4 + length + 0 + aligned_to_4
        int align = 0;
        if ((length + 1) % 4 != 0) {
            align = 4 - (length + 1) % 4;
        }
        // check buffer length
        if (mSendIndex + 4 + length + 1 + align > VDB_CMD_SIZE) {
            Logger.t(TAG).w("vdb_id is too long: " + length);
            return;
        }
        writei32(length + 1);
        for (int i = 0; i < length; i++) {
            mCmdBuffer[mSendIndex] = (byte) vdbId.charAt(i);
            mSendIndex++;
        }
        for (int i = 0; i <= align; i++) {
            mCmdBuffer[mSendIndex] = 0;
            mSendIndex++;
        }
    }


    private static class Builder {
        private VdbCommand mVdbCommand;
        private Builder() {
            mVdbCommand = new VdbCommand();
        }

        private Builder writeCmdCode(int code, int tag) {
            mVdbCommand.writeCmdCode(code, tag);
            mVdbCommand.mCommndCode = code;
            return this;
        }


        private Builder writeInt32(int value) {
            mVdbCommand.writei32(value);
            return this;
        }

        private Builder writeInt64(long value) {
            mVdbCommand.writei64(value);
            return this;
        }

        private Builder writeVdbId(String vdbId) {
            mVdbCommand.writeVdbId(vdbId);
            return this;
        }

        private VdbCommand build() {
            return mVdbCommand;
        }
    }


    public static class Factory {

        protected static final int CMD_Null = 0;
        protected static final int CMD_GetVersionInfo = 1;
        protected static final int CMD_GetClipSetInfo = 2;
        protected static final int CMD_GetIndexPicture = 3;
        protected static final int CMD_GetPlaybackUrl = 4;
        // protected static final int CMD_GetDownloadUrl = 5; // obsolete
        protected static final int CMD_MarkClip = 6;
        // protected static final int CMD_GetCopyState = 7; // obsolete
        protected static final int CMD_DeleteClip = 8;
        protected static final int CMD_GetRawData = 9;
        protected static final int CMD_SetRawDataOption = 10;
        protected static final int CMD_GetRawDataBlock = 11;
        protected static final int CMD_GetDownloadUrlEx = 12;

        protected static final int CMD_GetAllPlaylists = 13;
        protected static final int CMD_GetPlaylistIndexPicture = 14;
        protected static final int CMD_ClearPlaylist = 15;
        protected static final int CMD_InsertClip = 16;
        protected static final int CMD_MoveClip = 17;
        protected static final int CMD_GetPlaylistPlaybackUrl = 18;

        protected static final int CMD_GetClipExtent = 32;
        protected static final int CMD_SetClipExtent = 33;

        public static VdbCommand createCmdGetClipSetInfo(int type) {
            return new Builder()
                .writeCmdCode(CMD_GetClipSetInfo, 0)
                .writeInt32(type)
                .build();
        }

        public static VdbCommand createCmdGetIndexPicture(ClipPos clipPos) {
            int cmd = CMD_GetIndexPicture;
            if (false && clipPos.getType() == ClipPos.TYPE_POSTER) {
                cmd |= (1 << 16);
            }
            return new Builder()
                .writeCmdCode(cmd, clipPos.getType())
                .writeInt32(clipPos.cid.type)
                .writeInt32(clipPos.cid.subType)
                .writeInt32(clipPos.getType() | (clipPos.isLast() ? ClipPos.F_IS_LAST : 0))
                .writeInt64(clipPos.getClipTimeMs())
                .build();
        }
    }
}
