package com.spingday97.douyinlite;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
        void onLike(VideoItem item, int pos);
        void onOpenComments(VideoItem item);
        void onCollect(VideoItem item, int pos);
        void onOpenMoreSheet(VideoItem item);
        void onToggleCleanScreen();
    }

    private final List<VideoItem> items = new ArrayList<>();
    private final ActionListener listener;
    private boolean cleanScreen = false;

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
        pauseIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
        pauseIcon.setTextColor(Color.argb(175, 255, 255, 255));
        pauseIcon.setVisibility(View.GONE);
        root.addView(pauseIcon, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        // Bottom-left info column
        LinearLayout infoCol = new LinearLayout(ctx);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        infoCol.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 82), dp(ctx, 18));
        root.addView(infoCol, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        TextView tvAuthor = new TextView(ctx);
        tvAuthor.setTextColor(Color.WHITE);
        tvAuthor.setTypeface(Typeface.DEFAULT_BOLD);
        tvAuthor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvAuthor.setShadowLayer(4, 0, 1, Color.BLACK);
        infoCol.addView(tvAuthor);

        TextView tvDesc = new TextView(ctx);
        tvDesc.setTextColor(Color.argb(240, 255, 255, 255));
        tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvDesc.setMaxLines(3);
        tvDesc.setEllipsize(TextUtils.TruncateAt.END);
        tvDesc.setPadding(0, dp(ctx, 6), 0, dp(ctx, 6));
        tvDesc.setShadowLayer(4, 0, 1, Color.BLACK);
        infoCol.addView(tvDesc);

        TextView tvMusic = new TextView(ctx);
        tvMusic.setTextColor(Color.argb(220, 255, 255, 255));
        tvMusic.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvMusic.setSingleLine(true);
        tvMusic.setEllipsize(TextUtils.TruncateAt.END);
        tvMusic.setShadowLayer(4, 0, 1, Color.BLACK);
        infoCol.addView(tvMusic);

        // Right-side Douyin vertical action stack
        LinearLayout actionsCol = new LinearLayout(ctx);
        actionsCol.setOrientation(LinearLayout.VERTICAL);
        actionsCol.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams actLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        actLp.setMargins(0, 0, dp(ctx, 10), dp(ctx, 16));
        root.addView(actionsCol, actLp);

        // Author avatar circle
        TextView avatarBadge = new TextView(ctx);
        avatarBadge.setTextColor(Color.WHITE);
        avatarBadge.setTypeface(Typeface.DEFAULT_BOLD);
        avatarBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        avatarBadge.setGravity(Gravity.CENTER);
        GradientDrawable avBg = new GradientDrawable();
        avBg.setShape(GradientDrawable.OVAL);
        avBg.setColor(Color.parseColor("#2A2D36"));
        avBg.setStroke(dp(ctx, 2), Color.WHITE);
        avatarBadge.setBackground(avBg);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(ctx, 44), dp(ctx, 44));
        avLp.bottomMargin = dp(ctx, 14);
        avatarBadge.setLayoutParams(avLp);
        actionsCol.addView(avatarBadge);

        TextView btnLike = makeIconBtn(ctx, 28);
        TextView tvLikeCount = makeCountLabel(ctx);
        TextView btnComment = makeIconBtn(ctx, 26);
        TextView tvCommentCount = makeCountLabel(ctx);
        TextView btnCollect = makeIconBtn(ctx, 28);
        TextView tvCollectCount = makeCountLabel(ctx);
        TextView btnMore = makeIconBtn(ctx, 26);
        TextView tvMoreLabel = makeCountLabel(ctx);
        tvMoreLabel.setText("更多");

        // Spinning music disc
        TextView musicDisc = new TextView(ctx);
        musicDisc.setText("♪");
        musicDisc.setTextColor(Color.WHITE);
        musicDisc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        musicDisc.setGravity(Gravity.CENTER);
        GradientDrawable discBg = new GradientDrawable();
        discBg.setShape(GradientDrawable.OVAL);
        discBg.setColor(Color.parseColor("#18191E"));
        discBg.setStroke(dp(ctx, 5), Color.parseColor("#353740"));
        musicDisc.setBackground(discBg);
        LinearLayout.LayoutParams discLp = new LinearLayout.LayoutParams(dp(ctx, 38), dp(ctx, 38));
        discLp.topMargin = dp(ctx, 10);
        musicDisc.setLayoutParams(discLp);
        ObjectAnimator spin = ObjectAnimator.ofFloat(musicDisc, "rotation", 0f, 360f);
        spin.setDuration(5500);
        spin.setRepeatCount(ObjectAnimator.INFINITE);
        spin.setInterpolator(new LinearInterpolator());
        spin.start();

        actionsCol.addView(btnLike);
        actionsCol.addView(tvLikeCount);
        actionsCol.addView(btnComment);
        actionsCol.addView(tvCommentCount);
        actionsCol.addView(btnCollect);
        actionsCol.addView(tvCollectCount);
        actionsCol.addView(btnMore);
        actionsCol.addView(tvMoreLabel);
        actionsCol.addView(musicDisc);

        // Clean-screen restore button (visible only when cleanScreen == true)
        TextView btnRestore = new TextView(ctx);
        btnRestore.setText("退出清屏");
        btnRestore.setTextColor(Color.WHITE);
        btnRestore.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        GradientDrawable resBg = new GradientDrawable();
        resBg.setColor(Color.argb(140, 20, 22, 28));
        resBg.setCornerRadius(dp(ctx, 14));
        btnRestore.setBackground(resBg);
        btnRestore.setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 6));
        btnRestore.setVisibility(View.GONE);
        FrameLayout.LayoutParams resLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        resLp.setMargins(0, 0, dp(ctx, 14), dp(ctx, 18));
        root.addView(btnRestore, resLp);

        // Slim 2dp progress bar at bottom
        ProgressBar progressBar = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(1000);
        progressBar.setProgress(0);
        progressBar.getProgressDrawable().setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
        FrameLayout.LayoutParams pbLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 2), Gravity.BOTTOM);
        root.addView(progressBar, pbLp);

        return new VH(root, pv, pauseIcon, infoCol, actionsCol, avatarBadge,
                tvAuthor, tvDesc, tvMusic, btnLike, tvLikeCount,
                btnComment, tvCommentCount, btnCollect, tvCollectCount,
                btnMore, btnRestore, progressBar);
    }

    private TextView makeIconBtn(Context ctx, int sp) {
        TextView tv = new TextView(ctx);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setGravity(Gravity.CENTER);
        tv.setShadowLayer(6, 0, 2, Color.argb(160, 0, 0, 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(ctx, 46), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView makeCountLabel(Context ctx) {
        TextView tv = new TextView(ctx);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setGravity(Gravity.CENTER);
        tv.setShadowLayer(4, 0, 1, Color.BLACK);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VideoItem item = items.get(position);
        String nick = item.authorName.isEmpty() ? "抖" : item.authorName;
        h.avatarBadge.setText(nick.substring(0, 1));
        h.tvAuthor.setText("@" + nick);
        h.tvDesc.setText(item.desc);
        h.tvMusic.setText(item.musicTitle.isEmpty() ? "♪ 原声" : "♪ " + item.musicTitle);

        h.btnLike.setText("♥");
        h.btnLike.setTextColor(item.isDigged ? Color.parseColor("#FE2C55") : Color.WHITE);
        h.tvLikeCount.setText(formatCount(item.diggCount));

        h.btnComment.setText("💬");
        h.tvCommentCount.setText(formatCount(item.commentCount));

        h.btnCollect.setText("★");
        h.btnCollect.setTextColor(item.isCollected ? Color.parseColor("#FACE15") : Color.WHITE);
        h.tvCollectCount.setText(formatCount(item.collectCount));

        h.btnMore.setText("↗");

        int vis = cleanScreen ? View.GONE : View.VISIBLE;
        h.infoCol.setVisibility(vis);
        h.actionsCol.setVisibility(vis);
        h.btnRestore.setVisibility(cleanScreen ? View.VISIBLE : View.GONE);

        h.itemView.setOnClickListener(v -> listener.onTogglePlay(h));
        h.btnLike.setOnClickListener(v -> listener.onLike(item, h.getBindingAdapterPosition()));
        h.btnComment.setOnClickListener(v -> listener.onOpenComments(item));
        h.btnCollect.setOnClickListener(v -> listener.onCollect(item, h.getBindingAdapterPosition()));
        h.btnMore.setOnClickListener(v -> listener.onOpenMoreSheet(item));
        h.btnRestore.setOnClickListener(v -> listener.onToggleCleanScreen());
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
        public final LinearLayout infoCol, actionsCol;
        public final TextView avatarBadge, tvAuthor, tvDesc, tvMusic;
        public final TextView btnLike, tvLikeCount, btnComment, tvCommentCount;
        public final TextView btnCollect, tvCollectCount, btnMore, btnRestore;
        public final ProgressBar progressBar;

        public VH(@NonNull View itemView, PlayerView pv, TextView pauseIcon,
                  LinearLayout infoCol, LinearLayout actionsCol, TextView avatarBadge,
                  TextView tvAuthor, TextView tvDesc, TextView tvMusic,
                  TextView btnLike, TextView tvLikeCount, TextView btnComment, TextView tvCommentCount,
                  TextView btnCollect, TextView tvCollectCount, TextView btnMore,
                  TextView btnRestore, ProgressBar progressBar) {
            super(itemView);
            this.playerView = pv;
            this.pauseIcon = pauseIcon;
            this.infoCol = infoCol;
            this.actionsCol = actionsCol;
            this.avatarBadge = avatarBadge;
            this.tvAuthor = tvAuthor;
            this.tvDesc = tvDesc;
            this.tvMusic = tvMusic;
            this.btnLike = btnLike;
            this.tvLikeCount = tvLikeCount;
            this.btnComment = btnComment;
            this.tvCommentCount = tvCommentCount;
            this.btnCollect = btnCollect;
            this.tvCollectCount = tvCollectCount;
            this.btnMore = btnMore;
            this.btnRestore = btnRestore;
            this.progressBar = progressBar;
        }
    }
}
