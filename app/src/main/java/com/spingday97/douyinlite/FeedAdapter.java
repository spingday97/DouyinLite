package com.spingday97.douyinlite;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

@UnstableApi
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.VH> {
    public interface ActionListener {
        void onTogglePlay(VH holder);
        void onToggleCleanScreen();
        void onCycleSpeed();
        void onDownloadVideo(VideoItem item);
        void onDownloadAudio(VideoItem item);
        void onOpenComments(VideoItem item);
        void onLike(VideoItem item, TextView tv);
    }

    private final List<VideoItem> items = new ArrayList<>();
    private final ActionListener listener;
    private boolean cleanScreen = false;
    private float currentSpeed = 1.0f;

    public FeedAdapter(ActionListener listener) {
        this.listener = listener;
    }

    public void addItems(List<VideoItem> more) {
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public void replaceItems(List<VideoItem> fresh) {
        items.clear();
        items.addAll(fresh);
        notifyDataSetChanged();
    }

    public VideoItem getItem(int pos) {
        if (pos < 0 || pos >= items.size()) return null;
        return items.get(pos);
    }

    public void setCleanScreen(boolean clean) {
        this.cleanScreen = clean;
        notifyDataSetChanged();
    }

    public void setSpeed(float speed) {
        this.currentSpeed = speed;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        FrameLayout root = new FrameLayout(ctx);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.BLACK);

        PlayerView pv = new PlayerView(ctx);
        pv.setUseController(false);
        pv.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        pv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(pv);

        TextView pauseIcon = new TextView(ctx);
        pauseIcon.setText("▶");
        pauseIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        pauseIcon.setTextColor(Color.argb(180, 255, 255, 255));
        pauseIcon.setVisibility(View.GONE);
        FrameLayout.LayoutParams pLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.addView(pauseIcon, pLp);

        LinearLayout infoCol = new LinearLayout(ctx);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(ctx, 16);
        infoCol.setPadding(pad, pad, dp(ctx, 84), dp(ctx, 28));
        FrameLayout.LayoutParams infoLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        root.addView(infoCol, infoLp);

        TextView tvAuthor = new TextView(ctx);
        tvAuthor.setTextColor(Color.WHITE);
        tvAuthor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvAuthor.setShadowLayer(4, 0, 1, Color.BLACK);
        infoCol.addView(tvAuthor);

        TextView tvDesc = new TextView(ctx);
        tvDesc.setTextColor(Color.argb(235, 255, 255, 255));
        tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvDesc.setMaxLines(3);
        tvDesc.setEllipsize(TextUtils.TruncateAt.END);
        tvDesc.setPadding(0, dp(ctx, 6), 0, dp(ctx, 6));
        tvDesc.setShadowLayer(4, 0, 1, Color.BLACK);
        infoCol.addView(tvDesc);

        TextView tvMusic = new TextView(ctx);
        tvMusic.setTextColor(Color.argb(210, 255, 255, 255));
        tvMusic.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tvMusic.setSingleLine(true);
        tvMusic.setEllipsize(TextUtils.TruncateAt.END);
        tvMusic.setShadowLayer(4, 0, 1, Color.BLACK);
        infoCol.addView(tvMusic);

        LinearLayout actionsCol = new LinearLayout(ctx);
        actionsCol.setOrientation(LinearLayout.VERTICAL);
        actionsCol.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams actLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        actLp.setMargins(0, 0, dp(ctx, 12), dp(ctx, 28));
        root.addView(actionsCol, actLp);

        TextView btnLike = makeActionBtn(ctx);
        TextView btnComment = makeActionBtn(ctx);
        TextView btnSpeed = makeActionBtn(ctx);
        TextView btnClean = makeActionBtn(ctx);
        TextView btnDlVideo = makeActionBtn(ctx);
        TextView btnDlAudio = makeActionBtn(ctx);

        actionsCol.addView(btnLike);
        actionsCol.addView(btnComment);
        actionsCol.addView(btnSpeed);
        actionsCol.addView(btnClean);
        actionsCol.addView(btnDlVideo);
        actionsCol.addView(btnDlAudio);

        return new VH(root, pv, pauseIcon, infoCol, actionsCol,
                tvAuthor, tvDesc, tvMusic, btnLike, btnComment,
                btnSpeed, btnClean, btnDlVideo, btnDlAudio);
    }

    private TextView makeActionBtn(Context ctx) {
        TextView tv = new TextView(ctx);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(110, 20, 22, 28));
        bg.setCornerRadius(dp(ctx, 18));
        bg.setStroke(1, Color.argb(40, 255, 255, 255));
        tv.setBackground(bg);
        int h = dp(ctx, 10);
        int v = dp(ctx, 8);
        tv.setPadding(h, v, h, v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(ctx, 56), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 10);
        tv.setLayoutParams(lp);
        return tv;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VideoItem item = items.get(position);
        h.tvAuthor.setText("@" + item.authorName);
        h.tvDesc.setText(item.desc);
        h.tvMusic.setText(item.musicTitle.isEmpty() ? "♪ 原声" : "♪ " + item.musicTitle);

        h.btnLike.setText((item.isDigged ? "♥\n" : "♡\n") + formatCount(item.diggCount));
        h.btnLike.setTextColor(item.isDigged ? Color.parseColor("#FF4D6D") : Color.WHITE);
        h.btnComment.setText("评\n" + formatCount(item.commentCount));
        h.btnSpeed.setText(currentSpeed + "x");
        h.btnClean.setText(cleanScreen ? "还原" : "清屏");
        h.btnDlVideo.setText("存视频");
        h.btnDlAudio.setText("提音频");

        int vis = cleanScreen ? View.INVISIBLE : View.VISIBLE;
        h.infoCol.setVisibility(vis);
        h.btnLike.setVisibility(vis);
        h.btnComment.setVisibility(vis);
        h.btnSpeed.setVisibility(vis);
        h.btnDlVideo.setVisibility(vis);
        h.btnDlAudio.setVisibility(vis);

        h.itemView.setOnClickListener(v -> listener.onTogglePlay(h));
        h.btnLike.setOnClickListener(v -> listener.onLike(item, h.btnLike));
        h.btnComment.setOnClickListener(v -> listener.onOpenComments(item));
        h.btnSpeed.setOnClickListener(v -> listener.onCycleSpeed());
        h.btnClean.setOnClickListener(v -> listener.onToggleCleanScreen());
        h.btnDlVideo.setOnClickListener(v -> listener.onDownloadVideo(item));
        h.btnDlAudio.setOnClickListener(v -> listener.onDownloadAudio(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String formatCount(long c) {
        if (c >= 10000) return String.format("%.1fw", c / 10000.0);
        return String.valueOf(c);
    }

    private static int dp(Context ctx, int val) {
        return (int) (val * ctx.getResources().getDisplayMetrics().density);
    }

    public static class VH extends RecyclerView.ViewHolder {
        public final PlayerView playerView;
        public final TextView pauseIcon;
        public final LinearLayout infoCol;
        public final LinearLayout actionsCol;
        public final TextView tvAuthor, tvDesc, tvMusic;
        public final TextView btnLike, btnComment, btnSpeed, btnClean, btnDlVideo, btnDlAudio;

        public VH(@NonNull View itemView, PlayerView pv, TextView pauseIcon,
                  LinearLayout infoCol, LinearLayout actionsCol,
                  TextView tvAuthor, TextView tvDesc, TextView tvMusic,
                  TextView btnLike, TextView btnComment, TextView btnSpeed,
                  TextView btnClean, TextView btnDlVideo, TextView btnDlAudio) {
            super(itemView);
            this.playerView = pv;
            this.pauseIcon = pauseIcon;
            this.infoCol = infoCol;
            this.actionsCol = actionsCol;
            this.tvAuthor = tvAuthor;
            this.tvDesc = tvDesc;
            this.tvMusic = tvMusic;
            this.btnLike = btnLike;
            this.btnComment = btnComment;
            this.btnSpeed = btnSpeed;
            this.btnClean = btnClean;
            this.btnDlVideo = btnDlVideo;
            this.btnDlAudio = btnDlAudio;
        }
    }
}
