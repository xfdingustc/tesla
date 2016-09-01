package com.waylens.hachi.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Xiaofei on 2016/9/2.
 */
public class FileUtils {
    public static void writeFile(InputStream inputStream, File file) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (file != null && file.exists()) {
            file.delete();
        }

        FileOutputStream out = new FileOutputStream(file);
        byte[] buffer = new byte[1024 * 128];
        int length = -1;
        while ((length = inputStream.read(buffer)) != -1 ) {
            out.write(buffer, 0, length);
        }

        out.flush();
        out.close();
        inputStream.close();
    }
}
