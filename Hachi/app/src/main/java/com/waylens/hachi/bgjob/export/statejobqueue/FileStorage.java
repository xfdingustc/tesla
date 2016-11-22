package com.waylens.hachi.bgjob.export.statejobqueue;

import android.content.Context;
import android.support.annotation.Nullable;

import com.orhanobut.logger.Logger;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;


public class FileStorage {
    private static final String TAG = FileStorage.class.getSimpleName();
    private static final String EXT = ".jobs";
    private final File folder;
    FileStorage(Context appContext, String id) {
        this.folder = new File(appContext.getDir("state_jobs", Context.MODE_PRIVATE),
                "files_" + id);
        //noinspection ResultOfMethodCallIgnored
        this.folder.mkdirs();
    }

    void delete(String id) {
        final File file = toFile(id);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    @Nullable
    byte[] load(String id) throws IOException {
        final File file = toFile(id);
        if (file.exists() && file.canRead()) {
            BufferedSource source = Okio.buffer(Okio.source(file));
            try {
                return source.readByteArray();
            } finally {
                closeQuitely(source);
            }

        }
        return null;
    }

    void save(String id, byte[] data) throws IOException {
        final File file = toFile(id);
        BufferedSink sink = Okio.buffer(Okio.sink(file));
        try {
            sink.write(data).flush();
        } finally {
            closeQuitely(sink);
        }
    }

    private static String filename(String id) {
        return id + EXT;
    }

    private File toFile(String id) {
        return new File(folder, filename(id));
    }

    @Nullable
    private static String filenameToId(String filename) {
        if (filename.length() < EXT.length() + 1) {
            return null;
        }
        return filename.substring(0, filename.length() - EXT.length());
    }

    void truncateExcept(Set<String> ids) {
        for (String filename : folder.list()) {
            if (!filename.endsWith(EXT)) {
                continue;
            }
            String id = filenameToId(filename);
            if (!ids.contains(id)) {
                File file = new File(folder, filename);
                if (!file.delete()) {
                    Logger.t(TAG).d("cannot delete unused job toFile " + file.getAbsolutePath());
                }
            }
        }
    }

    private static void closeQuitely(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }
}
