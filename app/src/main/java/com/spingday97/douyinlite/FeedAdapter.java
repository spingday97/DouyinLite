package com.spingday97.douyinlite;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UnstableApi
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.VH> {
    public interface ActionListener {
        void onTogglePlay(VH holder);
        void onLike(VideoItem item, int pos);
        void onOpenComments(VideoItem item);
        void onCollect(VideoItem item, int pos);
        void onToggleCleanScreen();
        void onOpenMoreSheet(VideoItem item);
    }

    private static final ExecutorService IMG_EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final List<VideoItem> items = new ArrayList<>();
    private final ActionListener listener;
    private boolean cleanScreen = false;
    
    public FeedAdapter(ActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<VideoItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void updateItemAt(int pos) {
        if (pos >= 0 && pos < items.size()) notifyItemChanged(pos);
    }
    public VideoItem getItem(int pos) { return (pos >= 0 && pos < items.size()) ? items.get(pos) : null; }
    public void setCleanScreen(boolean clean) { this.cleanScreen = clean; notifyDataSetChanged(); }
    public void replaceItems(List<VideoItem> list) { setItems(list); }
    public void addItems(List<VideoItem> list) { if (list == null || list.isEmpty()) return; int start = items.size(); items.addAll(list); notifyItemRangeInserted(start, list.size()); }
    
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        FrameLayout root = new FrameLayout(ctx);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.BLACK);

        PlayerView pv = new PlayerView(ctx);
        pv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        pv.setUseController(false);
        pv.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        pv.setKeepScreenOn(true);
        root.addView(pv);

        ViewPager2 imagePager = new ViewPager2(ctx);
        imagePager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        imagePager.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imagePager.setVisibility(View.GONE);
        root.addView(imagePager);

        TextView imageBadge = new TextView(ctx);
        imageBadge.setTextColor(Color.WHITE);
        imageBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        imageBadge.setPadding(dp(ctx, 10), dp(ctx, 4), dp(ctx, 10), dp(ctx, 4));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0x88000000);
        badgeBg.setCornerRadius(dp(ctx, 12));
        imageBadge.setBackground(badgeBg);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.gravity = Gravity.TOP | Gravity.END;
        badgeLp.topMargin = dp(ctx, 56);
        badgeLp.rightMargin = dp(ctx, 16);
        imageBadge.setLayoutParams(badgeLp);
        imageBadge.setVisibility(View.GONE);
        root.addView(imageBadge);

        TextView pauseIcon = new TextView(ctx);
        pauseIcon.setText("▶");
        pauseIcon.setTextColor(0xAAFFFFFF);
        pauseIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 52);
        pauseIcon.setVisibility(View.GONE);
        FrameLayout.LayoutParams pauseLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(pauseIcon, pauseLp);

        // Right action bar
        LinearLayout rightBar = new LinearLayout(ctx);
        rightBar.setOrientation(LinearLayout.VERTICAL);
        rightBar.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams rbLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rbLp.gravity = Gravity.BOTTOM | Gravity.END;
        rbLp.rightMargin = dp(ctx, 12);
        rbLp.bottomMargin = dp(ctx, 80);
        rightBar.setLayoutParams(rbLp);

        // Author avatar + follow badge
        FrameLayout avatarBox = new FrameLayout(ctx);
        LinearLayout.LayoutParams avBoxLp = new LinearLayout.LayoutParams(dp(ctx, 48), dp(ctx, 56));
        avBoxLp.bottomMargin = dp(ctx, 16);
        avatarBox.setLayoutParams(avBoxLp);

        TextView avatarCircle = new TextView(ctx);
        avatarCircle.setGravity(Gravity.CENTER);
        avatarCircle.setTextColor(Color.WHITE);
        avatarCircle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        avatarCircle.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable avBg = new GradientDrawable();
        avBg.setShape(GradientDrawable.OVAL);
        avBg.setColor(0xFF333333);
        avBg.setStroke(dp(ctx, 2), Color.WHITE);
        avatarCircle.setBackground(avBg);
        FrameLayout.LayoutParams avLp = new FrameLayout.LayoutParams(dp(ctx, 46), dp(ctx, 46), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        avatarBox.addView(avatarCircle, avLp);

        TextView followBadge = new TextView(ctx);
        followBadge.setText("+");
        followBadge.setGravity(Gravity.CENTER);
        followBadge.setTextColor(Color.WHITE);
        followBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        followBadge.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable fbBg = new GradientDrawable();
        fbBg.setShape(GradientDrawable.OVAL);
        fbBg.setColor(0xFFFE2C55);
        followBadge.setBackground(fbBg);
        FrameLayout.LayoutParams fbLp = new FrameLayout.LayoutParams(dp(ctx, 20), dp(ctx, 20), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        avatarBox.addView(followBadge, fbLp);
        rightBar.addView(avatarBox);

        TextView likeIcon = makeActionIcon(ctx, "♥");
        TextView likeCount = makeActionLabel(ctx);
        rightBar.addView(likeIcon);
        rightBar.addView(likeCount);

        TextView commentIcon = makeActionIcon(ctx, "💬");
        TextView commentCount = makeActionLabel(ctx);
        rightBar.addView(commentIcon);
        rightBar.addView(commentCount);

        TextView collectIcon = makeActionIcon(ctx, "★");
        TextView collectCount = makeActionLabel(ctx);
        rightBar.addView(collectIcon);
        rightBar.addView(collectCount);

        TextView shareIcon = makeActionIcon(ctx, "↗");
        TextView shareLabel = makeActionLabel(ctx);
        shareLabel.setText("分享");
        rightBar.addView(shareIcon);
        rightBar.addView(shareLabel);

        // Spinning music disc
        TextView discView = new TextView(ctx);
        discView.setText("♫");
        discView.setGravity(Gravity.CENTER);
        discView.setTextColor(Color.WHITE);
        discView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        GradientDrawable discBg = new GradientDrawable();
        discBg.setShape(GradientDrawable.OVAL);
        discBg.setColor(0xFF222222);
        discBg.setStroke(dp(ctx, 6), 0xFF444444);
        discView.setBackground(discBg);
        LinearLayout.LayoutParams discLp = new LinearLayout.LayoutParams(dp(ctx, 42), dp(ctx, 42));
        discLp.topMargin = dp(ctx, 10);
        rightBar.addView(discView, discLp);

        root.addView(rightBar);

        // Bottom left info
        LinearLayout bottomBar = new LinearLayout(ctx);
        bottomBar.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams bbLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bbLp.gravity = Gravity.BOTTOM | Gravity.START;
        bbLp.leftMargin = dp(ctx, 14);
        bbLp.rightMargin = dp(ctx, 84);
        bbLp.bottomMargin = dp(ctx, 24);
        bottomBar.setLayoutParams(bbLp);

        TextView authorTv = new TextView(ctx);
        authorTv.setTextColor(Color.WHITE);
        authorTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        authorTv.setTypeface(Typeface.DEFAULT_BOLD);
        authorTv.setShadowLayer(4, 1, 1, 0xAA000000);
        bottomBar.addView(authorTv);

        TextView descTv = new TextView(ctx);
        descTv.setTextColor(0xEEFFFFFF);
        descTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        descTv.setMaxLines(3);
        descTv.setEllipsize(TextUtils.TruncateAt.END);
        descTv.setShadowLayer(4, 1, 1, 0xAA000000);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descLp.topMargin = dp(ctx, 6);
        bottomBar.addView(descTv, descLp);

        TextView musicTv = new TextView(ctx);
        musicTv.setTextColor(0xCCFFFFFF);
        musicTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        musicTv.setSingleLine(true);
        musicTv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        musicTv.setSelected(true);
        LinearLayout.LayoutParams musicLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        musicLp.topMargin = dp(ctx, 6);
        bottomBar.addView(musicTv, musicLp);

        root.addView(bottomBar);

        // Progress bar at very bottom
        ProgressBar seekBar = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        seekBar.setMax(1000);
        seekBar.setProgress(0);
        FrameLayout.LayoutParams sbLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 2), Gravity.BOTTOM);
        sbLp.bottomMargin = dp(ctx, 2);
        root.addView(seekBar, sbLp);

        return new VH(root, pv, imagePager, imageBadge, pauseIcon, avatarCircle, followBadge,
                likeIcon, likeCount, commentIcon, commentCount, collectIcon, collectCount,
                shareIcon, discView, authorTv, descTv, musicTv, seekBar, rightBar, bottomBar);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VideoItem item = items.get(position);
        h.boundPosition = position;

        if (!item.imageUrls.isEmpty()) {
            h.playerView.setVisibility(View.GONE);
            h.imagePager.setVisibility(View.VISIBLE);
            h.imageBadge.setVisibility(View.VISIBLE);
            int total = item.imageUrls.size();
            h.imageBadge.setText("图文 1/" + total);
            h.imagePager.setAdapter(new ImagePagerAdapter(item.imageUrls));
            if (h.pageChangeCallback != null) {
                h.imagePager.unregisterOnPageChangeCallback(h.pageChangeCallback);
            }
            h.pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int pos) {
                    h.imageBadge.setText("图文 " + (pos + 1) + "/" + total);
                }
            };
            h.imagePager.registerOnPageChangeCallback(h.pageChangeCallback);
        } else {
            h.playerView.setVisibility(View.VISIBLE);
            h.imagePager.setVisibility(View.GONE);
            h.imageBadge.setVisibility(View.GONE);
            if (h.pageChangeCallback != null) {
                h.imagePager.unregisterOnPageChangeCallback(h.pageChangeCallback);
                h.pageChangeCallback = null;
            }
        }

        h.authorTv.setText("@" + (item.authorName.isEmpty() ? "抖音用户" : item.authorName));
        h.descTv.setText(item.desc);
        h.musicTv.setText("🎵 " + (item.musicTitle.isEmpty() ? "原声" : item.musicTitle));
        h.avatarCircle.setText(item.authorName.isEmpty() ? "抖" : item.authorName.substring(0, 1));
        h.followBadge.setVisibility(item.isFollowed ? View.GONE : View.VISIBLE);

        h.likeIcon.setTextColor(item.isDigged ? 0xFFFE2C55 : Color.WHITE);
        h.likeCount.setText(formatCount(item.diggCount));
        h.commentCount.setText(formatCount(item.commentCount));
        h.collectIcon.setTextColor(item.isCollected ? 0xFFFFC107 : Color.WHITE);
        h.collectCount.setText(formatCount(item.collectCount));

        h.pauseIcon.setVisibility(View.GONE);
        h.seekBar.setProgress(0);
        int uiVis = cleanScreen ? View.GONE : View.VISIBLE;
        h.rightBar.setVisibility(uiVis);
        h.bottomBar.setVisibility(uiVis);
        
        h.itemView.setOnClickListener(v -> listener.onTogglePlay(h));
        h.itemView.setOnLongClickListener(v -> { listener.onToggleCleanScreen(); return true; });
        h.likeIcon.setOnClickListener(v -> listener.onLike(item, h.getBindingAdapterPosition()));
        h.commentIcon.setOnClickListener(v -> listener.onOpenComments(item));
        h.collectIcon.setOnClickListener(v -> listener.onCollect(item, h.getBindingAdapterPosition()));
        h.shareIcon.setOnClickListener(v -> listener.onOpenMoreSheet(item));

        if (h.discAnim == null) {
            h.discAnim = ObjectAnimator.ofFloat(h.discView, "rotation", 0f, 360f);
            h.discAnim.setDuration(4000);
            h.discAnim.setRepeatCount(ObjectAnimator.INFINITE);
            h.discAnim.setInterpolator(new LinearInterpolator());
            h.discAnim.start();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static TextView makeActionIcon(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        tv.setGravity(Gravity.CENTER);
        tv.setShadowLayer(4, 1, 1, 0xAA000000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(ctx, 48), dp(ctx, 36));
        lp.topMargin = dp(ctx, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static TextView makeActionLabel(Context ctx) {
        TextView tv = new TextView(ctx);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setGravity(Gravity.CENTER);
        tv.setShadowLayer(3, 1, 1, 0xAA000000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static String formatCount(long count) {
        if (count <= 0) return "0";
        if (count >= 10000) return String.format("%.1fw", count / 10000.0);
        return String.valueOf(count);
    }

    private static int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    private static class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImgVH> {
        private final List<String> urls;

        ImagePagerAdapter(List<String> urls) {
            this.urls = urls;
        }

        @NonNull
        @Override
        public ImgVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setBackgroundColor(Color.BLACK);
            return new ImgVH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull ImgVH holder, int position) {
            String url = urls.get(position);
            holder.imageView.setImageBitmap(null);
            holder.imageView.setTag(url);
            IMG_EXECUTOR.execute(() -> {
                Bitmap bmp = null;
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setInstanceFollowRedirects(true);
                    conn.connect();
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        try (InputStream is = conn.getInputStream()) {
                            bmp = BitmapFactory.decodeStream(is);
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (conn != null) conn.disconnect();
                }
                Bitmap finalBmp = bmp;
                MAIN_HANDLER.post(() -> {
                    if (url.equals(holder.imageView.getTag()) && finalBmp != null) {
                        holder.imageView.setImageBitmap(finalBmp);
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        static class ImgVH extends RecyclerView.ViewHolder {
            final ImageView imageView;

            ImgVH(@NonNull ImageView iv) {
                super(iv);
                this.imageView = iv;
            }
        }
    }

    public static class VH extends RecyclerView.ViewHolder {
        public final PlayerView playerView;
        public final ViewPager2 imagePager;
        public final TextView imageBadge;
        public final TextView pauseIcon;
        public final TextView avatarCircle;
        public final TextView followBadge;
        public final TextView likeIcon;
        public final TextView likeCount;
        public final TextView commentIcon;
        public final TextView commentCount;
        public final TextView collectIcon;
        public final TextView collectCount;
        public final TextView shareIcon;
        public final TextView discView;
        public final TextView authorTv;
        public final TextView descTv;
        public final TextView musicTv;
        public final ProgressBar seekBar;
        public ObjectAnimator discAnim; public final ProgressBar progressBar; public final LinearLayout rightBar; public final LinearLayout bottomBar;
        public ViewPager2.OnPageChangeCallback pageChangeCallback;
        public int boundPosition = -1;

        public VH(@NonNull View itemView, PlayerView playerView, ViewPager2 imagePager, TextView imageBadge,
                  TextView pauseIcon, TextView avatarCircle, TextView followBadge,
                  TextView likeIcon, TextView likeCount, TextView commentIcon, TextView commentCount,
                  TextView collectIcon, TextView collectCount, TextView shareIcon, TextView discView,
                  TextView authorTv, TextView descTv, TextView musicTv, ProgressBar seekBar, LinearLayout rightBar, LinearLayout bottomBar) {
            super(itemView);
            this.playerView = playerView;
            this.imagePager = imagePager;
            this.imageBadge = imageBadge;
            this.pauseIcon = pauseIcon;
            this.avatarCircle = avatarCircle;
            this.followBadge = followBadge;
            this.likeIcon = likeIcon;
            this.likeCount = likeCount;
            this.commentIcon = commentIcon;
            this.commentCount = commentCount;
            this.collectIcon = collectIcon;
            this.collectCount = collectCount;
            this.shareIcon = shareIcon;
            this.discView = discView;
            this.authorTv = authorTv;
            this.descTv = descTv;
            this.musicTv = musicTv;
            this.seekBar = seekBar; this.progressBar = seekBar; this.rightBar = rightBar; this.bottomBar = bottomBar;
        }
    }
}
