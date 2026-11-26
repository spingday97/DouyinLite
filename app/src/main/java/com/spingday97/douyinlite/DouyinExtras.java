package com.spingday97.douyinlite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DouyinExtras {
    private static final String PREF = "dyl_prefs";
    private static final String KEY_HISTORY = "watch_history";

    public interface MeCallback {
        void onWebLogin();
        void onReloadFeed();
        void onFetchMyList(String kind);
        void onClearCache();
        void onPlayList(List<VideoItem> items);
    }

    public static void recordWatchHistory(Context ctx, VideoItem item) {
        if (item == null || item.awemeId == null || item.awemeId.isEmpty()) return;
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray oldArr = new JSONArray(sp.getString(KEY_HISTORY, "[]"));
            JSONArray newArr = new JSONArray();
            newArr.put(item.toHistoryJson());
            for (int i = 0; i < oldArr.length() && newArr.length() < 50; i++) {
                JSONObject o = oldArr.optJSONObject(i);
                if (o != null && !item.awemeId.equals(o.optString("awemeId"))) {
                    newArr.put(o);
                }
            }
            sp.edit().putString(KEY_HISTORY, newArr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<VideoItem> loadWatchHistory(Context ctx) {
        List<VideoItem> list = new ArrayList<>();
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString(KEY_HISTORY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                VideoItem v = VideoItem.fromHistoryJson(arr.optJSONObject(i));
                if (v != null) list.add(v);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void showMeMenuDialog(Activity act, MeCallback cb) {
        String[] items = {
                "网页登录抖音",
                "导入 Cookie (Header String)",
                "观看历史 (本地)",
                "我的喜欢",
                "我的收藏",
                "清理缓存"
        };
        new AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("我与设置")
                .setItems(items, (d, which) -> {
                    if (which == 0) cb.onWebLogin();
                    else if (which == 1) showImportCookieDialog(act, cb::onReloadFeed);
                    else if (which == 2) showVideoListDialog(act, "观看历史", loadWatchHistory(act), cb::onPlayList);
                    else if (which == 3) cb.onFetchMyList("favorite");
                    else if (which == 4) cb.onFetchMyList("collect");
                    else if (which == 5) cb.onClearCache();
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    public static void showImportCookieDialog(Activity act, Runnable onDone) {
        EditText input = new EditText(act);
        input.setHint("粘贴电脑端导出的 Header String Cookie...");
        input.setMinLines(4);
        new AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("导入 Cookie")
                .setView(input)
                .setPositiveButton("导入并刷新", (d, w) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) return;
                    CookieManager cm = CookieManager.getInstance();
                    cm.setAcceptCookie(true);
                    String[] parts = raw.split(";");
                    int count = 0;
                    for (String p : parts) {
                        String kv = p.trim();
                        if (kv.contains("=")) {
                            cm.setCookie("https://www.douyin.com", kv + "; Domain=.douyin.com; Path=/");
                            count++;
                        }
                    }
                    cm.flush();
                    Toast.makeText(act, "已写入 " + count + " 条 Cookie，正在刷新", Toast.LENGTH_SHORT).show();
                    if (onDone != null) onDone.run();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    public interface ListSelectCallback {
        void onSelect(List<VideoItem> reordered);
    }

    public static void showVideoListDialog(Activity act, String title, List<VideoItem> list, ListSelectCallback cb) {
        if (list == null || list.isEmpty()) {
            Toast.makeText(act, title + "暂无内容", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            VideoItem v = list.get(i);
            String tag = v.imageUrls.isEmpty() ? "[视频] " : "[图文] ";
            String desc = (v.desc == null || v.desc.isEmpty()) ? "(无标题)" : v.desc;
            if (desc.length() > 28) desc = desc.substring(0, 28) + "...";
            labels[i] = tag + "@" + v.authorName + ": " + desc;
        }
        new AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title + " (" + list.size() + ")")
                .setItems(labels, (d, which) -> {
                    List<VideoItem> reordered = new ArrayList<>();
                    for (int i = which; i < list.size(); i++) reordered.add(list.get(i));
                    for (int i = 0; i < which; i++) reordered.add(list.get(i));
                    if (cb != null) cb.onSelect(reordered);
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    public static String formatComments(String json) {
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
                    long digg = c.optLong("digg_count", 0);
                    String text = c.optString("text", "");
                    String extra = "";
                    if (c.optJSONArray("image_list") != null && c.optJSONArray("image_list").length() > 0) extra += " [图片]";
                    if (c.optJSONObject("sticker") != null) extra += " [表情]";
                    sb.append(nick).append(" [赞").append(digg).append("]: ").append(text).append(extra).append("\n\n");
                }
            }
        } catch (Exception ignored) {}
        return sb.length() > 0 ? sb.toString() : "暂无评论或需要登录后查看";
    }

    public static void clearDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) clearDir(f);
            else f.delete();
        }
    }
}
