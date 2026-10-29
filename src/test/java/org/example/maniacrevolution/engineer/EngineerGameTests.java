package org.example.maniacrevolution.engineer;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.block.ModBlocks;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.hack.HackConfig;

@GameTestHolder("engineer_tests")
@PrefixGameTestTemplate(false)
public final class EngineerGameTests {
    private static EngineerStatePacket state(ServerPlayer p) {
        try {
            var field = EngineerManager.class.getDeclaredField("HUD"); field.setAccessible(true);
            return (EngineerStatePacket) ((Map<?, ?>) field.get(null)).get(p.getUUID());
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static final class Scenario implements AutoCloseable {
        final GameTestHelper h; final ServerPlayer owner, enemy; final BlockPos pos; final int oldPhase;
        Scenario(GameTestHelper h) {
            this.h = h; GameManager.init(h.getLevel().getServer());
            var sb = h.getLevel().getScoreboard(); var phase = sb.getObjective("phaseGame");
            oldPhase = sb.getOrCreatePlayerScore("game", phase).getScore(); sb.getOrCreatePlayerScore("game", phase).setScore(3);
            EngineerManager.resetMatch(h.getLevel().getServer());
            pos = h.absolutePos(new BlockPos(5, 3, 5));
            for (int x = -2; x <= 2; x++) for (int z = -3; z <= 3; z++) h.getLevel().setBlockAndUpdate(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            owner = player("survivors", pos.offset(0, 0, -2)); enemy = player("maniac", pos.offset(2, 0, 2));
            var cls = sb.getObjective("SurvivorClass");
            if (cls == null) cls = sb.addObjective("SurvivorClass", ObjectiveCriteria.DUMMY, Component.literal("class"), ObjectiveCriteria.RenderType.INTEGER);
            sb.getOrCreatePlayerScore(owner.getScoreboardName(), cls).setScore(EngineerRules.CLASS_ID);
            owner.getInventory().add(new ItemStack(ModItems.ENGINEER_TABLET.get())); owner.getInventory().add(new ItemStack(ModItems.ENGINEER_REMOTE.get()));
            aim(owner, pos.getX()+.5, pos.getY()-.002, pos.getZ()+.5); update();
        }
        ServerPlayer player(String team, BlockPos at) {
            var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "en_" + UUID.randomUUID().toString().substring(0, 8)));
            p.setGameMode(GameType.ADVENTURE); p.setNoGravity(true); p.setPos(at.getX()+.5, at.getY(), at.getZ()+.5);
            var sb = h.getLevel().getScoreboard(); var t = sb.getPlayerTeam(team); if (t == null) t = sb.addPlayerTeam(team);
            sb.addPlayerToTeam(p.getScoreboardName(), t); h.getLevel().addNewPlayer(p); return p;
        }
        void update() { EngineerManager.update(h.getLevel().getServer(), List.of(owner, enemy)); }
        void action(ServerPlayer p, EngineerActionPacket.Action action, BlockPos pos, int value) { EngineerManager.action(p, new EngineerActionPacket(action, pos, value)); }
        void begin(int type) { action(owner, EngineerActionPacket.Action.SELECT, BlockPos.ZERO, type); action(owner, EngineerActionPacket.Action.PLACE, pos, 0); }
        EngineerDeviceBlockEntity device(boolean barricade) {
            var block = (barricade ? ModBlocks.ENGINEER_BARRICADE : ModBlocks.ENGINEER_PANEL).get();
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var be = (EngineerDeviceBlockEntity) h.getLevel().getBlockEntity(pos);
            try { var field = EngineerManager.class.getDeclaredField("match"); field.setAccessible(true); be.match = (UUID) field.get(null); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            be.owner = owner.getUUID(); be.expires = barricade ? h.getLevel().getGameTime()+2400 : Long.MAX_VALUE;
            EngineerManager.deviceTick(be); return be;
        }
        @Override public void close() {
            EngineerManager.resetMatch(h.getLevel().getServer());
            for (var p : List.of(owner, enemy)) { h.getLevel().getScoreboard().removePlayerFromTeam(p.getScoreboardName()); p.discard(); }
            var sb = h.getLevel().getScoreboard(); sb.getOrCreatePlayerScore("game", sb.getObjective("phaseGame")).setScore(oldPhase);
        }
    }
    private static void aim(ServerPlayer p, double x, double y, double z) {
        double dx=x-p.getX(), dz=z-p.getZ(); p.setYRot((float) Math.toDegrees(Math.atan2(-dx,dz)));
        p.setXRot((float) Math.toDegrees(Math.atan2(p.getEyeY()-y, Math.sqrt(dx*dx+dz*dz))));
    }
    @GameTest(template="scp173_empty", batch="engineer_install", timeoutTicks=90)
    public static void installationChargesOnCompletionAndKeepsSmallTurns(GameTestHelper h) {
        var s = new Scenario(h); s.begin(0);
        h.assertTrue(EngineerManager.busy(s.owner) && state(s.owner).scrap()==40, "Adventure placement begins without spending scrap early");
        s.owner.setYRot(s.owner.getYRot()+3); s.owner.setPos(s.owner.position().add(.08,0,0)); s.update();
        h.assertTrue(EngineerManager.busy(s.owner), "Small accidental movement and camera turn preserve work");
        h.onEachTick(s::update);
        h.runAfterDelay(51, () -> {
            try (s) {
                h.assertTrue(h.getLevel().getBlockState(s.pos).is(ModBlocks.ENGINEER_BARRICADE.get()), "Barricade installed after full duration");
                h.assertTrue(h.getLevel().getBlockState(s.pos.above()).is(ModBlocks.ENGINEER_BARRICADE.get()), "Upper half is real collision");
                h.assertTrue(state(s.owner).scrap()==20 && state(s.owner).barricades()==1, "Exactly one purchase recorded");
                var be=(EngineerDeviceBlockEntity) h.getLevel().getBlockEntity(s.pos);
                h.assertTrue(be.expires-h.getLevel().getGameTime()>=2395, "Lifetime begins after installation and is 120 seconds");
            }
            h.succeed();
        });
    }
    @GameTest(template="scp173_empty", batch="engineer_cancel", timeoutTicks=50)
    public static void cancellationAndNoSpacingAndCreative(GameTestHelper h) {
        try (var s = new Scenario(h)) {
            s.begin(0); s.owner.setXRot(s.owner.getXRot()+20); s.update();
            h.assertTrue(!EngineerManager.busy(s.owner) && state(s.owner).scrap()==40, "Large turn cancels without charge");
            s.device(true);
            h.assertTrue(EngineerManager.validPlacement(h.getLevel(), s.pos.east(), 0, net.minecraft.core.Direction.NORTH), "Adjacent barricade is allowed without spacing rule");
            s.owner.setGameMode(GameType.CREATIVE); s.action(s.owner, EngineerActionPacket.Action.SELECT, BlockPos.ZERO, 0);
            h.assertTrue(EngineerManager.active(s.owner) && state(s.owner).selected()==0, "Creative may use the same workshop and items");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty", batch="engineer_break", timeoutTicks=75)
    public static void adventureDismantlingRequiresHoldAndFacing(GameTestHelper h) {
        var s=new Scenario(h); s.device(true);
        aim(s.enemy,s.pos.getX()+.5,s.pos.getY()+.8,s.pos.getZ()+.5);
        s.action(s.enemy,EngineerActionPacket.Action.BREAK,s.pos,3);
        h.assertTrue(EngineerManager.busy(s.enemy), "Maniac starts dismantling in Adventure");
        h.onEachTick(() -> { s.action(s.enemy,EngineerActionPacket.Action.KEEP_WORKING,s.pos,3); s.update(); });
        h.runAfterDelay(41,() -> {
            try(s) { h.assertTrue(h.getLevel().getBlockState(s.pos).isAir(), "Holding attack removes real block in Adventure"); }
            h.succeed();
        });
    }
    @GameTest(template="scp173_empty", batch="engineer_panel", timeoutTicks=40)
    public static void panelChargesCooldownOwnershipAndExhaustion(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            var be=s.device(false); s.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.ENGINEER_REMOTE.get()));
            s.action(s.enemy,EngineerActionPacket.Action.PLAY,s.pos,0); h.assertTrue(be.uses==3,"Foreign player cannot activate panel");
            s.action(s.owner,EngineerActionPacket.Action.PLAY,s.pos,0); h.assertTrue(be.uses==2 && be.playing(),"First remote press spends one charge");
            s.action(s.owner,EngineerActionPacket.Action.PLAY,s.pos,0); h.assertTrue(be.uses==2,"Repeated packet cannot spend during playback");
            for(int i=0;i<2;i++) {
                be.playingUntil=h.getLevel().getGameTime(); EngineerManager.deviceTick(be); be.readyAt=0;
                s.action(s.owner,EngineerActionPacket.Action.PLAY,s.pos,1);
            }
            h.assertTrue(be.uses==0 && be.playing(),"Third activation plays fully before disappearance");
            be.playingUntil=h.getLevel().getGameTime(); EngineerManager.deviceTick(be);
            h.assertTrue(h.getLevel().getBlockState(s.pos).isAir(),"Exhausted panel disappears without destructive explosion");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty", batch="engineer_income", timeoutTicks=40)
    public static void contributionRollbackAndPlateAndCleanup(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            float points=HackConfig.HACK_POINTS_REQUIRED*.1f;
            EngineerManager.onProgress(99101,0,points,Map.of(s.owner,points));
            h.assertTrue(state(s.owner).scrap()==50,"Ten percent personal progress gives ten scrap");
            EngineerManager.onProgress(99101,0,points,Map.of(s.owner,points));
            h.assertTrue(state(s.owner).scrap()==50,"Repaired rollback cannot pay again");
            EngineerManager.onProgress(99102,0,points,Map.of(s.enemy,points));
            h.assertTrue(state(s.owner).scrap()==50,"Other player does not generate engineer scrap");
            var be=s.device(true); be.expires=h.getLevel().getGameTime(); EngineerManager.deviceTick(be);
            h.assertTrue(h.getLevel().getBlockState(s.pos).isAir(),"Lifetime expiry removes barricade");
            s.device(false); EngineerManager.resetMatch(h.getLevel().getServer());
            h.assertTrue(h.getLevel().getBlockState(s.pos).isAir(),"Round reset removes devices");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty", batch="engineer_plate", timeoutTicks=75)
    public static void protectionAbsorbsManiacDamageAndIgnoresEnvironment(GameTestHelper h) {
        var s = new Scenario(h);
        s.action(s.owner, EngineerActionPacket.Action.SELECT, BlockPos.ZERO, 2);
        h.onEachTick(s::update);
        h.runAfterDelay(41, () -> {
            try(s) {
                h.assertTrue(state(s.owner).armor()==3 && state(s.owner).scrap()==15, "Protection is charged only after full preparation");
                var fall = new net.minecraftforge.event.entity.living.LivingDamageEvent(s.owner, s.owner.damageSources().fall(), 4);
                EngineerManager.damage(fall);
                h.assertTrue(fall.getAmount()==4 && state(s.owner).armor()==3, "Environment neither consumes nor benefits from protection");
                var hit = new net.minecraftforge.event.entity.living.LivingDamageEvent(s.owner, s.owner.damageSources().playerAttack(s.enemy), 9);
                EngineerManager.damage(hit);
                h.assertTrue(hit.getAmount()==6 && state(s.owner).armor()==0, "Only damage exceeding protection reaches HP");
                var again = new net.minecraftforge.event.entity.living.LivingDamageEvent(s.owner, s.owner.damageSources().playerAttack(s.enemy), 9);
                EngineerManager.damage(again); h.assertTrue(again.getAmount()==9, "Second hit gets no free protection");
            }
            h.succeed();
        });
    }
    @GameTest(template="scp173_empty", batch="engineer_repeat_purchase", timeoutTicks=100)
    public static void workshopCanBuyProtectionAgainImmediately(GameTestHelper h) {
        var s = new Scenario(h);
        EngineerManager.depositScrap(s.owner, new ItemStack(ModItems.ENGINEER_SCRAP.get()));
        s.action(s.owner, EngineerActionPacket.Action.SELECT, BlockPos.ZERO, 2);
        h.onEachTick(s::update);
        h.runAfterDelay(41, () -> {
            h.assertTrue(state(s.owner).armor()==3 && state(s.owner).scrap()==35, "First purchase spends 25 scrap");
            s.action(s.owner, EngineerActionPacket.Action.SELECT, BlockPos.ZERO, 2);
            h.assertTrue(EngineerManager.busy(s.owner), "Existing protection and old cooldown cannot block another purchase");
        });
        h.runAfterDelay(82, () -> {
            try(s) {
                h.assertTrue(state(s.owner).armor()==6 && state(s.owner).scrap()==10, "Second purchase stacks and charges exactly once");
            }
            h.succeed();
        });
    }
    @GameTest(template="scp173_empty", batch="engineer_armor_pool", timeoutTicks=40)
    public static void scrapStacksCapsAndRetainsUnusedProtection(GameTestHelper h) {
        try(var s = new Scenario(h)) {
            var scrap = new ItemStack(ModItems.ENGINEER_SCRAP.get(), 10);
            s.owner.setItemInHand(InteractionHand.MAIN_HAND, scrap);
            for(int i=0;i<7;i++) ModItems.ENGINEER_SCRAP.get().finishUsingItem(scrap, h.getLevel(), s.owner);
            h.assertTrue(state(s.owner).armor()==20 && scrap.getCount()==3, "Seven portions stack to the cap of twenty");
            ModItems.ENGINEER_SCRAP.get().finishUsingItem(scrap, h.getLevel(), s.owner);
            h.assertTrue(!EngineerManager.canEatScrap(s.owner) && scrap.getCount()==3, "Full reserve refuses consumption");
            var hit = new net.minecraftforge.event.entity.living.LivingDamageEvent(s.owner, s.owner.damageSources().playerAttack(s.enemy), 2);
            EngineerManager.damage(hit);
            h.assertTrue(hit.getAmount()==0 && state(s.owner).armor()==18, "Small hit preserves the other eighteen points");
            ModItems.ENGINEER_SCRAP.get().finishUsingItem(scrap, h.getLevel(), s.owner);
            h.assertTrue(state(s.owner).armor()==20 && scrap.getCount()==2, "Partly depleted reserve can be replenished immediately");
            var heavy = new net.minecraftforge.event.entity.living.LivingDamageEvent(s.owner, s.owner.damageSources().playerAttack(s.enemy), 23);
            EngineerManager.damage(heavy);
            h.assertTrue(heavy.getAmount()==3 && state(s.owner).armor()==0, "Heavy hit spends twenty and sends three damage to HP");
            EngineerManager.eatScrap(s.owner);
            EngineerManager.death(new net.minecraftforge.event.entity.living.LivingDeathEvent(s.owner, s.owner.damageSources().playerAttack(s.enemy)));
            h.assertTrue(state(s.owner).armor()==0, "Death clears the reserve");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty", batch="engineer_armor_packet", timeoutTicks=40)
    public static void protectionPacketPreservesFractionalReserveAndPanelData(GameTestHelper h) {
        var packet = new EngineerStatePacket(999,true,40,12,2,19.5f,-1,2,new BlockPos(1,64,3),900,1000,
                List.of(new EngineerStatePacket.Panel(net.minecraft.world.level.Level.OVERWORLD.location(),new BlockPos(4,65,6),2,1200,1500,true)));
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            packet.encode(buf);
            h.assertTrue(packet.equals(EngineerStatePacket.decode(buf)) && !buf.isReadable(), "Updated protocol preserves fractional protection and all following fields");
        } finally { buf.release(); }
        h.succeed();
    }
    private static void outsideItems(GameTestHelper h, GameType mode) {
        var s = new Scenario(h);
        var board = h.getLevel().getScoreboard();
        board.getOrCreatePlayerScore("game", board.getObjective("phaseGame")).setScore(0);
        board.getOrCreatePlayerScore(s.owner.getScoreboardName(), board.getObjective("SurvivorClass")).setScore(0);
        board.removePlayerFromTeam(s.owner.getScoreboardName()); s.owner.setGameMode(mode); s.update();
        var barricades = new ItemStack(ModItems.ENGINEER_BARRICADE.get(), 2);
        s.owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ENGINEER_TABLET.get()));
        ModItems.ENGINEER_TABLET.get().use(h.getLevel(), s.owner, InteractionHand.MAIN_HAND);
        h.assertTrue(state(s.owner).active() && state(s.owner).scrap()==40, "Workshop item is authorised without class, team or match in "+mode);
        s.owner.setItemInHand(InteractionHand.MAIN_HAND, barricades);
        ModItems.ENGINEER_BARRICADE.get().use(h.getLevel(), s.owner, InteractionHand.MAIN_HAND);
        h.assertTrue(EngineerManager.busy(s.owner), "Physical barricade item starts building in "+mode);
        h.onEachTick(s::update);
        h.runAfterDelay(51, () -> {
            h.assertTrue(h.getLevel().getBlockState(s.pos).is(ModBlocks.ENGINEER_BARRICADE.get()), "Barricade survives outside a match in "+mode);
            h.assertTrue(barricades.getCount()==(mode==GameType.CREATIVE?2:1) && state(s.owner).scrap()==40, "Physical construction consumes one item, not wallet scrap; Creative preserves it");
        });
        BlockPos panelPos=s.pos.east(); var panelStack=new ItemStack(ModItems.ENGINEER_PANEL.get());
        h.runAfterDelay(152, () -> {
            s.owner.setItemInHand(InteractionHand.MAIN_HAND,panelStack);
            aim(s.owner,panelPos.getX()+.5,panelPos.getY()-.002,panelPos.getZ()+.5);
            ModItems.ENGINEER_PANEL.get().use(h.getLevel(),s.owner,InteractionHand.MAIN_HAND);
            h.assertTrue(EngineerManager.busy(s.owner), "Physical panel starts building in "+mode+"; state="+state(s.owner)
                    +"; aim="+EngineerManager.placement(s.owner)+"; target="+panelPos+"; clock="+h.getLevel().getGameTime()
                    +"; usable="+EngineerManager.active(s.owner)+"; valid="+EngineerManager.validPlacement(h.getLevel(),panelPos,1,s.owner.getDirection()));
        });
        h.runAfterDelay(203, () -> {
            try(s) {
                h.assertTrue(h.getLevel().getBlockEntity(panelPos) instanceof EngineerDeviceBlockEntity, "Panel item places a usable device in "+mode);
                h.assertTrue(panelStack.getCount()==(mode==GameType.CREATIVE?1:0), "Panel item consumption matches mode");
                var panel=(EngineerDeviceBlockEntity)h.getLevel().getBlockEntity(panelPos);
                s.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.ENGINEER_REMOTE.get()));
                ModItems.ENGINEER_REMOTE.get().use(h.getLevel(),s.owner,InteractionHand.MAIN_HAND);
                s.action(s.owner,EngineerActionPacket.Action.PLAY,panelPos,0);
                h.assertTrue(panel.uses==2 && panel.playing(), "Remote activates the panel outside a match without a class in "+mode);
                var scrap=new ItemStack(ModItems.ENGINEER_SCRAP.get(),2);
                s.owner.setItemInHand(InteractionHand.MAIN_HAND,scrap);
                ModItems.ENGINEER_SCRAP.get().use(h.getLevel(),s.owner,InteractionHand.MAIN_HAND);
                h.assertTrue(s.owner.isUsingItem() && ModItems.ENGINEER_SCRAP.get().getUseDuration(scrap)==40, "Scrap uses the vanilla eating animation in "+mode);
                ModItems.ENGINEER_SCRAP.get().finishUsingItem(scrap,h.getLevel(),s.owner); s.owner.stopUsingItem();
                h.assertTrue(state(s.owner).armor()==3 && scrap.getCount()==(mode==GameType.CREATIVE?2:1), "Eating scrap adds protection with mode-appropriate consumption");
                s.owner.setShiftKeyDown(true);
                ModItems.ENGINEER_SCRAP.get().use(h.getLevel(),s.owner,InteractionHand.MAIN_HAND);
                h.assertTrue(state(s.owner).scrap()==60 && scrap.getCount()==(mode==GameType.CREATIVE?2:0), "Shift-use deposits the bundle into the wallet");
                s.owner.setShiftKeyDown(false);
            }
            h.succeed();
        });
    }
    @GameTest(template="scp173_empty", batch="engineer_outside_creative", timeoutTicks=260)
    public static void allItemsOutsideMatchCreative(GameTestHelper h) { outsideItems(h,GameType.CREATIVE); }
    @GameTest(template="scp173_empty", batch="engineer_outside_survival", timeoutTicks=260)
    public static void allItemsOutsideMatchSurvival(GameTestHelper h) { outsideItems(h,GameType.SURVIVAL); }
    @GameTest(template="scp173_empty", batch="engineer_outside_adventure", timeoutTicks=260)
    public static void allItemsOutsideMatchAdventure(GameTestHelper h) { outsideItems(h,GameType.ADVENTURE); }
}
