package dev.cataclysm.arsenal;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lightweight screen-space cinematic telemetry for the Solar Lance.
 * No HDR targets, framebuffer allocation, shader packs or GL extensions.
 * All coordinates scale with the active Minecraft GUI resolution.
 */
public final class StrikeCinematicHud {
    private static final List<Sequence> ACTIVE = new ArrayList<>();
    private static final long DURATION_NANOS = 8_000_000_000L;
    private static final int WHITE = 0xFFE8F6FF;
    private static final int CYAN = 0xFF64E6FF;
    private static final int RED = 0xFFFF626F;

    private StrikeCinematicHud() {}

    public static void begin(Vec3d target) {
        if (ACTIVE.size() >= 2) ACTIVE.remove(0);
        ACTIVE.add(new Sequence(System.nanoTime(), target));
    }

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> render(context));
    }

    private static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) {
            ACTIVE.clear();
            return;
        }
        long now = System.nanoTime();
        Iterator<Sequence> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().start > DURATION_NANOS) iterator.remove();
        }
        if (ACTIVE.isEmpty()) return;
        Sequence s = ACTIVE.get(ACTIVE.size() - 1);
        double t = (now - s.start) / 1_000_000_000.0;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        if (w < 240 || h < 150) return;
        int strip = Math.max(13, h / 18);
        ctx.fill(0, 0, w, strip, 0xD900070F);
        ctx.fill(0, h - strip, w, h, 0xD900070F);
        ctx.fill(0, strip, 2, h - strip, 0xCC28BBD8);
        ctx.fill(w - 2, strip, w, h - strip, 0xCC28BBD8);

        String phase;
        int accent;
        if (t < 0.9) { phase = "TARGET LOCK // ACQUIRED"; accent = CYAN; }
        else if (t < 2.7) { phase = "HELIOS PLATFORM // REACTOR ONLINE"; accent = CYAN; }
        else if (t < 3.2) { phase = "FIRING SOLAR LANCE"; accent = WHITE; }
        else if (t < 4.7) { phase = "IMPACT CONFIRMED"; accent = RED; }
        else { phase = "THERMAL AFTERSHOCK // STABILIZING"; accent = RED; }

        ctx.drawTextWithShadow(mc.textRenderer, Text.literal("C A T A C L Y S M   //   H E L I O S"), 10, 3, WHITE);
        int px = Math.max(10, (w - mc.textRenderer.getWidth(phase)) / 2);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(phase), px, h - strip + 2, accent);
        String coords = String.format(java.util.Locale.ROOT, "X %.0f   Y %.0f   Z %.0f", s.target.x, s.target.y, s.target.z);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(coords), 10, strip + 9, CYAN);

        // Charge / firing progress meter.
        int barWidth = Math.min(160, w / 3);
        int bx = w - barWidth - 12, by = strip + 12;
        double fraction = Math.max(0, Math.min(1, t / 3.2));
        ctx.fill(bx, by, bx + barWidth, by + 3, 0x99304755);
        ctx.fill(bx, by, bx + (int)(barWidth * fraction), by + 3, accent);

        // Reticle only during target acquisition; never blocks player controls.
        if (t < 2.7) {
            int cx = w / 2, cy = h / 2;
            int r = (int)(18 + Math.sin(t * 7) * 2);
            ctx.fill(cx - r - 12, cy, cx - r, cy + 1, CYAN);
            ctx.fill(cx + r, cy, cx + r + 12, cy + 1, CYAN);
            ctx.fill(cx, cy - r - 12, cx + 1, cy - r, CYAN);
            ctx.fill(cx, cy + r, cx + 1, cy + r + 12, CYAN);
        }
        if (t >= 3.2 && t < 3.5) {
            int a = (int)(Math.max(0, 1 - (t - 3.2) / 0.3) * 100);
            ctx.fill(0, strip, w, h - strip, (a << 24) | 0x00FFD3BC);
        }
    }

    private record Sequence(long start, Vec3d target) {}
}
