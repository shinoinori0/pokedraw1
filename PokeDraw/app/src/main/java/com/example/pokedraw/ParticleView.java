package com.example.pokedraw;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lightweight particle overlay drawn on Canvas.
 * Call startParticles(tier) to activate, stopParticles() to deactivate.
 */
public class ParticleView extends View {

    // ── Tweak these to adjust particle appearance ──────────────────────────────
    public static int   PARTICLE_COUNT       = 55;    // total particles on screen
    public static float SIZE_MIN_NORMAL      = 8f;    // min size for Shiny/Gold (dp-like)
    public static float SIZE_MAX_NORMAL      = 100f;   // max extra size for Shiny/Gold
    public static float SIZE_MIN_HOLO        = 12f;   // min size for Holo
    public static float SIZE_MAX_HOLO        = 14f;   // max extra size for Holo
    public static float SPEED_LATERAL        = 0.6f;  // side drift speed
    public static float SPEED_MIN_INWARD     = 0.3f;  // min inward speed
    public static float SPEED_MAX_INWARD     = 0.7f;  // max inward speed
    public static float ALPHA_MIN            = 0.75f; // starting opacity min
    public static float ALPHA_FADE_NORMAL    = 0.008f;// fade rate for Shiny/Gold
    public static float ALPHA_FADE_HOLO      = 0.006f;// fade rate for Holo
    // ──────────────────────────────────────────────────────────────────────────

    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng = new Random();

    private int activeTier = EvolutionChain.TIER_NORMAL;
    private boolean running = false;

    private static class Particle {
        float x, y, vx, vy, size, alpha;
        int color;
    }

    public ParticleView(Context context) { super(context); }
    public ParticleView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void startParticles(int tier) {
        activeTier = tier;
        running = tier > EvolutionChain.TIER_NORMAL;
        particles.clear();
        if (running) invalidate();
    }

    public void stopParticles() {
        running = false;
        particles.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!running || getWidth() == 0) return;

        // Spawn new particles to maintain pool
        while (particles.size() < PARTICLE_COUNT) particles.add(spawn());

        // Update and draw
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x     += p.vx;
            p.y     += p.vy;
            p.alpha -= activeTier == EvolutionChain.TIER_HOLO ? ALPHA_FADE_HOLO : ALPHA_FADE_NORMAL;
            if (p.alpha <= 0 || p.y < -p.size || p.y > getHeight() + p.size
                    || p.x < -p.size || p.x > getWidth() + p.size) {
                particles.set(i, spawn());
                continue;
            }
            paint.setColor(p.color);
            paint.setAlpha((int)(p.alpha * 255));
            canvas.drawCircle(p.x, p.y, p.size, paint);
        }

        postInvalidateOnAnimation();
    }

    private Particle spawn() {
        Particle p = new Particle();
        p.size  = activeTier == EvolutionChain.TIER_HOLO
                ? SIZE_MIN_HOLO   + rng.nextFloat() * SIZE_MAX_HOLO
                : SIZE_MIN_NORMAL + rng.nextFloat() * SIZE_MAX_NORMAL;
        p.alpha = ALPHA_MIN + rng.nextFloat() * (1f - ALPHA_MIN);
        p.color = particleColor();
        // Spawn from any edge
        int edge = rng.nextInt(4);
        switch (edge) {
            case 0: // bottom
                p.x  = rng.nextFloat() * getWidth();
                p.y  = getHeight() + p.size;
                p.vx = (rng.nextFloat() - 0.5f) * SPEED_LATERAL;
                p.vy = -(SPEED_MIN_INWARD + rng.nextFloat() * SPEED_MAX_INWARD);
                break;
            case 1: // top
                p.x  = rng.nextFloat() * getWidth();
                p.y  = -p.size;
                p.vx = (rng.nextFloat() - 0.5f) * SPEED_LATERAL;
                p.vy = SPEED_MIN_INWARD + rng.nextFloat() * SPEED_MAX_INWARD;
                break;
            case 2: // left
                p.x  = -p.size;
                p.y  = rng.nextFloat() * getHeight();
                p.vx = SPEED_MIN_INWARD + rng.nextFloat() * SPEED_MAX_INWARD;
                p.vy = (rng.nextFloat() - 0.5f) * SPEED_LATERAL;
                break;
            default: // right
                p.x  = getWidth() + p.size;
                p.y  = rng.nextFloat() * getHeight();
                p.vx = -(SPEED_MIN_INWARD + rng.nextFloat() * SPEED_MAX_INWARD);
                p.vy = (rng.nextFloat() - 0.5f) * SPEED_LATERAL;
                break;
        }
        return p;
    }

    private int particleColor() {
        switch (activeTier) {
            case EvolutionChain.TIER_SHINY:
                // Silver/white sparkles
                return rng.nextBoolean() ? Color.WHITE : Color.parseColor("#C0C0C0");
            case EvolutionChain.TIER_HOLO:
                // Cycling purple/blue/pink
                int[] holo = { 0xFFCF6FFF, 0xFF7B8CFF, 0xFFFF7BE8 };
                return holo[rng.nextInt(holo.length)];
            case EvolutionChain.TIER_GOLD:
                // Gold/amber/yellow
                int[] gold = { 0xFFFFB300, 0xFFFFE066, 0xFFFF8C00 };
                return gold[rng.nextInt(gold.length)];
            default:
                return Color.WHITE;
        }
    }
}
