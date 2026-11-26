package com.spingday97.douyinlite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UnstableApi
public class MainActivity extends Activity implements FeedAdapter.ActionListener {
    private ViewPager2 viewPager;
    private FeedAdapter adapter;
    private ExoPlayer player;
    private WebView bridgeWebView;
    private FrameLayout webContainer;
    private TextView statusBanner;

    private String cachedJs;
    private int currentPos = 0;
    private boolean cleanScreen = false;
    private float speed = 1.0f;
    private long lastBackMs = 0L;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        w.setStatusBarColor(Color.BLACK);
        w.setNavigationBarColor(Color.BLACK);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        // 1. Native ExoPlayer with Douyin headers
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", MediaDownloadHelper.DESKTOP_UA);
        headers.put("Referer", "https://www.douyin.com/");
        DefaultHttpDataSource.Factory dsFactory = new DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(headers)
                .setAllowCrossProtocolRedirects(true);
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dsFactory))
                .build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);

        // 2. Native vertical ViewPager2
        viewPager = new ViewPager2(this);
        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        adapter = new FeedAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPos = position;
                attachAndPlay(position);
                if (position >= adapter.getItemCount() - 3) {
                    bridgeWebView.evaluateJavascript("window.__dylLoadMore && window.__dylLoadMore()", null);
                }
            }
        });
        root.addView(viewPager, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 3. Top minimal overlay bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(12);
        topBar.setPadding(p, dp(32), p, dp(8));

        statusBanner = makeTopPill("正在连接精选流...");
        TextView btnSearch = makeTopPill("🔍 搜索");
        TextView btnRefresh = makeTopPill("↻ 刷新");
        TextView btnLogin = makeTopPill("👤 登录/验证");

        btnSearch.setOnClickListener(v -> showSearchDialog());
        btnRefresh.setOnClickListener(v -> {
            statusBanner.setText("刷新中...");
            bridgeWebView.loadUrl("https://www.douyin.com/jingxuan");
        });
        btnLogin.setOnClickListener(v -> toggleWebLogin(true));

        topBar.addView(statusBanner);
        View spacer = new View(this);
        topBar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        topBar.addView(btnSearch);
        topBar.addView(btnRefresh);
        topBar.addView(btnLogin);
        root.addView(topBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        // 4. WebView bridge + Login modal container
        webContainer = new FrameLayout(this);
        webContainer.setBackgroundColor(Color.BLACK);
        webContainer.setVisibility(View.INVISIBLE);
        root.addView(webContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bridgeWebView = new WebView(this);
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(bridgeWebView, true);

        WebSettings s = bridgeWebView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setUserAgentString(MediaDownloadHelper.DESKTOP_UA);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        cachedJs = readAsset("douyin_lite.js");
        bridgeWebView.addJavascriptInterface(new Bridge(), "DouyinLiteBridge");
        bridgeWebView.setWebChromeClient(new WebChromeClient());
        bridgeWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return !(url.startsWith("http://") || url.startsWith("https://"));
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
        webContainer.addView(bridgeWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView closeWeb = makeTopPill("✕ 返回播放器");
        closeWeb.setOnClickListener(v -> toggleWebLogin(false));
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        closeLp.setMargins(0, dp(36), dp(14), 0);
        webContainer.addView(closeWeb, closeLp);

        bridgeWebView.loadUrl("https://www.douyin.com/jingxuan");
    }

    private void toggleWebLogin(boolean show) {
        webContainer.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        if (show) {
            player.pause();
        } else {
            CookieManager.getInstance().flush();
            if (adapter.getItemCount() == 0) {
                bridgeWebView.reload();
            } else {
                player.play();
            }
        }
    }

    private void attachAndPlay(int position) {
        VideoItem item = adapter.getItem(position);
        if (item == null) return;
        RecyclerView rv = (RecyclerView) viewPager.getChildAt(0);
        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
        if (vh instanceof FeedAdapter.VH holder) {
            holder.playerView.setPlayer(player);
            holder.pauseIcon.setVisibility(View.GONE);
        }
        player.setMediaItem(MediaItem.fromUri(item.videoUrl));
        player.setPlaybackSpeed(speed);
        player.prepare();
        player.play();
    }

    @Override
    public void onTogglePlay(FeedAdapter.VH holder) {
        if (player.isPlaying()) {
            player.pause();
            holder.pauseIcon.setVisibility(View.VISIBLE);
        } else {
            player.play();
            holder.pauseIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public void onToggleCleanScreen() {
        cleanScreen = !cleanScreen;
        adapter.setCleanScreen(cleanScreen);
    }

    @Override
    public void onCycleSpeed() {
        if (speed == 1.0f) speed = 1.25f;
        else if (speed == 1.25f) speed = 1.5f;
        else if (speed == 1.5f) speed = 2.0f;
        else speed = 1.0f;
        player.setPlaybackSpeed(speed);
        adapter.setSpeed(speed);
    }

    @Override
    public void onDownloadVideo(VideoItem item) {
        if (item.videoUrl.isEmpty()) {
            Toast.makeText(this, "未找到视频地址", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "开始保存视频...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                MediaDownloadHelper.downloadToDownloads(this, item.videoUrl, "douyin_" + item.awemeId + ".mp4", "video/mp4");
                runOnUiThread(() -> Toast.makeText(this, "已保存到 Download/DouyinLite/", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onDownloadAudio(VideoItem item) {
        String url = !item.audioUrl.isEmpty() ? item.audioUrl : item.videoUrl;
        if (url.isEmpty()) {
            Toast.makeText(this, "未找到音频地址", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "开始提取音频...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String ext = !item.audioUrl.isEmpty() ? ".mp3" : ".mp4";
                String mime = !item.audioUrl.isEmpty() ? "audio/mpeg" : "video/mp4";
                MediaDownloadHelper.downloadToDownloads(this, url, "audio_" + item.awemeId + ext, mime);
                runOnUiThread(() -> Toast.makeText(this, "音频已保存到 Download/DouyinLite/", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "提取失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onOpenComments(VideoItem item) {
        if (item.awemeId.isEmpty()) return;
        Toast.makeText(this, "加载评论中...", Toast.LENGTH_SHORT).show();
        String js = "window.__dylFetchComments && window.__dylFetchComments('" + item.awemeId + "')";
        bridgeWebView.evaluateJavascript(js, null);
    }

    @Override
    public void onLike(VideoItem item, TextView tv) {
        item.isDigged = !item.isDigged;
        item.diggCount += item.isDigged ? 1 : -1;
        adapter.notifyItemChanged(currentPos);
        String js = "window.__dylDigg && window.__dylDigg('" + item.awemeId + "'," + (item.isDigged ? 1 : 0) + ")";
        bridgeWebView.evaluateJavascript(js, null);
    }

    private void showSearchDialog() {
        EditText input = new EditText(this);
        input.setHint("输入关键词搜索视频...");
        new AlertDialog.Builder(this)
                .setTitle("搜索")
                .setView(input)
                .setPositiveButton("搜索", (d, w) -> {
                    String kw = input.getText().toString().trim();
                    if (!kw.isEmpty()) {
                        statusBanner.setText("搜索: " + kw);
                        String safe = kw.replace("'", "\\'");
                        bridgeWebView.evaluateJavascript("window.__dylSearch && window.__dylSearch('" + safe + "')", null);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private TextView makeTopPill(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(130, 20, 22, 28));
        bg.setCornerRadius(dp(16));
        bg.setStroke(1, Color.argb(45, 255, 255, 255));
        tv.setBackground(bg);
        tv.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(6);
        tv.setLayoutParams(lp);
        return tv;
    }

    private String readAsset(String name) {
        try (InputStream is = getAssets().open(name);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        if (player != null) player.release();
        if (bridgeWebView != null) bridgeWebView.destroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webContainer.getVisibility() == View.VISIBLE) {
            toggleWebLogin(false);
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBackMs < 1800) super.onBackPressed();
        else {
            lastBackMs = now;
            Toast.makeText(this, "再按一次退出 DouyinLite", Toast.LENGTH_SHORT).show();
        }
    }

    public class Bridge {
        @JavascriptInterface
        public void onFeedJson(String json, boolean replace) {
            List<VideoItem> parsed = VideoItem.parseFeedJson(json);
            if (parsed.isEmpty()) return;
            runOnUiThread(() -> {
                boolean wasEmpty = adapter.getItemCount() == 0;
                if (replace) {
                    adapter.replaceItems(parsed);
                    viewPager.setCurrentItem(0, false);
                    attachAndPlay(0);
                } else {
                    adapter.addItems(parsed);
                    if (wasEmpty) {
                        viewPager.post(() -> attachAndPlay(0));
                    }
                }
                statusBanner.setText("精选 · " + adapter.getItemCount() + " 条");
            });
        }

        @JavascriptInterface
        public void onCommentsJson(String json) {
            runOnUiThread(() -> {
                StringBuilder sb = new StringBuilder();
                try {
                    JSONObject root = new JSONObject(json);
                    JSONArray arr = root.optJSONArray("comments");
                    if (arr != null) {
                        for (int i = 0; i < Math.min(arr.length(), 25); i++) {
                            JSONObject c = arr.optJSONObject(i);
                            if (c == null) continue;
                            JSONObject u = c.optJSONObject("user");
                            String nick = u != null ? u.optString("nickname", "用户") : "用户";
                            sb.append(nick).append(": ").append(c.optString("text", "")).append("\n\n");
                        }
                    }
                } catch (Exception ignored) {}
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("评论区")
                        .setMessage(sb.length() > 0 ? sb.toString() : "暂无评论或需要登录后查看")
                        .setPositiveButton("关闭", null)
                        .show();
            });
        }

        @JavascriptInterface
        public void onStatus(String msg) {
            runOnUiThread(() -> statusBanner.setText(msg));
        }
    }
}
