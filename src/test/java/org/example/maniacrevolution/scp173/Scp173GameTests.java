package org.example.maniacrevolution.scp173;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.DownedCapability;
import org.example.maniacrevolution.downed.DownedState;
import org.example.maniacrevolution.entity.RedColorCardProjectile;
import org.example.maniacrevolution.perk.perks.common.GtoMedalPerk;

import java.util.UUID;

/** Dedicated Forge checks; only the gameTestServer run includes this test source set. */
@GameTestHolder("maniacrev")
@PrefixGameTestTemplate(false)
public final class Scp173GameTests {
    @GameTest(template = "scp173_empty", batch = "scp173", timeoutTicks = 40)
    public static void blinkCoordinationAndLifecycle(GameTestHelper h) {
        var scoreboard = h.getLevel().getScoreboard();
        var objective = scoreboard.getObjective("phaseGame");
        if (objective == null) objective = scoreboard.addObjective("phaseGame", ObjectiveCriteria.DUMMY,
                Component.literal("phase"), ObjectiveCriteria.RenderType.INTEGER);
        var phase = scoreboard.getOrCreatePlayerScore("game", objective);
        int old = phase.getScore(); phase.setScore(1);
        var statue = player(h, "scp_blink_body", "maniac", 2, 2, 8);
        var first = player(h, "scp_blink_first", "survivors", 2, 2, 2);
        var second = player(h, "scp_blink_second", "survivors", 3, 2, 2);
        PlayerDataManager.get(statue).setManiacClassId(13);
        first.setYRot(0); second.setYRot(0);
        var players = java.util.List.<net.minecraft.server.level.ServerPlayer>of(statue, first, second);
        try {
            Scp173GameplayManager.update(players);
            h.assertTrue(Scp173GameplayManager.observers(statue) == 2 && Scp173Manager.held(statue), "both observers count initially");
            double baseSpeed = statue.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getValue();
            Scp173GameplayManager.input(first, org.example.maniacrevolution.network.packets.Scp173InputPacket.Action.BLINK);
            h.assertTrue(Scp173GameplayManager.blinking(first) && Scp173GameplayManager.observers(statue) == 1
                    && Scp173Manager.held(statue), "first blink does not release another observer's hold");
            h.assertTrue(statue.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getValue() == baseSpeed,
                    "one remaining observer prevents speed bonus");
            Scp173GameplayManager.input(second, org.example.maniacrevolution.network.packets.Scp173InputPacket.Action.BLINK);
            h.assertTrue(Scp173GameplayManager.observers(statue) == 0 && !Scp173Manager.held(statue), "simultaneous eyes closed release statue");
            Scp173GameplayManager.update(players);
            h.assertTrue(Math.abs(statue.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getValue()
                    / baseSpeed - 1.5) < 1e-8, "unobserved statue is fifty percent faster");
            DownedCapability.get(first).setState(DownedState.DOWNED);
            second.setGameMode(GameType.SPECTATOR);
            Scp173GameplayManager.update(players);
            h.assertTrue(!Scp173GameplayManager.blinking(first) && !Scp173GameplayManager.blinking(second), "downed and spectator clean blink state");
            first.setGameMode(GameType.SURVIVAL); DownedCapability.get(first).setState(DownedState.ALIVE);
            Scp173GameplayManager.update(players);
            h.assertTrue(!Scp173GameplayManager.blinking(first) && Scp173Manager.held(statue), "revived observer starts with open eyes");
            h.assertTrue(statue.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getValue() == baseSpeed,
                    "observation removes bonus without stacking modifiers");
            statue.removeEffect(org.example.maniacrevolution.effect.ModEffects.SCP173_ARMOR.get());
            Scp173GameplayManager.update(players);
            h.assertTrue(!statue.hasEffect(org.example.maniacrevolution.effect.ModEffects.SCP173_ARMOR.get()), "regular match tick does not reapply removed shell");
            phase.setScore(0); Scp173GameplayManager.update(players);
            h.assertTrue(!Scp173Manager.held(statue) && !Scp173GameplayManager.blinking(first), "match end clears restrictions and blink");
            h.assertTrue(statue.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    .getModifier(UUID.fromString("f3c1e4e9-e3a1-4a55-a53b-6e9e8c9dcbd1")) == null, "class speed modifier removed on exit");
            h.succeed();
        } finally {
            Scp173GameplayManager.update(java.util.List.of()); phase.setScore(old);
            for (var p : players) { p.discard(); scoreboard.removePlayerFromTeam(p.getScoreboardName()); }
        }
    }

    @GameTest(template = "scp173_empty", batch = "scp173", timeoutTicks = 40)
    public static void fullClassMechanics(GameTestHelper h) {
        var scoreboard = h.getLevel().getScoreboard();
        var objective = scoreboard.getObjective("phaseGame");
        if (objective == null) objective = scoreboard.addObjective("phaseGame", ObjectiveCriteria.DUMMY,
                Component.literal("phase"), ObjectiveCriteria.RenderType.INTEGER);
        var phase = scoreboard.getOrCreatePlayerScore("game", objective);
        int oldPhase = phase.getScore(); phase.setScore(1);
        FakePlayer statue = player(h, "scp_full_statue", "maniac", 2, 2, 8);
        FakePlayer observer = player(h, "scp_full_observer", "survivors", 2, 2, 2);
        PlayerDataManager.get(statue).setManiacClassId(13);
        var logout = new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(statue);
        try {
            // FakePlayer.tick() is empty, so native 60-tick spawn protection never expires.
            // Only this damage fixture disables it, using Forge's mapped test reflection.
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.server.level.ServerPlayer.class, statue, 0, "f_8921_");
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.server.level.ServerPlayer.class, observer, 0, "f_8921_");
            observer.setYRot(0); observer.setXRot(0);
            h.assertTrue(Scp173Manager.held(statue), "fixture initially held");
            h.assertTrue(statue.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA).isPresent(), "mana capability attached");
            statue.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA).ifPresent(m -> m.setMana(0));
            Scp173GameplayManager.input(statue, org.example.maniacrevolution.network.packets.Scp173InputPacket.Action.LIGHT);
            h.assertTrue(Scp173GameplayManager.blackout(observer) == 0, "insufficient mana cannot start blackout");
            h.assertTrue(statue.hasEffect(org.example.maniacrevolution.effect.ModEffects.SCP173_ARMOR.get()), "class entry grants removable armor");
            var armor = statue.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            h.assertTrue(armor.getValue() == 7, "shell equals all four leather pieces");
            float leather = 0;
            for (var item : new net.minecraft.world.item.Item[]{net.minecraft.world.item.Items.LEATHER_HELMET,
                    net.minecraft.world.item.Items.LEATHER_CHESTPLATE, net.minecraft.world.item.Items.LEATHER_LEGGINGS,
                    net.minecraft.world.item.Items.LEATHER_BOOTS}) leather += ((net.minecraft.world.item.ArmorItem) item).getDefense();
            h.assertTrue(net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(10, leather, 0)
                    == net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(10, (float) armor.getValue(), 0), "native damage formula matches leather");
            statue.removeEffect(org.example.maniacrevolution.effect.ModEffects.SCP173_ARMOR.get());
            Scp173GameplayManager.input(statue, org.example.maniacrevolution.network.packets.Scp173InputPacket.Action.LIGHT);
            h.assertTrue(armor.getValue() == 0 && !statue.hasEffect(org.example.maniacrevolution.effect.ModEffects.SCP173_ARMOR.get()), "removed shell not reapplied next action");
            statue.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA).ifPresent(m -> m.setMana(10));
            Scp173GameplayManager.input(statue, org.example.maniacrevolution.network.packets.Scp173InputPacket.Action.LIGHT);
            h.assertTrue(Scp173GameplayManager.blackout(observer) == 60, "held statue may black out entire dimension for three seconds");
            h.assertTrue(!Scp173Manager.held(statue) && Scp173GameplayManager.sightRange(observer) == 1, "distant observer no longer holds during blackout");
            statue.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA).ifPresent(m -> h.assertTrue(m.getMana() == 0, "successful light spends ten mana"));
            statue.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA).ifPresent(m -> m.setMana(10));
            Scp173GameplayManager.input(statue, org.example.maniacrevolution.network.packets.Scp173InputPacket.Action.LIGHT);
            statue.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA).ifPresent(m -> h.assertTrue(m.getMana() == 10, "cooldown rejection does not spend mana"));
            observer.setPos(statue.position().add(0, 0, 1.0)); observer.setYRot(0);
            statue.setYRot(0); statue.setXRot(0);
            observer.setPos(statue.position().add(0, 0, 1.81));
            h.assertTrue(Scp173CombatGeometry.target(statue) == null, "body contact beyond 1.5 blocks misses");
            observer.setPos(statue.position().add(0, 0, 1.79));
            h.assertTrue(Scp173CombatGeometry.target(statue) == observer, "body contact just inside 1.5 blocks reaches target");
            var cover = statue.blockPosition().offset(0, 1, 1);
            var oldCover = h.getLevel().getBlockState(cover);
            try {
                h.getLevel().setBlockAndUpdate(cover, Blocks.STONE.defaultBlockState());
                h.assertTrue(Scp173CombatGeometry.target(statue) == null, "extended strike cannot pass through solid cover");
            } finally { h.getLevel().setBlockAndUpdate(cover, oldCover); }
            h.assertTrue(!Scp173Manager.held(statue) && Scp173CombatGeometry.target(statue) == observer, "fixture has an unheld statue aimed at a close target");
            float health = observer.getHealth();
            Scp173GameplayManager.input(statue, org.example.maniacrevolution.network.packets.Scp173InputPacket.Action.MELEE);
            h.assertTrue(observer.getHealth() < health, "1.5-block hand strike deals damage beyond previous reach");
            health = observer.getHealth();
            Scp173GameplayManager.input(statue, org.example.maniacrevolution.network.packets.Scp173InputPacket.Action.MELEE);
            h.assertTrue(observer.getHealth() == health, "repeated strike during cooldown does no damage");
            observer.setPos(statue.position().add(0, 0, 1.0));
            statue.setOnGround(true); statue.setDeltaMovement(Vec3.ZERO);
            h.assertTrue(Scp173GameplayManager.permitClientJump(statue), "first ordinary jump is ready");
            h.assertTrue(statue.getEffect(org.example.maniacrevolution.effect.ModEffects.JUMP_COOLDOWN.get()).getDuration() == 60,
                    "jump cooldown is a visible three-second effect");
            var repeatJump = new ServerboundMovePlayerPacket.PosRot(statue.getX(), statue.getY() + 0.42,
                    statue.getZ(), statue.getYRot(), statue.getXRot(), false);
            h.assertTrue(Scp173MovementGate.filter(statue, repeatJump) == null, "server rejects repeated positional jump during cooldown");
            var forgedGroundJump = new ServerboundMovePlayerPacket.PosRot(statue.getX(), statue.getY() + 0.42,
                    statue.getZ(), statue.getYRot(), statue.getXRot(), true);
            h.assertTrue(Scp173MovementGate.filter(statue, forgedGroundJump) == null, "forged ground flag cannot bypass jump cooldown");
            observer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    org.example.maniacrevolution.effect.ModEffects.JUMP_COOLDOWN.get(), 60));
            observer.setOnGround(true); observer.setDeltaMovement(Vec3.ZERO);
            var survivorJump = new ServerboundMovePlayerPacket.PosRot(observer.getX(), observer.getY() + 0.42,
                    observer.getZ(), observer.getYRot(), observer.getXRot(), false);
            h.assertTrue(Scp173MovementGate.filter(observer, survivorJump) == null, "generic effect blocks other classes too");
            observer.setDeltaMovement(0, 0.5, 0);
            h.assertTrue(Scp173MovementGate.filter(observer, survivorJump) == survivorJump, "effect preserves external upward impulses");
            observer.setDeltaMovement(Vec3.ZERO);
            observer.removeEffect(org.example.maniacrevolution.effect.ModEffects.JUMP_COOLDOWN.get());
            h.assertTrue(Scp173MovementGate.filter(observer, survivorJump) == survivorJump, "removing generic effect restores jump");
            statue.invulnerableTime = 0; statue.setDeltaMovement(Vec3.ZERO); statue.setOnGround(true);
            statue.hurt(statue.damageSources().playerAttack(observer), 1);
            h.assertTrue(statue.getDeltaMovement().horizontalDistanceSqr() == 0, "native melee damage does not knock statue");
            statue.knockback(0.5, 1, 0);
            h.assertTrue(statue.getDeltaMovement().horizontalDistanceSqr() > 0, "external knockback API remains allowed even after damage");
            statue.setDeltaMovement(Vec3.ZERO); statue.invulnerableTime = 0;
            var arrow = new net.minecraft.world.entity.projectile.Arrow(h.getLevel(), observer);
            arrow.setBaseDamage(0.1); arrow.setKnockback(3); arrow.setNoGravity(true);
            arrow.setPos(statue.position().add(0, 0.8, -1)); arrow.setDeltaMovement(0, 0, 2);
            h.getLevel().addFreshEntity(arrow); arrow.tick(); arrow.discard();
            h.assertTrue(statue.getDeltaMovement().horizontalDistanceSqr() == 0, "ordinary Punch arrow cannot push statue");
            statue.invulnerableTime = 0;
            observer.setYRot(180);
            h.assertTrue(Scp173Manager.held(statue), "close observer may still hold during blackout");
            var perkArrow = new net.minecraft.world.entity.projectile.Arrow(h.getLevel(), observer);
            Scp173ImpulsePolicy.markAbility(perkArrow);
            perkArrow.setBaseDamage(0.1); perkArrow.setKnockback(3); perkArrow.setNoGravity(true);
            perkArrow.setPos(statue.position().add(0, 0.8, -1)); perkArrow.setDeltaMovement(0, 0, 2);
            h.getLevel().addFreshEntity(perkArrow); perkArrow.tick(); perkArrow.discard();
            h.assertTrue(statue.getDeltaMovement().horizontalDistanceSqr() > 0, "ability-provenance vanilla arrow may push statue");
            Scp173GameplayManager.logout(logout);
            h.assertTrue(Scp173GameplayManager.blackout(observer) == 0 && armor.getValue() == 0, "exit clears light and class modifiers");
            h.assertTrue(!statue.hasEffect(org.example.maniacrevolution.effect.ModEffects.JUMP_COOLDOWN.get()),
                    "class exit clears the jump effect it granted");
            observer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 100));
            h.assertTrue(Scp173GameplayManager.sightRange(observer) == 3, "blindness without light uses three-block range");
            h.succeed();
        } finally {
            Scp173GameplayManager.logout(logout);
            phase.setScore(oldPhase);
            for (var p : new FakePlayer[]{statue, observer}) { p.discard(); scoreboard.removePlayerFromTeam(p.getScoreboardName()); }
        }
    }

    @GameTest(template = "scp173_empty", batch = "scp173", timeoutTicks = 40)
    public static void staticModelVisibility(GameTestHelper h) {
        FakePlayer observer = player(h, "scp_model_observer", "survivors", 2, 2, 2);
        FakePlayer statue = player(h, "scp_model_statue", "maniac", 2, 2, 8);
        try {
            var surface = Scp173Geometry.surface();
            h.assertTrue(surface.size() == 22 * 26, "every shipped cube contributes surface samples");
            double top = surface.stream().mapToDouble(p -> p.y).max().orElseThrow();
            h.assertTrue(Math.abs(top - 2.025) < 0.00001, "authored 36-unit geometry uses the renderer's 0.9 scale");
            var front = new net.minecraft.world.phys.Vec3(0, 0, -1);
            var facingSouth = Scp173Geometry.worldPoint(front, net.minecraft.world.phys.Vec3.ZERO, 0);
            var facingWest = Scp173Geometry.worldPoint(front, net.minecraft.world.phys.Vec3.ZERO, 90);
            h.assertTrue(facingSouth.z > 0.999 && facingWest.x < -0.999, "geometry turns with character yaw");
            var head = new Vec3(0, top, 0);
            var standingHead = Scp173Geometry.worldPoint(head, Vec3.ZERO, 0, 0, 40, true);
            h.assertTrue(standingHead.distanceTo(head) < 1e-8, "swim transform leaves standing geometry unchanged");
            var swimmingHead = Scp173Geometry.worldPoint(head, Vec3.ZERO, 0, 1, 0, true);
            h.assertTrue(Math.abs(swimmingHead.y - 0.3) < 1e-8 && swimmingHead.z > 1,
                    "swimming model lies horizontally with its head facing forward");
            var divingHead = Scp173Geometry.worldPoint(head, Vec3.ZERO, 0, 1, 90, true);
            h.assertTrue(divingHead.y < -0.7, "underwater dive rotates rigid model with pitch");
            wall(h, Blocks.STONE, 2, 5);
            h.assertTrue(!Scp173Vision.sees(observer, statue), "full wall blocks actual model");
            wall(h, Blocks.AIR, 3, 5);
            wall(h, Blocks.STONE_SLAB, 3, 3);
            observer.setPos(observer.position().add(0, -0.32, 0));
            observer.setYRot(0); observer.setXRot(0);
            statue.setPose(net.minecraft.world.entity.Pose.CROUCHING);
            statue.refreshDimensions();
            h.assertTrue(statue.getBoundingBox().getYsize() < 1.8, "fixture uses crouched player hitbox");
            h.assertTrue(Scp173Vision.sees(observer, statue), "static head above slab holds despite crouched hitbox");
            statue.setPose(net.minecraft.world.entity.Pose.SWIMMING);
            statue.refreshDimensions();
            h.assertTrue(Scp173Vision.sees(observer, statue), "first swimming transition frame retains exposed standing head");
            wall(h, Blocks.STONE, 3, 3);
            h.assertTrue(!Scp173Vision.sees(observer, statue), "covering exposed model head releases gaze");
            h.succeed();
        } finally {
            for (var p : new FakePlayer[]{observer, statue}) {
                p.discard();
                h.getLevel().getScoreboard().removePlayerFromTeam(p.getScoreboardName());
            }
        }
    }

    @GameTest(template = "scp173_empty", batch = "scp173", timeoutTicks = 40)
    public static void gazeAndPhysics(GameTestHelper h) {
        var level = h.getLevel();
        var scoreboard = level.getScoreboard();
        var objective = scoreboard.getObjective("phaseGame");
        if (objective == null) objective = scoreboard.addObjective("phaseGame", ObjectiveCriteria.DUMMY,
                Component.literal("phase"), ObjectiveCriteria.RenderType.INTEGER);
        var phase = scoreboard.getOrCreatePlayerScore("game", objective);
        int previousPhase = phase.getScore();
        phase.setScore(1);
        FakePlayer observer = player(h, "scp_observer", "survivors", 2, 2, 2);
        FakePlayer statue = player(h, "scp_statue", "maniac", 2, 2, 8);
        PlayerDataManager.get(statue).setManiacClassId(13);
        try {
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) h.setBlock(x, 1, z, Blocks.STONE);
            observer.setYRot(0); observer.setXRot(0);
            h.assertTrue(Scp173Vision.sees(observer, statue), "unobstructed body visible");
            wall(h, Blocks.STONE, 2, 5);
            h.assertTrue(!Scp173Vision.sees(observer, statue), "opaque wall blocks entire body");
            wall(h, Blocks.AIR, 3, 5);
            h.assertTrue(Scp173Vision.sees(observer, statue), "head visible above wall despite hidden body centre");
            wall(h, Blocks.GLASS, 2, 5);
            h.assertTrue(Scp173Vision.sees(observer, statue), "glass wall passes gaze");
            wall(h, Blocks.RED_STAINED_GLASS, 2, 5);
            h.assertTrue(Scp173Vision.sees(observer, statue), "stained glass passes gaze");
            wall(h, Blocks.GLASS_PANE, 2, 5);
            h.assertTrue(Scp173Vision.sees(observer, statue), "glass panes pass gaze");
            wall(h, Blocks.AIR, 2, 5);
            observer.setYRot(180);
            h.assertTrue(!Scp173Manager.held(statue), "third-person camera cannot hold behind head");
            observer.setYRot(0);
            h.assertTrue(Scp173Manager.held(statue), "living survivor holds statue");
            var downed = DownedCapability.get(observer);
            h.assertTrue(downed != null, "observer has downed capability");
            downed.setState(DownedState.DOWNED);
            h.assertTrue(!Scp173Manager.held(statue), "downed survivor releases statue");
            downed.setState(DownedState.ALIVE);
            observer.setGameMode(GameType.SPECTATOR);
            h.assertTrue(!Scp173Manager.held(statue), "spectator releases statue");
            observer.setGameMode(GameType.SURVIVAL);
            h.assertTrue(Scp173Manager.held(statue), "holding resumes");
            FakePlayer second = player(h, "scp_observer2", "survivors", 3, 2, 2);
            try {
                second.setYRot(0);
                observer.setYRot(180);
                h.assertTrue(Scp173Manager.held(statue), "one remaining observer is sufficient");
                second.setHealth(0);
                h.assertTrue(!Scp173Manager.held(statue), "dead final observer releases statue");
            } finally {
                second.discard();
                scoreboard.removePlayerFromTeam(second.getScoreboardName());
            }
            observer.setYRot(0);
            h.assertTrue(Scp173Manager.held(statue), "first observer resumes holding");
            float yaw = statue.getYRot(), pitch = statue.getXRot();
            var forged = new ServerboundMovePlayerPacket.PosRot(statue.getX() + 5, statue.getY() + 1,
                    statue.getZ() + 5, yaw + 90, pitch + 30, false);
            var filtered = (ServerboundMovePlayerPacket) Scp173MovementGate.filter(statue, forged);
            h.assertTrue(filtered.getX(0) == statue.getX() && filtered.getY(0) == statue.getY()
                    && filtered.getZ(0) == statue.getZ(), "server rejects voluntary motion and jump");
            h.assertTrue(filtered.getYRot(0) == yaw && filtered.getXRot(0) == pitch, "server rejects turning");
            var input = (ServerboundPlayerInputPacket) Scp173MovementGate.filter(statue,
                    new ServerboundPlayerInputPacket(1, 1, true, true));
            h.assertTrue(input.getXxa() == 0 && input.getZza() == 0 && !input.isJumping(), "riding input suppressed");
            var attack = new AttackEntityEvent(statue, observer);
            Scp173Manager.attack(attack);
            h.assertTrue(attack.isCanceled(), "ordinary attack blocked");
            statue.setPos(statue.position().add(0, 3, 0));
            statue.setOnGround(false);
            statue.setDeltaMovement(0, -0.2, 0);
            double height = statue.getY();
            physicalTick(statue);
            h.assertTrue(statue.getY() < height && statue.getDeltaMovement().y < -0.2, "held statue falls with gravity");
            // Use the actual perk, then vanilla travel and the held server tick hooks.
            new GtoMedalPerk().onActivate(statue);
            Vec3 impulse = statue.getDeltaMovement();
            Vec3 before = statue.position();
            physicalTick(statue);
            h.assertTrue(impulse.z > 0 && statue.getZ() > before.z && statue.getY() > before.y,
                    "perk remains usable and its full impulse moves the held statue");
            statue.setDeltaMovement(Vec3.ZERO);
            statue.knockback(1, -1, 0);
            before = statue.position();
            physicalTick(statue);
            h.assertTrue(statue.getX() > before.x, "vanilla knockback API used by perk items is preserved");
            statue.setDeltaMovement(Vec3.ZERO);
            var card = new RedColorCardProjectile(level, observer);
            // Start outside ProjectileUtil's 0.3-block inflated target box: a ray starting
            // inside that box has no entry face and cannot exercise the flight collision path.
            card.setPos(statue.getX() - 1, statue.getY() + 1, statue.getZ());
            card.setDeltaMovement(2, 0, 0);
            level.addFreshEntity(card);
            card.tick();
            h.assertTrue(card.isRemoved(), "red card did not collide: target=" + statue.getBoundingBox()
                    + ", card=" + card.position() + ", pickable=" + statue.isPickable());
            h.assertTrue(statue.getDeltaMovement().horizontalDistanceSqr() > 0,
                    "red card collided without knockback: motion=" + statue.getDeltaMovement());
            before = statue.position();
            physicalTick(statue);
            h.assertTrue(statue.position().subtract(before).horizontalDistanceSqr() > 0,
                    "red card knockback displaces the held statue");
            statue.setGameMode(GameType.CREATIVE);
            h.assertTrue(!Scp173Manager.held(statue), "creative statue keeps vanilla behavior");
            statue.setGameMode(GameType.SURVIVAL);
            h.assertTrue(Scp173Manager.held(statue), "survival restores gaze holding");
            phase.setScore(0);
            h.assertTrue(!Scp173Manager.held(statue), "match end releases hold");
            h.assertTrue(Scp173MovementGate.filter(statue, forged) == forged, "free movement packet untouched");
            phase.setScore(1);
            PlayerDataManager.get(statue).setManiacClassId(9);
            h.assertTrue(!Scp173Manager.held(statue), "Warden class is unaffected");
            h.succeed();
        } finally {
            phase.setScore(previousPhase);
            statue.discard(); observer.discard();
            scoreboard.removePlayerFromTeam(statue.getScoreboardName());
            scoreboard.removePlayerFromTeam(observer.getScoreboardName());
        }
    }

    private static FakePlayer player(GameTestHelper h, String name, String teamName, int x, int y, int z) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
            // Forge's default FakePlayer is invulnerable and refuses PvP, so damage fixtures
            // must explicitly opt into the native ServerPlayer damage path.
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return false; }
            @Override public boolean canHarmPlayer(net.minecraft.world.entity.player.Player target) { return true; }
        };
        var pos = h.absolutePos(new BlockPos(x, y, z));
        p.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        p.setGameMode(GameType.SURVIVAL);
        var scoreboard = h.getLevel().getScoreboard();
        var team = scoreboard.getPlayerTeam(teamName);
        if (team == null) team = scoreboard.addPlayerTeam(teamName);
        scoreboard.addPlayerToTeam(p.getScoreboardName(), team);
        h.getLevel().addNewPlayer(p);
        return p;
    }

    private static void wall(GameTestHelper h, net.minecraft.world.level.block.Block block, int low, int high) {
        for (int x = 0; x <= 5; x++) for (int y = low; y <= high; y++) h.setBlock(x, y, 5, block);
    }

    private static void physicalTick(FakePlayer p) {
        Scp173Manager.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, p));
        p.travel(Vec3.ZERO);
        Scp173Manager.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
    }

    @GameTest(template = "scp173_empty", batch = "scp173", timeoutTicks = 40)
    public static void nativePlayerTick(GameTestHelper h) {
        var level = h.getLevel();
        var scoreboard = level.getScoreboard();
        var objective = scoreboard.getObjective("phaseGame");
        if (objective == null) objective = scoreboard.addObjective("phaseGame", ObjectiveCriteria.DUMMY,
                Component.literal("phase"), ObjectiveCriteria.RenderType.INTEGER);
        var phase = scoreboard.getOrCreatePlayerScore("game", objective);
        int previous = phase.getScore();
        phase.setScore(1);
        var observer = player(h, "scp_native_eye", "survivors", 2, 2, 2);
        var p = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "scp_native_body"));
        var origin = h.absolutePos(new BlockPos(2, 2, 8));
        p.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var listener = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, p);
        p.connection = listener;
        scoreboard.addPlayerToTeam(p.getScoreboardName(), scoreboard.getPlayerTeam("maniac") == null
                ? scoreboard.addPlayerTeam("maniac") : scoreboard.getPlayerTeam("maniac"));
        PlayerDataManager.get(p).setManiacClassId(13);
        level.addNewPlayer(p);
        try {
            observer.setYRot(0);
            h.assertTrue(Scp173Manager.held(p), "native server player held");
            p.setDeltaMovement(0.6, 0.4, 0);
            Vec3 before = p.position();
            listener.tick();
            h.assertTrue(p.getX() > before.x && p.getY() > before.y,
                    "native listener tick retains external motion after doTick instead of restoring old position");
            float yaw = p.getYRot();
            var forged = new ServerboundMovePlayerPacket.PosRot(p.getX() + 5, p.getY() + 1, p.getZ(), yaw + 90, 30, false);
            before = p.position();
            Scp173MovementGate.handleMove(listener, (ServerboundMovePlayerPacket) Scp173MovementGate.filter(p, forged));
            h.assertTrue(p.position().equals(before) && p.getYRot() == yaw,
                    "native listener cannot move or turn using a held client packet");
            p.setNoGravity(true);
            p.setDeltaMovement(Vec3.ZERO);
            for (int i = 0; i < 100; i++) {
                Scp173MovementGate.handleMove(listener, (ServerboundMovePlayerPacket) Scp173MovementGate.filter(p, forged));
                listener.tick();
            }
            h.assertTrue(!p.isRemoved() && !p.getAbilities().mayfly,
                    "100 ticks of server-owned airborne physics do not cause a vanilla flight kick or grant client flight");
            p.setNoGravity(false);
            observer.setYRot(180);
            h.assertTrue(!Scp173Manager.held(p), "free jump setup releases gaze");
            p.setOnGround(true);
            p.setSprinting(true);
            p.setDeltaMovement(0, -0.08, 0);
            double freeY = p.getY();
            Scp173MovementGate.handleMove(listener, new ServerboundMovePlayerPacket.PosRot(p.getX(), freeY + 0.42,
                    p.getZ(), p.getYRot(), p.getXRot(), false));
            h.assertTrue(p.getY() > freeY && p.getDeltaMovement().horizontalDistanceSqr() == 0,
                    "free client jump works without leaving a voluntary sprint kick in held server physics");
            p.setOnGround(true);
            Vec3 existingImpulse = new Vec3(0.7, 0.3, 0);
            p.setDeltaMovement(existingImpulse);
            Scp173MovementGate.handleMove(listener, new ServerboundMovePlayerPacket.PosRot(p.getX(), p.getY() + 0.42,
                    p.getZ(), p.getYRot(), p.getXRot(), false));
            h.assertTrue(p.getDeltaMovement().equals(existingImpulse), "ordinary jump bookkeeping retains existing external impulse");
            // Cross the horizontal FOV boundary in the final allowed physical step.
            p.setPos(observer.getX(), observer.getY(), observer.getZ() + 6);
            observer.setYRot(60);
            p.setDeltaMovement(1, -0.1, 0);
            h.assertTrue(Scp173Manager.held(p), "body surface initially inside horizontal FOV");
            before = p.position();
            listener.tick();
            h.assertTrue(p.getX() > before.x + 0.9 && !Scp173Manager.held(p),
                    "impulse crossing visibility boundary is retained when gaze releases during doTick");
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    org.example.maniacrevolution.effect.ModEffects.JUMP_COOLDOWN.get(), 2));
            listener.tick();
            h.assertTrue(p.hasEffect(org.example.maniacrevolution.effect.ModEffects.JUMP_COOLDOWN.get())
                    && p.getEffect(org.example.maniacrevolution.effect.ModEffects.JUMP_COOLDOWN.get()).getDuration() == 1,
                    "native player tick advances jump recovery timer");
            listener.tick();
            h.assertTrue(!p.hasEffect(org.example.maniacrevolution.effect.ModEffects.JUMP_COOLDOWN.get()),
                    "native player tick expires jump recovery without separate deadline");
            h.succeed();
        } finally {
            phase.setScore(previous);
            Scp173Manager.held(p);
            p.discard(); observer.discard();
            scoreboard.removePlayerFromTeam(p.getScoreboardName());
            scoreboard.removePlayerFromTeam(observer.getScoreboardName());
        }
    }
}
