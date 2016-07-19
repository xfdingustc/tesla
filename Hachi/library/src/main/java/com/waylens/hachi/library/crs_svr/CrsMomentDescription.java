package com.waylens.hachi.library.crs_svr;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Richard on 1/13/16.
 */
public class CrsMomentDescription extends CrsCommand {


    String jid;
    long momentID;
    String backgroundMusic;
    ArrayList<CrsFragment> fragments = new ArrayList<>();

    public CrsMomentDescription(String userID,
                                long momentID,
                                String bgMusic,
                                String privateKey) {
        super(privateKey);
        jid = userID + "/" + WAYLENS_RESOURCE_TYPE_ANDROID;
        this.momentID = momentID;
        this.backgroundMusic = bgMusic;
    }

    public void addFragment(CrsFragment fragment) {
        fragments.add(fragment);
    }

    @Override
    public void encode() throws IOException {
        write(jid, true);
        write(momentID);
        write(backgroundMusic, true);
        write((byte)fragments.size());
        for (CrsFragment fragment : fragments) {
            write(fragment.guid, true);
            write(fragment.captureTime, true);
            write(fragment.startTime);
            write(fragment.offset);
            write(fragment.duration);
            write(fragment.frameRate);
            write(fragment.resolution);
            write(fragment.dataType);
        }
    }

    @Override
    public CrsMomentDescription decode(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            jid = readString(inputStream, 0);
            momentID = readLong(inputStream);
            backgroundMusic = readString(inputStream, 0);
            fragments.clear();
            byte fragmentCount = readByte(inputStream);
            for (int i = 0; i < fragmentCount; i++) {
                CrsFragment fragment = new CrsFragment();
                fragment.guid = readString(inputStream, 0);
                fragment.captureTime = readString(inputStream, 0);
                fragment.startTime = readLong(inputStream);
                fragment.offset = readLong(inputStream);
                fragment.duration = readInt(inputStream);
                fragment.frameRate = readDouble(inputStream);
                fragment.resolution = readInt(inputStream);
                fragment.dataType = readInt(inputStream);
                fragments.add(fragment);
            }
            return this;
        } catch (IOException e) {
            Log.e("CrsMomentDescription", "", e);
            return null;
        }
    }

    @Override
    public CommandHead getCommandHeader() {
        return new CommandHead(CRS_C2S_UPLOAD_MOMENT_DESC);
    }

    @Override
    public EncodeCommandHeader getEncodeHeader() {
        return new EncodeCommandHeader();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (CrsFragment fragment : fragments) {
            sb.append(fragment).append(";");
        }
        return String.format("jid[%s], momentId[%d], bgMusic[%s], fragments[%d], fragmentContent[%s]",
                jid, momentID, backgroundMusic, fragments.size(), sb.toString());
    }
}
