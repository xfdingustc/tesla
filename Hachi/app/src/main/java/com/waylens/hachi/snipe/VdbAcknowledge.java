package com.waylens.hachi.snipe;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by Xiaofei on 2015/8/18.
 */
public class VdbAcknowledge {
    private static final String TAG = VdbAcknowledge.class.getSimpleName();
    public final int statusCode;
    public final boolean notModified;
    private final int mCmdCode;
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

    public VdbAcknowledge(int statusCode, boolean notModified, int cmdCode, VdbConnection
                          vdbConnection) {
        this.statusCode = statusCode;
        this.notModified = notModified;
        this.mVdbConnection = vdbConnection;
        this.mCmdCode = cmdCode;

        try {
            while (true) {
                mReceiveBuffer = mVdbConnection.receivedAck();
                parseAcknowledge();
                if (mCmdCode == mMsgCode) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseAcknowledge() throws IOException {
        mMsgIndex = 0;
        if (readi32() != MSG_MAGIC) {
            //
        }
        mMsgSeqid = readi32(); // ++ each time, set by server
        mUser1 = readi32(); // cmd->user1
        mUser2 = readi32(); // cmd->user2
        mMsgCode = readi16(); // cmd->cmd_code
        mMsgFlags = readi16(); // cmd->cmd_flags
        mCmdTag = readi32(); // cmd->cmd_tag

        mCmdRetCode = readi32();
        int extra_bytes = readi32();
        if (extra_bytes > 0) {
            mMsgBuffer = new byte[VDB_ACK_SIZE + extra_bytes];
            System.arraycopy(mReceiveBuffer, 0, mMsgBuffer, 0, VDB_ACK_SIZE);
            mVdbConnection.readFully(mMsgBuffer, VDB_ACK_SIZE, extra_bytes);
            mReceiveBuffer = mMsgBuffer;
        }

        mMsgIndex = 32;
    }

    public int getRetCode() {
        return mCmdRetCode;
    }

    public int getMsgCode() {
        return mMsgCode;
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

    public void skip(int n) {
        mMsgIndex += n;
    }

    public void readByteArray(byte[] output, int size) {
        System.arraycopy(mReceiveBuffer, mMsgIndex, output, 0, size);
        mMsgIndex += size;
    }

    public String readString() {
        int size = readi32();
        String result;
        try {
            result = new String(mReceiveBuffer, mMsgIndex, size - 1, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            result = "";
        }
        mMsgIndex += size;
        return result;
    }

    public String readStringAligned() {
        int size = readi32();
        if (size <= 0)
            return "";
        String result;
        try {
            result = new String(mMsgBuffer, mMsgIndex, size - 1, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            result = "";
        }
        mMsgIndex += size;
        if ((size % 4) != 0) {
            mMsgIndex += 4 - (size % 4);
        }
        return result;
    }

}
