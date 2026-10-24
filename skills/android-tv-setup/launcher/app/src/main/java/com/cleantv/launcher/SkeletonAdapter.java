package com.cleantv.launcher;

import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Placeholder tiles shown on cold start until the app list is loaded. */
final class SkeletonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** One animator drives every plate of every skeleton adapter, so they sweep in sync. */
    static final class Shimmer {
        private final List<ShimmerDrawable> plates = new ArrayList<>();
        private final ValueAnimator anim = ValueAnimator.ofFloat(0, 1);

        Shimmer() {
            anim.setDuration(1400);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.addUpdateListener(a -> {
                float p = (float) a.getAnimatedValue();
                for (int i = 0; i < plates.size(); i++) plates.get(i).setProgress(p);
            });
        }

        void start() {
            anim.start();
        }

        void stop() {
            anim.cancel();
            plates.clear();
        }
    }

    private final Shimmer shimmer;
    private final int count, cardHeight, gap;
    private final float radius;

    SkeletonAdapter(Shimmer shimmer, int count, int cardHeight, int gap, float radius) {
        this.shimmer = shimmer;
        this.count = count;
        this.cardHeight = cardHeight;
        this.gap = gap;
        this.radius = radius;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tile, parent, false);
        v.setPadding(gap / 2, 0, gap / 2, 0);
        View card = v.findViewById(R.id.card);
        card.getLayoutParams().height = cardHeight;
        ShimmerDrawable plate = new ShimmerDrawable(radius);
        card.setBackground(plate);
        shimmer.plates.add(plate);
        v.findViewById(R.id.label).setAlpha(0);
        return new RecyclerView.ViewHolder(v) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
    }

    @Override
    public int getItemCount() {
        return count;
    }
}
