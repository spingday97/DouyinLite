package com.spingday97.douyinlite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private static final String DOUYIN_HOME = "https://www.douyin.com/";

    private ViewPager2 viewPager;
    private FeedAdapter adapter;
    private ExoPlayer player;
    private WebView bridgeWebView;
    private FrameLayout webContainer;

    private TextView tabFollow, tabFriend, tabRecommend;
    private TextView navHome, navFriend, navMe;
    private int activeTopTab = 2; // 0=关注, 1=朋友, 2=推荐

    private String cachedJs;
    private int currentPos = 0;
    private boolean cleanScreen = false;
    private boolean autoNext = false;
    private float speed = 1.0f;
    private long lastBackMs = 0L;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressTick = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                long dur = player.getDuration();
                long pos = player.getCurrentPosition();
                if (dur > 0) {
                    RecyclerView rv = (RecyclerView) viewPager.getChildAt(0);
                    RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(currentPos);
                    if (vh instanceof FeedAdapter.VH holder) {
                        holder.progressBar.setProgress((int) (pos * 1000L / dur));
                    }
                }
            }
            uiHandler.postDelayed(this, 300);
        }
    };

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        w.setStatusBarColor(Color.BLACK);
        w.setNavigationBarColor(Color.BLACK);

        LinearLayout mainCol = new LinearLayout(this);
        mainCol.setOrientation(LinearLayout.VERTICAL);
        mainCol.setBackgroundColor(Color.BLACK);
        setContentView(mainCol);

        FrameLayout feedArea = new FrameLayout(this);
        mainCol.addView(feedArea, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 1. Native ExoPlayer
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", MediaDownloadHelper.DESKTOP_UA);
        headers.put("Referer", DOUYIN_HOME);
        DefaultHttpDataSource.Factory dsFactory = new DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(headers)
                .setAllowCrossProtocolRedirects(true);
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dsFactory))
                .build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED && autoNext) {
                    int next = currentPos + 1;
                    if (next < adapter.getItemCount()) {
                        viewPager.setCurrentItem(next, true);
                    }
                }
            }
        });

        // 2. Vertical ViewPager2
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
        feedArea.addView(viewPager, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 3. Douyin Top Bar (Refresh | 关注 朋友 推荐 | Search)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(14), dp(30), dp(14), dp(10));

        TextView btnRefresh = makeTopIcon("↻");
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "正在刷新推荐流...", Toast.LENGTH_SHORT).show();
            bridgeWebView.loadUrl(DOUYIN_HOME);
        });

        tabFollow = makeTopTab("关注");
        tabFriend = makeTopTab("朋友");
        tabRecommend = makeTopTab("推荐");
        tabFollow.setOnClickListener(v -> switchFeedTab(0));
        tabFriend.setOnClickListener(v -> switchFeedTab(1));
        tabRecommend.setOnClickListener(v -> switchFeedTab(2));
        updateTopTabs();

        TextView btnSearch = makeTopIcon("🔍");
        btnSearch.setOnClickListener(v -> showSearchDialog());

        topBar.addView(btnRefresh);
        View sp1 = new View(this);
        topBar.addView(sp1, new LinearLayout.LayoutParams(0, 1, 1f));
        topBar.addView(tabFollow);
        topBar.addView(tabFriend);
        topBar.addView(tabRecommend);
        View sp2 = new View(this);
        topBar.addView(sp2, new LinearLayout.LayoutParams(0, 1, 1f));
        topBar.addView(btnSearch);

        feedArea.addView(topBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        // 4. Douyin Bottom Navigation Bar (首页 | 朋友 | ⊕ | 登录 | 我)
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setBackgroundColor(Color.BLACK);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setPadding(0, dp(8), 0, dp(14));

        navHome = makeBottomNav("首页", true);
        navFriend = makeBottomNav("朋友", false);
        TextView navPlus = makeBottomNav("⊕", false);
        navPlus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        TextView navMsg = makeBottomNav("刷新", false);
        navMe = makeBottomNav("我", false);

        navHome.setOnClickListener(v -> switchFeedTab(2));
        navFriend.setOnClickListener(v -> switchFeedTab(1));
        navPlus.setOnClickListener(v -> bridgeWebView.evaluateJavascript("window.__dylLoadMore && window.__dylLoadMore()", null));
        navMsg.setOnClickListener(v -> {
            Toast.makeText(this, "加载新推荐...", Toast.LENGTH_SHORT).show();
            bridgeWebView.loadUrl(DOUYIN_HOME);
        });
        navMe.setOnClickListener(v -> toggleWebLogin(true));

        for (TextView tv : new TextView[]{navHome, navFriend, navPlus, navMsg, navMe}) {
            bottomBar.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        mainCol.addView(bottomBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 5. WebView bridge + Login modal overlay
        webContainer = new FrameLayout(this);
        webContainer.setBackgroundColor(Color.BLACK);
        webContainer.setVisibility(View.INVISIBLE);
        feedArea.addView(webContainer, new FrameLayout.LayoutParams(
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

        TextView closeWeb = new TextView(this);
        closeWeb.setText("✕ 完成并返回");
        closeWeb.setTextColor(Color.WHITE);
        closeWeb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(Color.argb(180, 254, 44, 85));
        cBg.setCornerRadius(dp(16));
        closeWeb.setBackground(cBg);
        closeWeb.setPadding(dp(12), dp(6), dp(12), dp(6));
        closeWeb.setOnClickListener(v -> toggleWebLogin(false));
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        closeLp.setMargins(0, dp(34), dp(14), 0);
        webContainer.addView(closeWeb, closeLp);

        bridgeWebView.loadUrl(DOUYIN_HOME);
        uiHandler.postDelayed(progressTick, 300);
    }

    private void switchFeedTab(int tab) {
        activeTopTab = tab;
        updateTopTabs();
        if (tab == 0) {
            bridgeWebView.loadUrl("https://www.douyin.com/follow");
        } else if (tab == 1) {
            bridgeWebView.evaluateJavascript("window.__dylSwitchTab && window.__dylSwitchTab('friend')", null);
        } else {
            bridgeWebView.loadUrl(DOUYIN_HOME);
        }
    }

    private void updateTopTabs() {
        TextView[] tabs = {tabFollow, tabFriend, tabRecommend};
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (i == activeTopTab);
            tabs[i].setTextColor(active ? Color.WHITE : Color.argb(165, 255, 255, 255));
            tabs[i].setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            tabs[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, active ? 17 : 15.5f);
        }
        if (navHome != null && navFriend != null) {
            navHome.setTextColor(activeTopTab == 2 ? Color.WHITE : Color.argb(150, 255, 255, 255));
            navFriend.setTextColor(activeTopTab == 1 ? Color.WHITE : Color.argb(150, 255, 255, 255));
        }
    }

    private void toggleWebLogin(boolean show) {
        webContainer.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        if (show) {
            player.pause();
        } else {
            CookieManager.getInstance().flush();
            if (adapter.getItemCount() == 0) {
                bridgeWebView.loadUrl(DOUYIN_HOME);
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
        player.setRepeatMode(autoNext ? Player.REPEAT_MODE_OFF : Player.REPEAT_MODE_ONE);
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
    public void onLike(VideoItem item, int pos) {
        item.isDigged = !item.isDigged;
        item.diggCount += item.isDigged ? 1 : -1;
        adapter.notifyItemChanged(pos);
        bridgeWebView.evaluateJavascript(
                "window.__dylDigg && window.__dylDigg('" + item.awemeId + "'," + (item.isDigged ? 1 : 0) + ")", null);
    }

    @Override
    public void onCollect(VideoItem item, int pos) {
        item.isCollected = !item.isCollected;
        item.collectCount += item.isCollected ? 1 : -1;
        adapter.notifyItemChanged(pos);
        bridgeWebView.evaluateJavascript(
                "window.__dylCollect && window.__dylCollect('" + item.awemeId + "'," + (item.isCollected ? 1 : 0) + ")", null);
    }

    @Override
    public void onOpenComments(VideoItem item) {
        if (item.awemeId.isEmpty()) return;
        Toast.makeText(this, "加载评论中...", Toast.LENGTH_SHORT).show();
        bridgeWebView.evaluateJavascript(
                "window.__dylFetchComments && window.__dylFetchComments('" + item.awemeId + "')", null);
    }

    @Override
    public void onOpenMoreSheet(VideoItem item) {
        String[] options = {
                "⬇ 保存视频到本地",
                "🎵 提取独立音频",
                "🔗 复制视频直链",
                "⚡ 倍速切换 (当前 " + speed + "x)",
                cleanScreen ? "👁 退出清屏" : "👁 清屏模式",
                autoNext ? "🔁 自动连播: 开" : "🔁 自动连播: 关",
                "👤 打开网页登录/验证"
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("更多功能")
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0 -> downloadVideo(item);
                        case 1 -> downloadAudio(item);
                        case 2 -> copyText(item.videoUrl);
                        case 3 -> cycleSpeed();
                        case 4 -> onToggleCleanScreen();
                        case 5 -> {
                            autoNext = !autoNext;
                            player.setRepeatMode(autoNext ? Player.REPEAT_MODE_OFF : Player.REPEAT_MODE_ONE);
                            Toast.makeText(this, "自动连播: " + (autoNext ? "开" : "关"), Toast.LENGTH_SHORT).show();
                        }
                        case 6 -> toggleWebLogin(true);
                    }
                })
                .show();
    }

    private void cycleSpeed() {
        if (speed == 1.0f) speed = 1.25f;
        else if (speed == 1.25f) speed = 1.5f;
        else if (speed == 1.5f) speed = 2.0f;
        else speed = 1.0f;
        player.setPlaybackSpeed(speed);
        Toast.makeText(this, "播放倍速: " + speed + "x", Toast.LENGTH_SHORT).show();
    }

    private void copyText(String text) {
        if (text == null || text.isEmpty()) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("DouyinLite", text));
            Toast.makeText(this, "已复制直链", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadVideo(VideoItem item) {
        if (item.videoUrl.isEmpty()) return;
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

    private void downloadAudio(VideoItem item) {
        String url = !item.audioUrl.isEmpty() ? item.audioUrl : item.videoUrl;
        if (url.isEmpty()) return;
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

    private void showSearchDialog() {
        EditText input = new EditText(this);
        input.setHint("输入关键词搜索视频...");
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("搜索")
                .setView(input)
                .setPositiveButton("搜索", (d, w) -> {
                    String kw = input.getText().toString().trim();
                    if (!kw.isEmpty()) {
                        String safe = kw.replace("'", "\\'");
                        bridgeWebView.evaluateJavascript("window.__dylSearch && window.__dylSearch('" + safe + "')", null);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private TextView makeTopIcon(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        tv.setPadding(dp(8), dp(4), dp(8), dp(4));
        return tv;
    }

    private TextView makeTopTab(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(dp(12), dp(4), dp(12), dp(4));
        return tv;
    }

    private TextView makeBottomNav(String text, boolean active) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTypeface(active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tv.setTextColor(active ? Color.WHITE : Color.argb(150, 255, 255, 255));
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
        uiHandler.removeCallbacks(progressTick);
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
                            sb.append(nick).append("：").append(c.optString("text", "")).append("\n\n");
                        }
                    }
                } catch (Exception ignored) {}
                new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle("评论区")
                        .setMessage(sb.length() > 0 ? sb.toString() : "暂无评论或需要登录后查看")
                        .setPositiveButton("关闭", null)
                        .show();
            });
        }
    }
}
