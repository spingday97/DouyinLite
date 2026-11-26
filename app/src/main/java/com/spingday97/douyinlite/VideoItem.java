package com.spingday97.douyinlite;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VideoItem {
    public String awemeId = "";
    public String desc = "";
    public String authorName = "";
    public String musicTitle = "";
    public String videoUrl = "";
    public String audioUrl = "";
    public long diggCount = 0;
    public long commentCount = 0;
    public boolean isDigged = false;

    public static List<VideoItem> parseFeedJson(String jsonStr) {
        List<VideoItem> list = new ArrayList<>();
        if (jsonStr == null || jsonStr.isEmpty()) return list;
        try {
            JSONObject root = new JSONObject(jsonStr);
            JSONArray arr = root.optJSONArray("aweme_list");
            if (arr == null) {
                JSONObject data = root.optJSONObject("data");
                if (data != null) arr = data.optJSONArray("aweme_list");
            }
            if (arr == null) return list;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) continue;
                JSONObject video = obj.optJSONObject("video");
                if (video == null) continue;

                VideoItem item = new VideoItem();
                item.awemeId = obj.optString("aweme_id", "");
                item.desc = obj.optString("desc", "");
                item.isDigged = obj.optInt("user_digged", 0) == 1;

                JSONObject author = obj.optJSONObject("author");
                if (author != null) {
                    item.authorName = author.optString("nickname", "");
                }

                JSONObject stats = obj.optJSONObject("statistics");
                if (stats != null) {
                    item.diggCount = stats.optLong("digg_count", 0);
                    item.commentCount = stats.optLong("comment_count", 0);
                }

                JSONObject playAddr = video.optJSONObject("play_addr");
                if (playAddr != null) {
                    JSONArray urls = playAddr.optJSONArray("url_list");
                    if (urls != null && urls.length() > 0) {
                        item.videoUrl = urls.optString(0, "");
                    }
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

                if (!item.videoUrl.isEmpty()) {
                    list.add(item);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }
}
