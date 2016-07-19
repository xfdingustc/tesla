package com.waylens.hachi.library.crs_svr.v2;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.waylens.hachi.library.crs_svr.ProtocolConstMsg;

/**
 * CommandHead
 * Created by Richard on 1/13/16.
 */
public class CommandHead extends CrsCommand {
    public static final short LENGTH = 8;

    public short size;
    public short cmd;
    public int version;

    public CommandHead() {
        super(null);
        version = ProtocolConstMsg.WAYLENS_VERSION;
    }

    public CommandHead(short cmd) {
        super(null);
        this.cmd = cmd;
        version = ProtocolConstMsg.WAYLENS_VERSION;
    }

    public void setSize(short size) {
        this.size = size;
    }

    @Override
    public void encode() throws IOException {
        write(size);
        write(cmd);
        write(version);
    }

    @Override
    public CommandHead decode(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            size = readShort(inputStream);
            cmd = readShort(inputStream);
            version = readInt(inputStream);
            return this;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public CommandHead getCommandHeader() {
        return null;
    }

    @Override
    public EncodeCommandHeader getEncodeHeader() {
        return null;
    }

    @Override
    public byte[] getEncodedCommand() throws IOException {
        return null;
    }

    @Override
    public String toString() {
        return String.format("size[%d], cmd[%d], version[%d]", size, cmd, version);
    }
}
