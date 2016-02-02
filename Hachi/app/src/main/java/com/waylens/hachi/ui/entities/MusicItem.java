package com.waylens.hachi.ui.entities;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.waylens.hachi.ui.helpers.DownloadHelper;
import com.waylens.hachi.utils.ImageUtils;

import org.json.JSONObject;

import java.io.File;

/**
 * Created by Richard on 2/1/16.
 */
public class MusicItem implements DownloadHelper.Downloadable {
    public int id;

    public String title;

    public String description;

    public String url;

    public String md5sum;

    public int duration;

    public long updateTime;

    public String localPath;

    public boolean isDownloading;

    private String destFileName;

    public static MusicItem fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        MusicItem item = new MusicItem();

        item.id = jsonObject.optInt("id");
        item.title = jsonObject.optString("name");
        item.description = jsonObject.optString("description");
        item.url = jsonObject.optString("url");
        item.md5sum = jsonObject.optString("md5sum");
        item.duration = jsonObject.optInt("duration");
        item.updateTime = jsonObject.optLong("updateTime");

        item.destFileName = "music-" + item.id + ".m4a";
        return item;
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
        return String.format("[%d] %s", id, title);
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
        //request.setDestinationInExternalFilesDir(context, "musics", destFileName);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, destFileName);
        request.setTitle(title);
        return request;
    }

    public boolean isDownloaded() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) {
            return false;
        }
        File file = new File(dir, destFileName);
        boolean exist = file.exists();
        if (exist) {
            localPath = file.getAbsolutePath();
        }
        return exist;
    }
}
