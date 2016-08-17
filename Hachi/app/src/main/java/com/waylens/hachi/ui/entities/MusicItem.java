package com.waylens.hachi.ui.entities;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import com.waylens.hachi.ui.helpers.DownloadHelper;
import com.waylens.hachi.utils.ImageUtils;

import org.json.JSONObject;

import java.io.File;

/**
 * Created by Richard on 2/1/16.
 */
public class MusicItem implements DownloadHelper.Downloadable {

    public static final int STATUS_LOCAL = 0;

    public static final int STATUS_REMOTE = 1;

    public static final int STATUS_DOWNLOADING = 2;

    public int id;

    public String name;

    public String description;

    public String url;

    public String md5sum;

    public int duration;

    public long updateTime;

    public String localPath;

    public int status;

    private String destFileName;

    public static MusicItem fromJson(JSONObject jsonObject, Context context) {
        if (jsonObject == null) {
            return null;
        }
        MusicItem item = new MusicItem();

        item.id = jsonObject.optInt("id");
        item.name = jsonObject.optString("name");
        item.description = jsonObject.optString("description");
        item.url = jsonObject.optString("url");
        item.md5sum = jsonObject.optString("md5sum");
        item.duration = jsonObject.optInt("duration");
        item.updateTime = jsonObject.optLong("updateTime");

        item.checkLocalFile(context);
        return item;
    }

    public void checkLocalFile(Context context) {
        destFileName = "music-" + id + ".m4a";
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (dir == null) {
            status = STATUS_REMOTE;
            return;
        }
        File file = new File(dir, destFileName);
        if (file.exists()) {
            localPath = file.getAbsolutePath();
            status = STATUS_LOCAL;
        } else {
            status = STATUS_REMOTE;
            localPath = null;
        }
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof MusicItem)) {
            MusicItem other = (MusicItem) obj;
            return id == other.id;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("[%d] %s", id, name);
    }

    @Override
    public DownloadManager.Request getDownloadRequest(Context context) {
        if (!ImageUtils.isExternalStorageReady()) {
            return null;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        request.setAllowedOverRoaming(true);
        request.setMimeType("audio/mp4");
        request.setVisibleInDownloadsUi(true);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MUSIC, destFileName);
        //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, destFileName);
        request.setTitle(name);
        return request;
    }

    public boolean isDownloaded() {
        return status == STATUS_LOCAL;
    }

    public Bundle toBundle() {
        Bundle data = new Bundle();
        data.putInt("id", id);
        data.putString("name", name);
        data.putString("localPath", localPath);
        data.putInt("duration", duration);
        return data;
    }

    public static MusicItem fromBundle(Bundle bundle) {
        MusicItem musicItem = new MusicItem();
        musicItem.id = bundle.getInt("id");
        musicItem.duration = bundle.getInt("duration");
        musicItem.name = bundle.getString("name");
        musicItem.localPath = bundle.getString("localPath");
        return musicItem;
    }
}
