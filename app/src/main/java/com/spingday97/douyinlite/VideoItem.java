package com.spingday97.douyinlite;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VideoItem {
    public String awemeId = "";
    public String desc = "";
    public String authorName = "";
    public String authorUid = "";
    public String authorSecUid = "";
    public String musicTitle = "";
    public String videoUrl = "";
    public String audioUrl = "";
    public List<String> imageUrls = new ArrayList<>();
    public long diggCount = 0;
    public long commentCount = 0;
    public long collectCount = 0;
    public boolean isDigged = false;
    public boolean isCollected = false;
    public boolean isFollowed = false;

    public static List<VideoItem> parseFeedJson(String jsonStr) {
        List<VideoItem> list = new ArrayList<>();
        if (jsonStr == null || jsonStr.isEmpty()) return list;
        try {
            JSONObject root = new JSONObject(jsonStr);
            JSONArray arr = root.optJSONArray("aweme_list");
            if (arr == null || arr.length() == 0) {
                JSONObject data = root.optJSONObject("data");
                if (data != null) arr = data.optJSONArray("aweme_list");
            }
            if (arr == null || arr.length() == 0) arr = root.optJSONArray("data");
            if (arr == null) return list;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) continue;
JSONObject awemeInfo = obj.optJSONObject("aweme_info");
               if (awemeInfo != null) obj = awemeInfo; 
                VideoItem item = new VideoItem();
                item.awemeId = obj.optString("aweme_id", "");
                item.desc = obj.optString("desc", "");
                item.isDigged = obj.optInt("user_digged", 0) == 1;
                item.isCollected = obj.optInt("collect_stat", 0) == 1;

                JSONObject author = obj.optJSONObject("author");
                if (author != null) {
                    item.authorName = author.optString("nickname", "");
                    item.authorUid = author.optString("uid", "");
                    item.authorSecUid = author.optString("sec_uid", "");
                    item.isFollowed = author.optInt("follow_status", 0) > 0;
                }

                JSONObject stats = obj.optJSONObject("statistics");
                if (stats != null) {
                    item.diggCount = stats.optLong("digg_count", 0);
                    item.commentCount = stats.optLong("comment_count", 0);
                    item.collectCount = stats.optLong("collect_count", 0);
                }

                JSONArray images = obj.optJSONArray("images");
                if (images != null) {
                    for (int j = 0; j < images.length(); j++) {
                        JSONObject imgObj = images.optJSONObject(j);
                        if (imgObj == null) continue;
                        JSONArray uList = imgObj.optJSONArray("url_list");
                    if (uList == null || uList.length() == 0) {
                        uList = imgObj.optJSONArray("download_url_list");
                    }
                        if (uList != null && uList.length() > 0) {
                            String imgUrl = uList.optString(0, "");
                            if (!imgUrl.isEmpty()) item.imageUrls.add(imgUrl);
                        }
                    }
                }

                JSONObject video = obj.optJSONObject("video");
                if (video != null) {
                    JSONObject playAddr = video.optJSONObject("play_addr");
                    if (playAddr != null) {
                        JSONArray urls = playAddr.optJSONArray("url_list");
                        if (urls != null && urls.length() > 0) {
                            item.videoUrl = pickBestUrl(urls); // item.videoUrl = urls.optString(0, "");
                        }
                    }
               JSONArray br = item.videoUrl.isEmpty() ? video.optJSONArray("bit_rate") : null;
               JSONObject b0 = (br != null && br.length() > 0) ? br.optJSONObject(0) : null;
               JSONObject bpa = b0 != null ? b0.optJSONObject("play_addr") : null;
               if (bpa != null) item.videoUrl = pickBestUrl(bpa.optJSONArray("url_list"));
                }

                JSONObject music = obj.optJSONObject("music");
                if (music != null) {
                    item.musicTitle = music.optString("title", "");
                    JSONObject mPlay = music.optJSONObject("play_url");
                    if (mPlay != null) {
                        JSONArray mUrls = mPlay.optJSONArray("url_list");
                        if (mUrls != null && mUrls.length() > 0) {
                            item.audioUrl = mUrls.optString(0, "");
                        }
                    }
                }

                if (item.videoUrl.isEmpty() && !item.audioUrl.isEmpty()) {
                    item.videoUrl = item.audioUrl;
                }

                if (!item.videoUrl.isEmpty() || !item.imageUrls.isEmpty()) {
                    list.add(item);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private static String pickBestUrl(JSONArray urls) {
       if (urls == null || urls.length() == 0) return "";
       for (int i = 0; i < urls.length(); i++) if (urls.optString(i, "").startsWith("https://")) return urls.optString(i, "");
       return urls.optString(0, "");
    }
    public JSONObject toHistoryJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("awemeId", awemeId);
            o.put("desc", desc);
            o.put("authorName", authorName);
            o.put("authorUid", authorUid);
            o.put("authorSecUid", authorSecUid);
            o.put("musicTitle", musicTitle);
            o.put("videoUrl", videoUrl);
            o.put("audioUrl", audioUrl);
            JSONArray imgs = new JSONArray();
            for (String u : imageUrls) {
                imgs.put(u);
            }
            o.put("imageUrls", imgs);
        } catch (Exception ignored) {
        }
        return o;
    }

    public static VideoItem fromHistoryJson(JSONObject o) {
        VideoItem item = new VideoItem();
        if (o == null) return item;
        item.awemeId = o.optString("awemeId", "");
        item.desc = o.optString("desc", "");
        item.authorName = o.optString("authorName", "");
        item.authorUid = o.optString("authorUid", "");
        item.authorSecUid = o.optString("authorSecUid", "");
        item.musicTitle = o.optString("musicTitle", "");
        item.videoUrl = o.optString("videoUrl", "");
        item.audioUrl = o.optString("audioUrl", "");
        JSONArray imgs = o.optJSONArray("imageUrls");
        if (imgs != null) {
            for (int i = 0; i < imgs.length(); i++) {
                String u = imgs.optString(i, "");
                if (!u.isEmpty()) item.imageUrls.add(u);
            }
        }
        return item;
    }
}
