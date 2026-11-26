package com.spingday97.douyinlite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://www.douyin.com/";
    private static final String MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; SM-N976U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

    private WebView webView;
    private String cachedJs;
    private long lastBackMs = 0L;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        w.setStatusBarColor(Color.BLACK);
        w.setNavigationBarColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        setContentView(webView);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setUserAgentString(MOBILE_UA);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        cachedJs = readAsset("douyin_lite.js");
        webView.addJavascriptInterface(new LiteBridge(), "DouyinLiteBridge");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                // Block snssdk/snssdk1128 deep-link redirects to official Douyin app
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                CookieManager.getInstance().flush();
                if (cachedJs != null && !cachedJs.isEmpty()) {
                    view.evaluateJavascript(cachedJs, null);
                }
            }
        });

        webView.loadUrl(HOME_URL);
    }

    private String readAsset(String name) {
        try (InputStream is = getAssets().open(name);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBackMs < 1800) {
            super.onBackPressed();
        } else {
            lastBackMs = now;
            Toast.makeText(this, "再按一次退出 DouyinLite", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    public class LiteBridge {
        @JavascriptInterface
        public void copyText(String text) {
            runOnUiThread(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("DouyinLite", text));
                    Toast.makeText(MainActivity.this, "已复制链接", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void toast(String msg) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void reloadHome() {
            runOnUiThread(() -> webView.loadUrl(HOME_URL));
        }

        @JavascriptInterface
        public void downloadMedia(String mediaUrl, String fileName, String mimeType) {
            if (mediaUrl == null || mediaUrl.isEmpty()) {
                toast("未获取到可下载链接");
                return;
            }
            toast("开始下载: " + fileName);
            new Thread(() -> {
                try {
                    URL url = new URL(mediaUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", MOBILE_UA);
                    conn.setRequestProperty("Referer", HOME_URL);
                    String cookie = CookieManager.getInstance().getCookie(mediaUrl);
                    if (cookie != null) conn.setRequestProperty("Cookie", cookie);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.connect();

                    int code = conn.getResponseCode();
                    if (code < 200 || code >= 400) {
                        toast("下载失败 HTTP " + code);
                        return;
                    }

                    try (InputStream in = conn.getInputStream()) {
                        saveToDownloads(in, fileName, mimeType);
                    }
                    toast("已保存到 Download/DouyinLite/" + fileName);
                } catch (Exception e) {
                    toast("下载失败: " + e.getMessage());
                }
            }).start();
        }

        private void saveToDownloads(InputStream in, String fileName, String mimeType) throws Exception {
            byte[] buf = new byte[8192];
            int len;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType != null ? mimeType : "video/mp4");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DouyinLite");
                ContentResolver resolver = getContentResolver();
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
