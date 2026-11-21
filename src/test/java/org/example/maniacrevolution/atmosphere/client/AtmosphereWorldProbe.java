package org.example.maniacrevolution.atmosphere.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.item.NightmareLighterItem;
import org.example.maniacrevolution.settings.ClientGameSettings;
import org.example.maniacrevolution.settings.GraphicsMode;

import java.util.concurrent.TimeUnit;

/** Real player, block discovery, movement, NBT synchronization and use-item packets in an isolated world. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereWorldProbe {
    private static final BlockPos EMITTER = new BlockPos(0, 202, 24);
    private static final BlockPos RED_EMITTER = new BlockPos(-2, 201, 24);
    private static final BlockPos CYAN_EMITTER = new BlockPos(2, 201, 24);
    private static final BlockPos WALL_EMITTER = new BlockPos(5, 202, 21);
    private static final BlockPos OUTER_EMITTER = new BlockPos(2, 202, 30);
    private static final GraphicsMode[] MODES = {GraphicsMode.LIGHT, GraphicsMode.NORMAL, GraphicsMode.HEAVY};
    private static boolean loading, done;
    private static int stage, ticks, mode;
    private static String capture;
    private static long started;
    private static long cachedScans;
    private static final BlockPos EDITED_EMITTER = new BlockPos(-1, 202, 16);

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static InteractionHand hand() { return mode == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND; }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("maniacrev.atmosphere.worldProbe") || done || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            if (!loading) {
                if (mc.screen == null || mc.getOverlay() != null) return;
                started = System.nanoTime();
                loading = true;
                mc.createWorldOpenFlows().createFreshLevel("atmosphere-lighting-" + System.currentTimeMillis(),
                        new LevelSettings("Atmosphere light checks", GameType.SURVIVAL, false,
                                Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(12345L, false, false), WorldPresets::createNormalWorldDimensions);
                return;
            }
            if (System.nanoTime() - started > TimeUnit.MINUTES.toNanos(4)) throw new AssertionError("World probe timed out");
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.player.tickCount < 30) return;
            if (mc.screen != null) mc.setScreen(null);
            // Tick events can run while a loading/menu frame still pauses the world.
            if (mc.isPaused()) return;
            ticks++;
            ClientGameSettings.setGraphicsMode(stage < 2 || stage >= 6 ? GraphicsMode.HEAVY : MODES[mode]);
            if (stage == 0) {
                mc.getSingleplayerServer().submit(() -> {
                    var server = mc.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                    var level = player.serverLevel();
                    level.setDayTime(18000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    for (int x = -6; x <= 6; x++) for (int z = -3; z <= 31; z++) {
                        for (int y = 199; y <= 206; y++) {
                            boolean boundary = y == 199 || y == 206 || x == -6 || x == 6 || z == -3 || z == 31;
                            level.setBlock(new BlockPos(x, y, z), (boundary ? Blocks.STONE_BRICKS : Blocks.AIR).defaultBlockState(), 3);
                        }
                    }
                    // Side ledges reproduce near/far depth discontinuities without blocking the central source.
                    for (int z = 4; z <= 16; z += 3) for (int y = 200; y <= 202; y++)
                        level.setBlock(new BlockPos(-3, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    level.setBlock(EMITTER, Blocks.GLOWSTONE.defaultBlockState(), 3);
                    level.setBlock(OUTER_EMITTER, Blocks.SEA_LANTERN.defaultBlockState(), 3);
                    // A ceiling tile crossing the four-block grouping grid must retain all four parts as one fixture.
                    for (int x = 3; x <= 4; x++) for (int z = 7; z <= 8; z++)
                        level.setBlock(new BlockPos(x, 206, z), Blocks.GLOWSTONE.defaultBlockState(), 3);
                    for (int z = 4; z <= 14; z += 2) for (int x : new int[]{-4, 4}) {
                        level.setBlock(new BlockPos(x, 200, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                        level.setBlock(new BlockPos(x, 201, z), Blocks.TORCH.defaultBlockState(), 3);
                    }
                    level.setBlock(RED_EMITTER.below(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    level.setBlock(RED_EMITTER, Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
                    level.setBlock(CYAN_EMITTER.below(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    level.setBlock(CYAN_EMITTER, Blocks.SOUL_LANTERN.defaultBlockState(), 3);
                    level.setBlock(WALL_EMITTER, Blocks.REDSTONE_WALL_TORCH.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, net.minecraft.core.Direction.WEST), 3);
                    player.setGameMode(GameType.SURVIVAL);
                    player.getInventory().clearContent();
                    player.getInventory().setItem(0, new ItemStack(ModItems.NIGHTMARE_LIGHTER.get()));
                    player.getInventory().selected = 0;
                    player.teleportTo(level, .5, 200, .5, 0, 0);
                    player.inventoryMenu.broadcastChanges();
                    return null;
                }).get(15, TimeUnit.SECONDS);
                stage = 1; ticks = 0;
                return;
            }
            if (stage == 1) {
                if (!mc.level.getBlockState(EMITTER).is(Blocks.GLOWSTONE)) { ticks = 0; return; }
                if (ticks < 35 && mc.player.position().distanceToSqr(new net.minecraft.world.phys.Vec3(.5, 200, .5)) > .25) {
                    ticks = 0; return;
                }
                if (ticks == 1) ClientCommandHandler.runCommand("mrvisual preview survivor");
                if (ticks < 35) return;
                check(AtmosphereClient.profile() != org.example.maniacrevolution.atmosphere.AtmosphereRules.Profile.OFF, "Preview inactive");
                check(AtmosphereLights.strengthAt(EMITTER) > .8F, "24-block source missing: player=" + mc.player.position()
                        + ", groups=" + AtmosphereLights.groupCount() + ", sections=" + AtmosphereLights.cachedSections()
                        + ", scans=" + AtmosphereLights.sectionScans() + ", paused=" + mc.isPaused()
                        + ", state=" + mc.level.getBlockState(EMITTER));
                for (BlockPos emitter : new BlockPos[]{RED_EMITTER, CYAN_EMITTER, WALL_EMITTER}) {
                    check(AtmosphereLights.strengthAt(emitter) > .99F, "Crowded-scene source has delayed ignition: " + emitter);
                    check(AtmosphereLights.uploaded(emitter), "Near lights evict distant colored source: " + emitter);
                }
                check(AtmosphereLights.uploaded(OUTER_EMITTER), "Emitter beyond 28 blocks was culled despite overlapping visible space");
                for (int x = 3; x <= 4; x++) for (int z = 7; z <= 8; z++) {
                    BlockPos tile = new BlockPos(x, 206, z);
                    check(AtmosphereLights.rowSizeAt(tile) == 1 && AtmosphereLights.fixtureNeighborsAt(tile) == 3,
                            "2x2 ceiling fixture lost neighbor/diagonal self-shadow exclusion at a group boundary: " + tile);
                }
                if (ticks == 45 || ticks == 65) {
                    double z = ticks == 45 ? 2.5 : .5;
                    mc.getSingleplayerServer().submit(() -> {
                        var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                        p.teleportTo(p.serverLevel(), .5, 200, z, 0, 0);
                    }).get(10, TimeUnit.SECONDS);
                }
                if (ticks < 85) return;
                Maniacrev.LOGGER.info("Atmosphere world range PASSED: distant red/cyan/wall emitters survive twelve near torches, rescans and movement");
                stage = 6; ticks = 0; return;
            }
            if (stage == 6) {
                if (ticks == 1) cachedScans = AtmosphereLights.sectionScans();
                if (ticks == 15) {
                    check(AtmosphereLights.sectionScans() == cachedScans, "Unchanged nearby sections are still being rescanned");
                    Maniacrev.LOGGER.info("Atmosphere stationary cache PASSED: {} scans unchanged across 14 ticks, {} indexed sections",
                            cachedScans, AtmosphereLights.cachedSections());
                    mc.getSingleplayerServer().submit(() -> {
                        var level = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
                        level.setBlock(EDITED_EMITTER, Blocks.GLOWSTONE.defaultBlockState(), 3);
                    }).get(10, TimeUnit.SECONDS);
                }
                if (ticks == 30) {
                    check(AtmosphereLights.uploaded(EDITED_EMITTER), "New source was not indexed after a block packet");
                    check(AtmosphereLights.sectionScans() > cachedScans, "Block-change cache invalidation hook did not run");
                    check(AtmosphereLights.sectionScans() <= cachedScans + 2, "Block edit rescanned unrelated sections");
                    mc.getSingleplayerServer().submit(() -> {
                        var level = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
                        level.setBlock(EDITED_EMITTER, Blocks.AIR.defaultBlockState(), 3);
                        for (int x = -4; x < 4; x++) level.setBlock(new BlockPos(x, 204, 8), Blocks.GLOWSTONE.defaultBlockState(), 3);
                    }).get(10, TimeUnit.SECONDS);
                }
                if (ticks == 60) {
                    check(AtmosphereLights.strengthAt(EDITED_EMITTER) == 0, "Removed source persists in the cache");
                    for (int x = -4; x < 4; x++) {
                        BlockPos block = new BlockPos(x, 204, 8);
                        check(AtmosphereLights.rowSizeAt(block) == 4, "Contiguous row was not merged into stable four-block groups");
                        check(AtmosphereLights.uploaded(block), "Row endpoint was omitted from the shader");
                    }
                    mc.getSingleplayerServer().submit(() -> {
                        var level = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
                        for (int x = -5; x <= 5; x++) for (int y = 200; y <= 205; y++)
                            level.setBlock(new BlockPos(x, y, 18), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }).get(10, TimeUnit.SECONDS);
                }
                if (ticks == 80) {
                    check(AtmosphereLights.strengthAt(EMITTER) == 1, "Occlusion deleted a cached light");
                    check(!AtmosphereLights.uploaded(EMITTER), "Fully enclosed far source leaks through the wall");
                    mc.getSingleplayerServer().submit(() -> {
                        var level = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).serverLevel();
                        for (int x = -5; x <= 5; x++) for (int y = 200; y <= 205; y++)
                            level.setBlock(new BlockPos(x, y, 18), Blocks.AIR.defaultBlockState(), 3);
                    }).get(10, TimeUnit.SECONDS);
                }
                if (ticks < 100) return;
                check(AtmosphereLights.uploaded(EMITTER) && AtmosphereLights.strengthAt(EMITTER) == 1,
                        "Removing the wall did not restore the light immediately");
                mc.getSingleplayerServer().submit(() -> {
                    var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    p.setNoGravity(true);
                    p.teleportTo(p.serverLevel(), .5, 200, -65.5, 0, 0);
                }).get(10, TimeUnit.SECONDS);
                stage = 7; ticks = 0; return;
            }
            if (stage == 7) {
                if (ticks == 25) mc.getSingleplayerServer().submit(() -> {
                    var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    p.teleportTo(p.serverLevel(), .5, 200, .5, 0, 0);
                    p.fallDistance = 0;
                    p.setNoGravity(false);
                }).get(10, TimeUnit.SECONDS);
                if (ticks < 50) return;
                check(AtmosphereLights.strengthAt(EMITTER) == 1 && AtmosphereLights.uploaded(EMITTER),
                        "Returning to an indexed area causes delayed ignition");
                check(AtmosphereLights.rowSizeAt(new BlockPos(-4, 204, 8)) == 4, "Returning changed row grouping");
                Maniacrev.LOGGER.info("Atmosphere cache world PASSED: no stationary rescans, block packet invalidation, rows, wall occlusion and return");
                stage = 2; ticks = 0;
            }
            if (stage == 2) {
                mc.getSingleplayerServer().submit(() -> {
                    var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    p.getInventory().clearContent();
                    p.setItemInHand(hand(), new ItemStack(ModItems.NIGHTMARE_LIGHTER.get()));
                    p.inventoryMenu.broadcastChanges();
                    return null;
                }).get(10, TimeUnit.SECONDS);
                stage = 3; ticks = 0; return;
            }
            if (ticks < 15) return;
            if (stage == 3) {
                check(!NightmareLighterItem.isLit(mc.player.getItemInHand(hand())), "Closed lighter Lit tag");
                check(AtmosphereLights.handLightCount() == 0, "Closed lighter uploads a light in " + MODES[mode]);
                mc.gameMode.useItem(mc.player, hand());
                stage = 4; ticks = 0; return;
            }
            if (stage == 4) {
                check(NightmareLighterItem.isLit(mc.player.getItemInHand(hand())), "Use-item Lit state did not sync");
                check(AtmosphereLights.handLightCount() == 1, "Burning lighter missing from " + MODES[mode]);
                if (ticks >= 30) {
                    check(AtmosphereLights.handStrength() > .98F, "Held light decays in " + MODES[mode]);
                    var light = mc.level.getBlockState(mc.player.blockPosition().above());
                    check(light.is(Blocks.LIGHT) && light.getValue(net.minecraft.world.level.block.LightBlock.LEVEL) == 12,
                            "Lighter has no full vanilla block light in " + MODES[mode]);
                    check(AtmosphereLights.sourceCount() >= 10, "Block sources missing in " + MODES[mode]);
                    check(AtmosphereLights.strengthAt(mc.player.blockPosition().above()) == 0,
                            "Moving LIGHT block is counted again as a fog source in " + MODES[mode]);
                }
                if (ticks == 35) capture = "atmosphere-world-lighter-" + MODES[mode].name().toLowerCase(java.util.Locale.ROOT);
                if (ticks < 60) return;
                mc.gameMode.useItem(mc.player, hand());
                stage = 5; ticks = 0; return;
            }
            if (stage == 5) {
                check(!NightmareLighterItem.isLit(mc.player.getItemInHand(hand())), "Close-item Lit state did not sync");
                check(AtmosphereLights.handLightCount() == 0, "Closed lighter retains shader illumination");
                check(!mc.level.getBlockState(mc.player.blockPosition().above()).is(Blocks.LIGHT),
                        "Closed lighter leaves its vanilla light block behind");
                Maniacrev.LOGGER.info("Atmosphere world lighter PASSED: {} {}, closed/open/held/closed", MODES[mode], hand());
                if (++mode < MODES.length) { stage = 2; ticks = 0; return; }
                done = true;
                Maniacrev.LOGGER.info("Atmosphere world probe PASSED: crowded colored emitters at 24 blocks, moving rescans, Lit NBT, both hands, three-quality light");
                mc.stop();
            }
        } catch (Exception | AssertionError error) {
            done = true;
            Maniacrev.LOGGER.error("Atmosphere world probe FAILED", error);
            mc.stop();
        }
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!Boolean.getBoolean("maniacrev.atmosphere.worldProbe") || capture == null || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(mc.gameDirectory.toPath().resolve(capture + ".png"));
        } catch (Exception error) { Maniacrev.LOGGER.error("Atmosphere world screenshot failed", error); }
        capture = null;
    }
}
