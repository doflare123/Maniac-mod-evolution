package org.example.maniacrevolution.forester;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.*;
import org.example.maniacrevolution.*;
import org.example.maniacrevolution.block.ModBlocks;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.effect.ModEffects;
import java.util.*;

@GameTestHolder("forester_tests")
@PrefixGameTestTemplate(false)
public final class ForesterGameTests {
    private static final class Scenario implements AutoCloseable {
        final GameTestHelper helper;
        final List<ServerPlayer> players = new ArrayList<>();
        final BlockPos trap;
        final ServerPlayer forester, maniac, ally;
        Scenario(GameTestHelper helper) {
            this.helper = helper; trap = helper.absolutePos(new BlockPos(5, 2, 5));
            helper.getLevel().setBlockAndUpdate(trap.below(), Blocks.STONE.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(trap, ModBlocks.FORESTER_TRAP.get().defaultBlockState());
            forester = player("survivors", 5, 2, 2);
            maniac = player("maniac", 5, 2, 8);
            ally = player("survivors", 8, 2, 5);
            PlayerDataManager.get(forester).setSurvivorClassId(ForesterRules.CLASS_ID);
        }
        ServerPlayer player(String team, int x, int y, int z) {
            var p = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "fr_" + UUID.randomUUID().toString().substring(0, 8))) {
                @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
                @Override public boolean canHarmPlayer(net.minecraft.world.entity.player.Player target) { return true; }
            };
            var pos = helper.absolutePos(new BlockPos(x, y, z)); p.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            p.setGameMode(GameType.SURVIVAL);
            var board = helper.getLevel().getScoreboard(); var t = board.getPlayerTeam(team);
            if (t == null) t = board.addPlayerTeam(team);
            board.addPlayerToTeam(p.getScoreboardName(), t); helper.getLevel().addNewPlayer(p); players.add(p);
            return p;
        }
        void tick(ServerPlayer p) { ForesterManager.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p)); }
        void aim() {
            double dx = trap.getX() + .5 - forester.getX(), dz = trap.getZ() + .5 - forester.getZ();
            forester.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
            forester.setXRot((float) Math.toDegrees(Math.atan2(forester.getEyeY() - (trap.getY() + .2), Math.sqrt(dx * dx + dz * dz))));
        }
        void spent() { helper.getLevel().setBlockAndUpdate(trap, ModBlocks.FORESTER_TRAP.get().defaultBlockState().setValue(ForesterTrapBlock.STATE, ForesterTrapBlock.State.SPENT)); }
        @Override public void close() {
            for (var p : players) {
                ForesterManager.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
                helper.getLevel().getScoreboard().removePlayerFromTeam(p.getScoreboardName()); p.discard();
            }
            helper.getLevel().removeBlock(trap, false);
        }
    }
    @GameTest(template="scp173_empty", timeoutTicks=40)
    public static void captureRolesAndDamage(GameTestHelper h) {
        try (var s = new Scenario(h)) {
            h.assertTrue(ForesterManager.capture(s.forester, s.trap), "Owner is vulnerable to own trap");
            ForesterManager.damage(new LivingHurtEvent(s.forester, s.forester.damageSources().fall(), 2));
            h.assertTrue(ForesterManager.trapped(s.forester) && !s.forester.hasEffect(ModEffects.OPEN_WOUND.get()), "Environmental damage neither releases nor bleeds");
            ForesterManager.damage(new LivingHurtEvent(s.forester, s.forester.damageSources().playerAttack(s.maniac), 1));
            h.assertTrue(!ForesterManager.trapped(s.forester) && s.forester.hasEffect(ModEffects.OPEN_WOUND.get()), "Player hit releases and applies existing blood trail");
            h.assertTrue(h.getLevel().getBlockState(s.trap).getValue(ForesterTrapBlock.STATE) == ForesterTrapBlock.State.SPENT, "Released trap is spent");
            h.getLevel().setBlockAndUpdate(s.trap, ModBlocks.FORESTER_TRAP.get().defaultBlockState());
            h.assertTrue(ForesterManager.capture(s.maniac, s.trap), "Maniac is vulnerable too");
            ForesterManager.damage(new LivingHurtEvent(s.maniac, s.maniac.damageSources().playerAttack(s.ally), 1));
            h.assertTrue(!ForesterManager.trapped(s.maniac), "Survivor hit also releases maniac");
            s.ally.setGameMode(GameType.CREATIVE);
            h.getLevel().setBlockAndUpdate(s.trap, ModBlocks.FORESTER_TRAP.get().defaultBlockState());
            h.assertTrue(!ForesterManager.capture(s.ally, s.trap), "Creative stays unaffected");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty", timeoutTicks=180)
    public static void gameAndTimeout(GameTestHelper h) {
        var s = new Scenario(h); ForesterManager.capture(s.maniac, s.trap);
        var initial = ForesterManager.state(s.maniac);
        ForesterManager.space(s.maniac, initial.sequence(), 30);
        h.assertTrue(ForesterManager.state(s.maniac).successes() == 0, "Client cannot skip initial delay");
        h.runAfterDelay(30, () -> {
            try {
                var snapshot = ForesterManager.state(s.maniac);
                h.assertTrue(snapshot.age() >= 30 && snapshot.age() < 40, "Server tracks delay");
                ForesterManager.space(s.maniac, snapshot.sequence() + 900, snapshot.age());
                h.assertTrue(ForesterManager.state(s.maniac).successes() == 0, "Wrong session token rejected");
            } catch (Throwable failure) { s.close(); throw failure; }
        });
        h.runAfterDelay(101, () -> {
            try {
                s.tick(s.maniac);
                h.assertTrue(ForesterManager.trapped(s.maniac), "Still trapped after five seconds total");
            } catch (Throwable failure) { s.close(); throw failure; }
        });
        h.runAfterDelay(131, () -> {
            try (s) {
                s.tick(s.maniac);
                h.assertTrue(!ForesterManager.trapped(s.maniac) && !s.maniac.hasEffect(ModEffects.OPEN_WOUND.get()), "Timeout releases without bleeding after delay plus five seconds");
            }
            h.succeed();
        });
    }
    @GameTest(template="scp173_empty", timeoutTicks=180)
    public static void successfulPressesAndMisses(GameTestHelper h) {
        var s = new Scenario(h); ForesterManager.capture(s.maniac, s.trap);
        final int[] stage = {0}, last = {-100};
        h.onEachTick(() -> {
            if (stage[0] == 4) return;
            var snap = ForesterManager.state(s.maniac);
            if (snap.age() < 31 || snap.age() - last[0] < 8) return;
            boolean correct = ForesterRules.hit(snap.age(), snap.target());
            try {
                if (stage[0] == 0 && correct) {
                    ForesterManager.space(s.maniac, snap.sequence(), snap.age()); last[0] = snap.age(); stage[0] = 1;
                    h.assertTrue(ForesterManager.state(s.maniac).successes() == 1, "First hit counts");
                } else if (stage[0] == 1 && !correct) {
                    ForesterManager.space(s.maniac, snap.sequence(), snap.age()); last[0] = snap.age(); stage[0] = 2;
                    h.assertTrue(ForesterManager.state(s.maniac).successes() == 1, "Miss preserves first hit");
                } else if (stage[0] == 2 && correct) {
                    ForesterManager.space(s.maniac, snap.sequence(), snap.age()); stage[0] = 4;
                    h.assertTrue(!ForesterManager.trapped(s.maniac) && !s.maniac.hasEffect(ModEffects.OPEN_WOUND.get()), "Two hits release without bleeding");
                    s.close(); h.succeed();
                }
            } catch (Throwable failure) { stage[0] = 4; s.close(); throw failure; }
        });
    }
    @GameTest(template="scp173_empty", timeoutTicks=120)
    public static void rearmAndCollect(GameTestHelper h) {
        var s = new Scenario(h); s.spent(); s.aim();
        h.assertTrue(ForesterManager.aimedAt(s.forester, s.trap), "Ray hits real trap shape");
        ForesterManager.workInput(s.ally, s.trap, true);
        h.assertTrue(ForesterManager.state(s.ally).work() == -1, "Only forester may interact");
        ForesterManager.workInput(s.forester, s.trap, false);
        final int[] stage = {0}; final long start = h.getLevel().getGameTime();
        h.onEachTick(() -> {
            if (stage[0] == 3) return;
            try {
                if (stage[0] == 0) {
                    s.tick(s.forester);
                    if (h.getLevel().getBlockState(s.trap).getValue(ForesterTrapBlock.STATE) == ForesterTrapBlock.State.OPEN) {
                        h.assertTrue(h.getLevel().getGameTime() - start >= 50, "Rearm takes full 2.5 seconds");
                        ForesterManager.workInput(s.forester, s.trap, true);
                        h.assertTrue(ForesterManager.state(s.forester).work() == -1, "Armed trap cannot be collected");
                        s.spent(); ForesterManager.workInput(s.forester, s.trap, true); stage[0] = 1;
                    }
                } else if (stage[0] == 1) {
                    s.tick(s.forester);
                    if (h.getLevel().getBlockState(s.trap).isAir()) {
                        h.assertTrue(s.forester.getInventory().countItem(ModItems.FORESTER_TRAP.get()) == 1, "Collection gives exactly one reusable item");
                        stage[0] = 3; s.close(); h.succeed();
                    }
                }
            } catch (Throwable failure) { stage[0] = 3; s.close(); throw failure; }
        });
    }
    @GameTest(template="scp173_empty", timeoutTicks=40)
    public static void flashlightRolesWallsAndCharges(GameTestHelper h) {
        try (var s = new Scenario(h)) {
            s.forester.setYRot(0); s.forester.setXRot(0);
            s.maniac.setPos(s.forester.getX(), s.forester.getY(), s.forester.getZ() + 5);
            s.ally.setPos(s.forester.getX(), s.forester.getY(), s.forester.getZ() + 3);
            var stack = new ItemStack(ModItems.FORESTER_FLASHLIGHT.get()); stack.getOrCreateTag().putInt("ForesterUses", 1);
            s.forester.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var item = (ForesterFlashlightItem) stack.getItem();
            item.use(h.getLevel(), s.forester, InteractionHand.MAIN_HAND);
            h.assertTrue(ForesterFlashlightItem.uses(stack) == 0, "Activation spends one charge");
            item.onUseTick(h.getLevel(), s.forester, stack, 59);
            h.assertTrue(s.maniac.hasEffect(ModEffects.FORESTER_BLIND.get()) && !s.ally.hasEffect(ModEffects.FORESTER_BLIND.get()), "Only maniac is blinded");
            h.assertTrue(s.maniac.getEffect(ModEffects.FORESTER_BLIND.get()).getDuration() == 40, "Whiteout lasts two seconds");
            s.maniac.removeEffect(ModEffects.FORESTER_BLIND.get());
            for (int y = 0; y < 4; y++) h.getLevel().setBlockAndUpdate(BlockPos.containing(s.forester.getX(), s.forester.getY() + y, s.forester.getZ() + 2), Blocks.STONE.defaultBlockState());
            item.onUseTick(h.getLevel(), s.forester, stack, 58);
            h.assertTrue(!s.maniac.hasEffect(ModEffects.FORESTER_BLIND.get()), "Solid wall blocks beam");
            s.forester.stopUsingItem();
            h.assertTrue(item.use(h.getLevel(), s.forester, InteractionHand.MAIN_HAND).getResult() == net.minecraft.world.InteractionResult.FAIL, "Empty flashlight cannot activate");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty", timeoutTicks=40)
    public static void pepperAllTeamsAndWater(GameTestHelper h) {
        try (var s = new Scenario(h)) {
            var canister = new PepperCanisterEntity(h.getLevel(), s.forester);
            var center = s.ally.position(); canister.setPos(center);
            canister.onHit(new net.minecraft.world.phys.BlockHitResult(center, net.minecraft.core.Direction.UP, s.ally.blockPosition(), false));
            s.forester.setPos(center.add(1, 0, 0)); s.maniac.setPos(center.add(-1, 0, 0));
            for (int i = 0; i < 4; i++) canister.tick();
            h.assertTrue(s.forester.hasEffect(ModEffects.PEPPER.get()) && s.maniac.hasEffect(ModEffects.PEPPER.get()) && s.ally.hasEffect(ModEffects.PEPPER.get()), "Pepper affects owner, survivor and maniac");
            h.assertTrue(s.maniac.getEffect(ModEffects.PEPPER.get()).getDuration() == 100, "Leaving smoke permits full five-second linger");
            h.getLevel().setBlockAndUpdate(canister.blockPosition(), Blocks.WATER.defaultBlockState());
            canister.tick();
            h.assertTrue(canister.burning() && !canister.isRemoved(), "Water cannot extinguish active canister");
            for (int i = 5; i < 299; i++) canister.tick();
            h.assertTrue(!canister.isRemoved(), "Canister lasts until tick 300");
            canister.tick(); h.assertTrue(canister.isRemoved(), "Canister expires after 15 seconds");
        }
        h.succeed();
    }

    @GameTest(template="scp173_empty", timeoutTicks=40)
    public static void trajectoryAndWorkCancellation(GameTestHelper h) {
        try (var s = new Scenario(h)) {
            s.forester.setYRot(0); s.forester.setXRot(-45);
            s.forester.setDeltaMovement(new net.minecraft.world.phys.Vec3(.05, .04, .02));
            var projectile = new PepperCanisterEntity(h.getLevel(), s.forester);
            var position = s.forester.getEyePosition().add(0, -(double) .1F, 0);
            var velocity = PepperCanisterEntity.launchVelocity(s.forester);
            for (int i = 0; i < 3; i++) {
                position = position.add(velocity);
                velocity = velocity.scale(ForesterRules.DRAG).add(0, -ForesterRules.GRAVITY, 0);
                projectile.tick();
                h.assertTrue(projectile.position().distanceToSqr(position) < 1e-12, "Preview equations match real ThrowableProjectile flight, including owner velocity");
            }
            projectile.discard();
            s.spent(); s.aim();
            ForesterManager.workInput(s.forester, s.trap, false);
            h.assertTrue(ForesterManager.state(s.forester).work() >= 0, "Valid rearm starts");
            s.forester.setYRot(s.forester.getYRot() + .2F);
            h.assertTrue(ForesterManager.aimedAt(s.forester, s.trap), "Small camera turn still points at trap");
            s.tick(s.forester);
            h.assertTrue(ForesterManager.state(s.forester).work() == -1, "Any intentional camera turn cancels work");
            s.aim(); ForesterManager.workInput(s.forester, s.trap, true);
            s.forester.getInventory().selected = 1; s.tick(s.forester);
            h.assertTrue(ForesterManager.state(s.forester).work() == -1 && s.forester.getInventory().countItem(ModItems.FORESTER_TRAP.get()) == 0, "Slot switch cancels collection without duplicating trap");
        }
        h.succeed();
    }

    @GameTest(template="scp173_empty", timeoutTicks=120)
    public static void adventurePlacementAndHeldClick(GameTestHelper h) {
        var s = new Scenario(h);
        h.getLevel().removeBlock(s.trap, false);
        s.forester.setGameMode(GameType.ADVENTURE);
        var stack = new ItemStack(ModItems.FORESTER_TRAP.get(), 2);
        s.forester.setItemInHand(InteractionHand.MAIN_HAND, stack); s.aim();
        h.assertTrue(!s.forester.getAbilities().mayBuild && !stack.hasTag(), "Adventure player has no CanPlaceOn tag");
        var place = new ForesterInputPacket(ForesterInputPacket.Action.PLACE, s.trap, InteractionHand.MAIN_HAND, 0, 0);
        ForesterManager.input(s.forester, place);
        h.assertTrue(ForesterManager.state(s.forester).work() == 0 && stack.getCount() == 2, "Single press starts adventure placement without spending early");
        ForesterManager.input(s.forester, new ForesterInputPacket(ForesterInputPacket.Action.RELEASE, BlockPos.ZERO, InteractionHand.MAIN_HAND, 0, 0));
        h.runAfterDelay(51, () -> {
            try {
                s.tick(s.forester);
                h.assertTrue(h.getLevel().getBlockState(s.trap).is(ModBlocks.FORESTER_TRAP.get()) && stack.getCount() == 1, "Adventure placement completes after mouse release and consumes exactly one item");
                s.spent(); s.aim();
                ForesterManager.input(s.forester, new ForesterInputPacket(ForesterInputPacket.Action.COLLECT, s.trap, InteractionHand.MAIN_HAND, 0, 0));
            } catch (Throwable failure) { s.close(); throw failure; }
        });
        h.runAfterDelay(82, () -> {
            try (s) {
                s.tick(s.forester);
                h.assertTrue(h.getLevel().getBlockState(s.trap).isAir() && s.forester.getInventory().countItem(ModItems.FORESTER_TRAP.get()) == 2, "Collection returns one trap");
                ForesterManager.input(s.forester, place);
                h.assertTrue(ForesterManager.state(s.forester).work() == -1 && h.getLevel().getBlockState(s.trap).isAir(), "Held RMB cannot re-place collected trap");
                ForesterManager.input(s.forester, new ForesterInputPacket(ForesterInputPacket.Action.RELEASE, BlockPos.ZERO, InteractionHand.MAIN_HAND, 0, 0));
                ForesterManager.input(s.forester, place);
                h.assertTrue(ForesterManager.state(s.forester).work() >= 0, "A new press can start another placement");
                ForesterManager.input(s.forester, new ForesterInputPacket(ForesterInputPacket.Action.CANCEL, BlockPos.ZERO, InteractionHand.MAIN_HAND, 0, 0));
                h.assertTrue(ForesterManager.state(s.forester).work() == -1 && s.forester.getInventory().countItem(ModItems.FORESTER_TRAP.get()) == 2, "Movement-attempt cancellation does not spend an item");
            }
            h.succeed();
        });
    }
}
