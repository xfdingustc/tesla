package com.waylens.hachi.snipe;

import com.orhanobut.logger.Logger;

import java.io.IOException;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class VdbAcknowledge {
    private static final String TAG = VdbAcknowledge.class.getSimpleName();
    public final int statusCode;
    public final boolean notModified;
    private final int mCommandCode;
    public byte[] mReceiveBuffer;
    public byte[] mMsgBuffer;
    private final VdbConnection mVdbConnection;
    private int mMsgIndex;
    protected static final int MSG_MAGIC = 0xFAFBFCFF;
    private static final int VDB_ACK_SIZE = 160;
    protected int mMsgSeqid;
    protected int mUser1;
    protected int mUser2;

    private int mCmdRetCode;
    private int mMsgCode;

    protected int mMsgFlags;
    protected int mCmdTag;

    public VdbAcknowledge(int statusCode, boolean notModified, int commandCode, VdbConnection
                          vdbConnection) {
        this.statusCode = statusCode;
        this.notModified = notModified;
        this.mVdbConnection = vdbConnection;
        this.mCommandCode = commandCode;

        try {
            mReceiveBuffer = mVdbConnection.receivedAck();
            parseAcknowledge();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseAcknowledge() throws IOException {
        mMsgIndex = 0;

        int syncHeaderPos = 0;
        while (true) {
            int syncHeader = readi32();
            if (syncHeader != MSG_MAGIC) {
                continue;
            } else {
                syncHeaderPos = mMsgIndex;
                Logger.t(TAG).d("Found the Msg Magic");
                mMsgSeqid = readi32(); // ++ each time, set by server
                mUser1 = readi32(); // cmd->user1
                mUser2 = readi32(); // cmd->user2
                mMsgCode = readi16(); // cmd->cmd_code
                mMsgFlags = readi16(); // cmd->cmd_flags
                mCmdTag = readi32(); // cmd->cmd_tag
                Logger.t(TAG).d("Msg code: " + mMsgCode + " Command code: " + mCommandCode);
                mCmdRetCode = readi32();
                int extra_bytes = readi32();
                if (mMsgCode == mCommandCode) {
                    Logger.t(TAG).d("BBBBBBBBBBBBBBBBBBBBBBB");
                    break;
                }
            }
        }
    }

    public int getRetCode() {
        return mCmdRetCode;
    }


    public int readi32() {
        int result = (int) mReceiveBuffer[mMsgIndex] & 0xFF;
        mMsgIndex++;
        result |= ((int) mReceiveBuffer[mMsgIndex] & 0xFF) << 8;
        mMsgIndex++;
        result |= ((int) mReceiveBuffer[mMsgIndex] & 0xFF) << 16;
        mMsgIndex++;
        result |= ((int) mReceiveBuffer[mMsgIndex] & 0xFF) << 24;
        mMsgIndex++;
        return result;
    }

    public byte readi8() {
        byte result = mReceiveBuffer[mMsgIndex];
        mMsgIndex++;
        return result;
    }

    public short readi16() {
        int result = (int) mReceiveBuffer[mMsgIndex] & 0xFF;
        mMsgIndex++;
        result |= ((int) mReceiveBuffer[mMsgIndex] & 0xFF) << 8;
        mMsgIndex++;
        return (short) result;
    }

    public long readi64() {
        int lo = readi32();
        int hi = readi32();
        return ((long)hi << 32) | ((long)lo & 0xFFFFFFFFL);
    }

    public final void skip(int n) {
        mMsgIndex += n;
    }

}
