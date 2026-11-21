package org.example.maniacrevolution.atmosphere.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.atmosphere.AtmosphereRules;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.WallhackGlowPacket;
import org.example.maniacrevolution.settings.ClientGameSettings;
import org.example.maniacrevolution.settings.GraphicsMode;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Actual world, vanilla entity outlines, viewer-local packets, fog cutoff and real effect synchronization. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereEffectsProbe {
    private record Case(GraphicsMode mode, boolean vanilla, boolean selective, boolean legacy, boolean blind, boolean dark) {}
    private static final Case[] CASES = {
            new Case(GraphicsMode.NORMAL, false, false, false, false, false),
            new Case(GraphicsMode.OFF, true, false, false, false, false),
            new Case(GraphicsMode.LIGHT, true, false, false, false, false),
            new Case(GraphicsMode.NORMAL, true, false, false, false, false),
            new Case(GraphicsMode.HEAVY, true, false, false, false, false),
            new Case(GraphicsMode.NORMAL, false, true, false, false, false),
            new Case(GraphicsMode.NORMAL, false, false, true, false, false),
            new Case(GraphicsMode.NORMAL, false, true, true, false, false),
            new Case(GraphicsMode.NORMAL, false, true, false, false, false),
            new Case(GraphicsMode.NORMAL, true, false, false, false, false),
            new Case(GraphicsMode.NORMAL, false, false, false, false, false),
            new Case(GraphicsMode.NORMAL, false, false, false, true, false),
            new Case(GraphicsMode.NORMAL, false, false, false, false, true)
    };
    private static boolean loading, ready, done;
    private static int ticks, step;
    private static long started;
    private static UUID target;
    private static String capture;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("maniacrev.atmosphere.effectsProbe") || done || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            if (!loading) {
                if (mc.screen == null || mc.getOverlay() != null) return;
                loading = true; started = System.nanoTime();
                mc.createWorldOpenFlows().createFreshLevel("atmosphere-effects-" + System.currentTimeMillis(),
                        new LevelSettings("Atmosphere effect checks", GameType.SURVIVAL, false,
                                Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(12345L, false, false), WorldPresets::createNormalWorldDimensions);
                return;
            }
            if (System.nanoTime() - started > TimeUnit.MINUTES.toNanos(4)) throw new AssertionError("Effects probe timed out");
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.player.tickCount < 30) return;
            if (mc.screen != null) mc.setScreen(null);
            if (mc.isPaused()) return;
            if (!ready) {
                mc.getSingleplayerServer().submit(() -> {
                    var server = mc.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                    var level = player.serverLevel();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    for (int x = -8; x <= 8; x++) for (int z = -2; z <= 38; z++)
                        level.setBlock(new BlockPos(x, 201, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    for (int x = -8; x <= 8; x++) for (int y = 202; y <= 207; y++)
                        level.setBlock(new BlockPos(x, y, 8), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    var stand = EntityType.ARMOR_STAND.create(level);
                    stand.setPos(.5, 202, 35.5); stand.setNoGravity(true); stand.setInvulnerable(true);
                    level.addFreshEntity(stand); target = stand.getUUID();
                    var team = server.getScoreboard().addPlayerTeam("outline_probe");
                    team.setColor(ChatFormatting.GREEN);
                    server.getScoreboard().addPlayerToTeam(stand.getScoreboardName(), team);
                    player.teleportTo(level, .5, 202, .5, 0, 0);
                    player.setGameMode(GameType.SURVIVAL);
                }).get(10, TimeUnit.SECONDS);
                mc.options.hideGui = true; mc.options.pauseOnLostFocus = false;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                ClientCommandHandler.runCommand("mrvisual preview survivor");
                ready = true; ticks = -60;
                return;
            }
            if (++ticks < 0) return;
            if (step >= CASES.length) {
                done = true;
                Maniacrev.LOGGER.info("Atmosphere effects probe PASSED: actual glowing behind wall at 35 blocks, OFF/LIGHT/NORMAL/HEAVY, selective/legacy overlap and removal, real blindness/darkness");
                mc.stop(); return;
            }
            var test = CASES[step];
            ClientGameSettings.setGraphicsMode(test.mode);
            if (ticks == 1) {
                mc.getSingleplayerServer().submit(() -> {
                    var server = mc.getSingleplayerServer();
                    var viewer = server.getPlayerList().getPlayer(mc.player.getUUID());
                    var stand = (net.minecraft.world.entity.LivingEntity) viewer.serverLevel().getEntity(target);
                    check(stand != null, "Target did not spawn");
                    stand.removeEffect(MobEffects.GLOWING);
                    stand.setGlowingTag(false);
                    SelectiveGlowingEffect.removeGlowing(stand, viewer);
                    ModNetworking.sendToPlayer(new WallhackGlowPacket(Set.of(stand.getId()), false), viewer);
                    if (test.vanilla) stand.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600));
                    if (test.selective) SelectiveGlowingEffect.addGlowing(stand, viewer, 600);
                    if (test.legacy) ModNetworking.sendToPlayer(new WallhackGlowPacket(Set.of(stand.getId()), true), viewer);
                    viewer.removeEffect(MobEffects.BLINDNESS); viewer.removeEffect(MobEffects.DARKNESS);
                    if (test.blind) viewer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 600));
                    if (test.dark) viewer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 600));
                }).get(10, TimeUnit.SECONDS);
            }
            if (ticks == 30) {
                if (test.mode.enabled()) check(AtmosphereClient.profile() == AtmosphereRules.Profile.SURVIVOR,
                        "An effect turned off the shader: " + test);
                check(VeilPostOrder.frames() > 0, "Post order mixin did not execute");
                capture = "effects-" + step;
            }
            if (ticks >= 35 && capture == null) { step++; ticks = 0; }
        } catch (Exception | AssertionError error) { fail(error); }
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!Boolean.getBoolean("maniacrev.atmosphere.effectsProbe") || capture == null || done || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(mc.gameDirectory.toPath().resolve(capture + ".png"));
            int green = 0;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y), r = pixel & 255, g = pixel >> 8 & 255, b = pixel >> 16 & 255;
                // Vanilla's small distant outline is antialiased and alpha blended; its peak is below the team's 255 green.
                if (g > 80 && g > r + 30 && g > b + 30) green++;
            }
            var test = CASES[step];
            boolean glowing = test.vanilla || test.selective || test.legacy;
            check(glowing ? green > 12 : green == 0, "Outline missing or leaked after removal: " + test + ", green pixels=" + green);
            if (!test.vanilla) {
                boolean serverGlow = mc.getSingleplayerServer().submit(() ->
                        mc.getSingleplayerServer().overworld().getEntity(target).hasGlowingTag()).get(5, TimeUnit.SECONDS);
                check(!serverGlow, "Viewer-local glow contaminated server metadata");
            }
            Maniacrev.LOGGER.info("Effects world case PASSED: {} {}, green pixels={}", step, test, green);
            capture = null;
        } catch (Exception | AssertionError error) { fail(error); }
    }

    private static void fail(Throwable error) {
        done = true;
        Maniacrev.LOGGER.error("Atmosphere effects probe FAILED", error);
        Minecraft.getInstance().stop();
    }
}
