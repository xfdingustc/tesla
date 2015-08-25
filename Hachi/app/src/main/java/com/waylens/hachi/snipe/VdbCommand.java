package com.waylens.hachi.snipe;

/**
 * Created by Xiaofei on 2015/8/19.
 */
public class VdbCommand {
    private static final int VDB_CMD_SIZE = 160;
    private byte[] mCmdBuffer = new byte[VDB_CMD_SIZE];
    private int mSendIndex = 0;
    private int mCommndCode;

    private VdbCommand() {

    }

    private final void writeCmdCode(int code, int tag) {
        mSendIndex = 0;
        writei32(code);
        writei32(tag);
        writei32(0); // user1
        writei32(0); // user2
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
    }
}
