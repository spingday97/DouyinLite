package com.spingday97.douyinlite;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaDownloadHelper {
    public static final String DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public static void downloadToDownloads(Context ctx, String mediaUrl, String fileName, String mimeType) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(mediaUrl).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", DESKTOP_UA);
        conn.setRequestProperty("Referer", "https://www.douyin.com/");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.connect();

        int code = conn.getResponseCode();
        if (code < 200 || code >= 400) {
            throw new IllegalStateException("HTTP " + code);
        }

        try (InputStream in = conn.getInputStream()) {
            byte[] buf = new byte[8192];
            int len;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DouyinLite");
                ContentResolver resolver = ctx.getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IllegalStateException("MediaStore insert failed");
                try (OutputStream out = resolver.openOutputStream(uri)) {
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DouyinLite");
                if (!dir.exists()) dir.mkdirs();
                File outFile = new File(dir, fileName);
                try (OutputStream out = new FileOutputStream(outFile)) {
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }
            }
        }
    }
}
