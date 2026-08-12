package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.colorroulette.ColorCard;
import org.example.maniacrevolution.colorroulette.ColorRouletteManager;
import org.example.maniacrevolution.perk.perks.common.ColorRoulettePerk;

/** Client-only animation clock for the three-card carousel. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ColorRouletteClientHandler {
    private static ColorCard initial = ColorCard.RED;
    private static ColorCard result = ColorCard.RED;
    private static int ageTicks;
    private static int resultTicks;
    private static double phase;
    private static int previousStep;
    private static boolean rolling;
    private static boolean showingResult;
    private static boolean stopRequested;

    private ColorRouletteClientHandler() {}

    public static void start(ColorCard initialCenter) {
        initial = initialCenter;
        result = initialCenter;
        ageTicks = 0;
        resultTicks = 0;
        phase = 0.0D;
        previousStep = 0;
        rolling = true;
        showingResult = false;
        stopRequested = false;
    }

    public static void result(ColorCard selected) {
        result = selected;
        rolling = false;
        showingResult = true;
        resultTicks = 0;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.9F, 1.4F);
        }
    }

    public static void clear() {
        rolling = false;
        showingResult = false;
        stopRequested = false;
        ageTicks = 0;
        resultTicks = 0;
        phase = 0.0D;
    }

    public static boolean isVisible() {
        return rolling || showingResult;
    }

    public static boolean isRolling() {
        return rolling;
    }

    public static boolean canStop() {
        return rolling && !stopRequested && ageTicks >= ColorRoulettePerk.INTRO_TICKS;
    }

    public static void markStopRequested() {
        stopRequested = true;
    }

    public static float age(float partialTick) {
        return ageTicks + partialTick;
    }

    public static float resultAge(float partialTick) {
        return resultTicks + partialTick;
    }

    public static double phase(float partialTick) {
        if (!rolling) return phase;
        return phase + speedForAge(ageTicks) * partialTick;
    }

    public static ColorCard initial() {
        return initial;
    }

    public static ColorCard result() {
        return result;
    }

    public static ColorCard centeredCard() {
        int step = (int) Math.floor(phase + 0.5D);
        int offset = Math.floorMod(-step, ColorCard.values().length);
        return ColorCard.byOrdinal(initial.ordinal() + offset >= ColorCard.values().length
                ? initial.ordinal() + offset - ColorCard.values().length
                : initial.ordinal() + offset);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) return;
        if (rolling) {
            phase += speedForAge(ageTicks);
            ageTicks++;
            int step = (int) Math.floor(phase + 0.5D);
            if (step != previousStep) {
                previousStep = step;
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    float intro = Math.min(1.0F,
                            ageTicks / (float) ColorRoulettePerk.INTRO_TICKS);
                    minecraft.player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(),
                            SoundSource.PLAYERS, 0.22F + 0.22F * intro,
                            0.85F + 0.55F * intro);
                }
            }
        } else if (showingResult && ++resultTicks >= ColorRouletteManager.RESULT_ANIMATION_TICKS) {
            showingResult = false;
        }
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static double speedForAge(int age) {
        if (age >= ColorRoulettePerk.INTRO_TICKS) return 0.17D;
        double t = Math.max(0.0D, Math.min(1.0D,
                age / (double) ColorRoulettePerk.INTRO_TICKS));
        double eased = t * t * (3.0D - 2.0D * t);
        return 0.012D + 0.158D * eased;
    }
}
