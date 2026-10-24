package com.cleantv.launcher;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

final class TileAdapter extends RecyclerView.Adapter<TileAdapter.Holder> {

    static final class Holder extends RecyclerView.ViewHolder {
        final View card;
        final ImageView image;
        final TextView label;

        Holder(View v) {
            super(v);
            card = v.findViewById(R.id.card);
            image = v.findViewById(R.id.image);
            label = v.findViewById(R.id.label);
        }
    }

    private final List<AppRepository.App> apps;
    private final int cardHeight, gap;
    private final float radius;
    private final Typeface font;
    private final boolean fadeIn;

    /** fadeIn: the images appear on their plates, used once when the tiles replace the skeletons. */
    TileAdapter(List<AppRepository.App> apps, int cardHeight, int gap, float radius, Typeface font, boolean fadeIn) {
        this.apps = apps;
        this.cardHeight = cardHeight;
        this.gap = gap;
        this.radius = radius;
        this.font = font;
        this.fadeIn = fadeIn;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tile, parent, false);
        v.setPadding(gap / 2, 0, gap / 2, 0);
        Holder h = new Holder(v);
        h.card.getLayoutParams().height = cardHeight;
        h.card.setClipToOutline(true);
        h.label.setTypeface(font);
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        AppRepository.App a = apps.get(pos);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(radius);
        if (a.isBanner) {
            bg.setColor(0xFF1C1C1E);
            h.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            h.image.setPadding(0, 0, 0, 0);
        } else {
            float[] hsv = new float[3];
            Color.colorToHSV(a.color, hsv);
            hsv[2] *= 0.32f;
            bg.setColor(Color.HSVToColor(hsv));
            int p = (int) (cardHeight * 0.2f);
            h.image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            h.image.setPadding(p, p, p, p);
        }
        h.card.setBackground(bg);
        h.image.setImageDrawable(a.image);
        if (fadeIn) {
            h.image.setAlpha(0f);
            h.image.animate().alpha(1).setDuration(300).start();
        } else {
            h.image.setAlpha(1f);
        }
        h.label.setText(a.label);
        h.label.setAlpha(0);
        h.card.setScaleX(1);
        h.card.setScaleY(1);
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }
}
