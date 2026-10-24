package com.cleantv.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

/** One translucent "glass" plate that springs from target to target. */
public class GlassHighlight extends View {

    /** Damped spring: ~4% overshoot, settles well within the duration. */
    static final Interpolator SPRING = t -> (float) (1 - Math.exp(-8 * t) * Math.cos(8 * t));

    private final RectF cur = new RectF(), from = new RectF(), to = new RectF();
    private float curR, fromR, toR;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final LinearGradient sheen;
    private final Matrix sheenMatrix = new Matrix();
    private ValueAnimator anim;
    private boolean placed;

    public GlassHighlight(Context c, AttributeSet a) {
        super(c, a);
        sheen = new LinearGradient(0, 0, 0, 1, 0x3DFFFFFF, 0x0AFFFFFF, Shader.TileMode.CLAMP);
        fill.setShader(sheen);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(getResources().getDisplayMetrics().density * 1.5f);
        stroke.setColor(0x59FFFFFF);
        setWillNotDraw(false);
    }

    public void moveTo(RectF target, float radius, boolean animate) {
        to.set(target);
        toR = radius;
        if (anim != null) anim.cancel();
        if (!animate || !placed) {
            cur.set(to);
            curR = toR;
            placed = true;
            animate().alpha(1).setDuration(120).start();
            invalidate();
            return;
        }
        from.set(cur);
        fromR = curR;
        anim = ValueAnimator.ofFloat(0, 1);
        anim.setDuration(420);
        anim.setInterpolator(SPRING);
        anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            cur.set(lerp(from.left, to.left, f), lerp(from.top, to.top, f),
                    lerp(from.right, to.right, f), lerp(from.bottom, to.bottom, f));
            curR = lerp(fromR, toR, f);
            invalidate();
        });
        anim.start();
    }

    private static float lerp(float a, float b, float f) {
        return a + (b - a) * f;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (!placed || cur.isEmpty()) return;
        sheenMatrix.setScale(1, cur.height());
        sheenMatrix.postTranslate(0, cur.top);
        sheen.setLocalMatrix(sheenMatrix);
        c.drawRoundRect(cur, curR, curR, fill);
        c.drawRoundRect(cur, curR, curR, stroke);
    }
}
