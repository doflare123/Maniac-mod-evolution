package org.example.maniacrevolution.techies;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.example.maniacrevolution.downed.*;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.util.ManaUtil;

/** Real bootstrapped Forge managers/capabilities/geometry; synthetic tick inputs avoid physics noise. */
@GameTestHolder("maniacrev")
@PrefixGameTestTemplate(false)
public final class TechiesGameTests {
    @SuppressWarnings("unchecked") private static <T> T field(String name) {
        try { var f=TechiesManager.class.getDeclaredField(name); f.setAccessible(true); return (T)f.get(null); }
        catch(Exception e) { throw new AssertionError(e); }
    }
    private static Map<Integer,TechiesManager.Mine> mines() { return field("MINES"); }
    private static void mana(ServerPlayer p,float amount) { p.getCapability(ManaProvider.MANA).orElseThrow(()->new AssertionError("mana cap")).setMana(amount); }
    private static void add(TechiesManager.Mine mine) {
        try { var method=TechiesManager.class.getDeclaredMethod("add",TechiesManager.Mine.class); method.setAccessible(true); method.invoke(null,mine); }
        catch(Exception e) { throw new AssertionError(e); }
    }
    private static void readyAttack(ServerPlayer p) {
        for(Class<?> type=p.getClass();type!=null;type=type.getSuperclass()) try {
            var field=type.getDeclaredField("attackStrengthTicker"); field.setAccessible(true); field.setInt(p,100); return;
        } catch(NoSuchFieldException ignored) {} catch(Exception e) { throw new AssertionError(e); }
        throw new AssertionError("attack-strength field not found");
    }
    private static final class Scenario implements AutoCloseable {
        final GameTestHelper h; final List<ServerPlayer> players=new ArrayList<>(); final net.minecraft.world.scores.Score phase;
        final int oldPhase; final ServerPlayer owner,enemy; final BlockPos support; final long tick;
        Scenario(GameTestHelper h) {
            this.h=h; var sb=h.getLevel().getScoreboard();
            var obj=sb.getObjective("phaseGame"); if(obj==null) obj=sb.addObjective("phaseGame",ObjectiveCriteria.DUMMY,Component.literal("phase"),ObjectiveCriteria.RenderType.INTEGER);
            phase=sb.getOrCreatePlayerScore("game",obj); oldPhase=phase.getScore(); phase.setScore(3);
            GameManager.init(h.getLevel().getServer()); TechiesManager.clear(null); ((Map<?,?>)field("COOLDOWNS")).clear();
            support=h.absolutePos(new BlockPos(4,2,4)); h.getLevel().setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());
            owner=player("tc_owner","survivors",4,3,2); enemy=player("tc_enemy","maniac",12,3,12);
            var cls=sb.getObjective("SurvivorClass"); if(cls==null) cls=sb.addObjective("SurvivorClass",ObjectiveCriteria.DUMMY,Component.literal("class"),ObjectiveCriteria.RenderType.INTEGER);
            sb.getOrCreatePlayerScore(owner.getScoreboardName(),cls).setScore(org.example.maniacrevolution.character.survivor.TechiesClass.SCOREBOARD_ID);
            owner.setYRot(0); owner.setXRot((float)Math.toDegrees(Math.atan2(owner.getEyeY()-(support.getY()+1.002),2)));
            tick=h.getLevel().getServer().overworld().getGameTime();
        }
        ServerPlayer player(String name,String team,int x,int y,int z) {
            var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),name)) {
                @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return isInvulnerable(); }
                @Override public boolean canHarmPlayer(net.minecraft.world.entity.player.Player target) { return true; }
            };
            var pos=h.absolutePos(new BlockPos(x,y,z)); p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5); p.setGameMode(GameType.SURVIVAL);
            var sb=h.getLevel().getScoreboard(); var t=sb.getPlayerTeam(team); if(t==null) t=sb.addPlayerTeam(team); sb.addPlayerToTeam(p.getScoreboardName(),t);
            h.getLevel().addNewPlayer(p);
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,p,0,"f_8921_");
            players.add(p); return p;
        }
        void select(MineType type) { TechiesManager.action(owner,TechiesActionPacket.SELECT,type.ordinal()); }
        void place() { TechiesManager.action(owner,TechiesActionPacket.PLACE,0); }
        void update(long elapsed) { TechiesManager.update(h.getLevel().getServer(),players,tick+elapsed); }
        @Override public void close() {
            TechiesManager.clear(null); ((Map<?,?>)field("COOLDOWNS")).clear(); phase.setScore(oldPhase);
            for(var p:players) { h.getLevel().getScoreboard().removePlayerFromTeam(p.getScoreboardName()); p.discard(); }
        }
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void installationAndInventory(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            ItemStack original=new ItemStack(Items.DIAMOND,7); original.getOrCreateTag().putString("sentinel","unchanged"); s.owner.getInventory().setItem(0,original.copy());
            ItemStack remote=new ItemStack(org.example.maniacrevolution.ModItems.TECHIES_REMOTE.get());
            remote.setHoverName(Component.literal("Named remote")); remote.getOrCreateTag().putString("sentinel","remote NBT");
            s.owner.getInventory().setItem(8,remote.copy());
            s.select(MineType.RED); h.assertTrue(ManaUtil.getMana(s.owner)==100,"selection is free");
            s.place(); h.assertTrue(TechiesManager.placing(s.owner) && mines().size()==1 && ManaUtil.getMana(s.owner)==95,"red reservation and immediate 5 mana");
            s.place(); h.assertTrue(ManaUtil.getMana(s.owner)==95 && mines().size()==1,"duplicate packet cannot double-charge");
            var mine=mines().values().iterator().next(); s.owner.setYRot(40); s.owner.getInventory().selected=1;
            s.owner.setPos(s.owner.getX()+.001,s.owner.getY(),s.owner.getZ());
            s.update(19); h.assertTrue(TechiesManager.placing(s.owner) && !mine.placed,"turning/slot change do not cancel and no early completion");
            s.update(20); h.assertTrue(mine.placed && !TechiesManager.placing(s.owner),"red completes exactly at 20 ticks");
            h.assertTrue(ItemStack.matches(original,s.owner.getInventory().getItem(0)),"original count and NBT stay intact");
            int remotes=0;
            for(int slot=0;slot<s.owner.getInventory().getContainerSize();slot++)
                if(s.owner.getInventory().getItem(slot).is(org.example.maniacrevolution.ModItems.TECHIES_REMOTE.get())) remotes+=s.owner.getInventory().getItem(slot).getCount();
            h.assertTrue(remotes==1 && ItemStack.matches(remote,s.owner.getInventory().getItem(8)),"named remote is preserved without duplicate loadout");
            h.assertTrue(s.owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)==0,"helmet gives no armor");
            h.assertTrue(mines().size()==1 && TechiesManager.clear(null)==1 && mines().isEmpty(),"admin clear deletes without explosion");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void cancellationAndServerValidation(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            mana(s.owner,4); s.select(MineType.RED); s.place(); h.assertTrue(mines().isEmpty() && ManaUtil.getMana(s.owner)==4,"insufficient mana cannot reserve");
            mana(s.owner,100); s.owner.setXRot(0); s.place(); h.assertTrue(mines().isEmpty(),"wall/miss placement is rejected");
            s.owner.setXRot((float)Math.toDegrees(Math.atan2(s.owner.getEyeY()-(s.support.getY()+1.002),2))); s.place();
            h.assertTrue(TechiesManager.placing(s.owner),"valid placement begins");
            s.owner.setPos(s.owner.getX()+.05,s.owner.getY(),s.owner.getZ()); s.update(1);
            h.assertTrue(!TechiesManager.placing(s.owner) && mines().isEmpty() && ManaUtil.getMana(s.owner)==95,"movement cancels without refund");
            h.assertTrue(((Map<UUID,long[]>)field("COOLDOWNS")).get(s.owner.getUUID())[0]==s.tick+100,"cooldown survives cancellation");
            DownedCapability.get(s.owner).setState(DownedState.DOWNED); s.select(MineType.GREEN); s.place(); h.assertTrue(mines().isEmpty(),"downed cannot install");
            DownedCapability.get(s.owner).setState(DownedState.ALIVE); s.phase.setScore(0); s.select(MineType.GREEN); s.place(); h.assertTrue(mines().isEmpty(),"inactive match cannot install");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void activationWindows(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            s.select(MineType.RED); s.place(); s.update(20); var mine=mines().values().iterator().next();
            s.enemy.setPos(mine.center.x,mine.center.y,mine.center.z+1); s.update(21);
            h.assertTrue(mine.started==s.tick+21,"maniac triggers immediately");
            s.enemy.setPos(mine.center.x+8,mine.center.y,mine.center.z); s.update(24);
            h.assertTrue(mine.started==-1 && mines().containsKey(mine.id),"leaving at tick 3 cancels and preserves mine");
            s.enemy.setPos(mine.center.x,mine.center.y,mine.center.z+1); s.update(25); s.update(29);
            s.enemy.setPos(mine.center.x+8,mine.center.y,mine.center.z); s.update(30);
            h.assertTrue(mine.started==s.tick+25,"leaving after four ticks cannot cancel");
            s.update(34); h.assertTrue(mines().containsKey(mine.id),"no explosion at nine ticks");
            s.update(35); h.assertTrue(!mines().containsKey(mine.id),"explosion exactly at ten ticks");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void blueManaAndWalls(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            var a=s.player("tc_seven","survivors",4,3,5); var b=s.player("tc_fifteen","survivors",5,3,4);
            s.owner.setPos(s.support.getX()+.5,s.support.getY()+1,s.support.getZ()+.5);
            s.enemy.setPos(s.support.getX()+.5,s.support.getY()+1,s.support.getZ()+1.5);
            mana(s.owner,20); mana(a,7); mana(b,15); mana(s.enemy,30);
            var mine=new TechiesManager.Mine(500,s.owner,s.support,MineType.BLUE); mine.placed=true; add(mine);
            TechiesManager.explode(mine,h.getLevel().getServer(),s.players);
            h.assertTrue(ManaUtil.getMana(s.owner)==57 && ManaUtil.getMana(a)==0 && ManaUtil.getMana(b)==0 && ManaUtil.getMana(s.enemy)==15,"sum actual drain 7+15+15; own mana unaffected");
            var blueSlow=a.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            h.assertTrue(a.getHealth()==20 && blueSlow!=null && blueSlow.getAmplifier()==1 && blueSlow.getDuration()==40,"blue has no damage and slow II for two seconds");
            mana(s.owner,99); mana(a,7); mana(b,0); mana(s.enemy,0);
            TechiesManager.explode(new TechiesManager.Mine(501,s.owner,s.support,MineType.BLUE),h.getLevel().getServer(),s.players);
            h.assertTrue(ManaUtil.getMana(s.owner)==100 && ManaUtil.getMana(a)==0,"overflow discarded while full drain remains");
            a.setPos(s.support.getX()+.5,s.support.getY()+1,s.support.getZ()+2.2); mana(a,15);
            for(int y=1;y<=4;y++) h.getLevel().setBlockAndUpdate(s.support.offset(0,y,1),Blocks.STONE.defaultBlockState());
            TechiesManager.explode(new TechiesManager.Mine(502,s.owner,s.support,MineType.BLUE),h.getLevel().getServer(),List.of(a,s.owner));
            h.assertTrue(ManaUtil.getMana(a)==15,"solid wall blocks volume effect");
            for(int y=2;y<=4;y++) h.getLevel().setBlockAndUpdate(s.support.offset(0,y,1),Blocks.AIR.defaultBlockState());
            a.setPos(s.support.getX()+.5,s.support.getY()+2,s.support.getZ()+1.7); mana(a,15);
            TechiesManager.explode(new TechiesManager.Mine(503,s.owner,s.support,MineType.BLUE),h.getLevel().getServer(),List.of(a,s.owner));
            h.assertTrue(ManaUtil.getMana(a)==15,"an exposed head OUTSIDE the sphere cannot expose legs protected inside it");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void greenRemoteOwnershipAndDowned(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            s.select(MineType.GREEN); s.place(); var mine=mines().values().iterator().next();
            h.assertTrue(ManaUtil.getMana(s.owner)==90,"green costs ten immediately");
            s.update(99); h.assertTrue(!mine.placed,"green needs one hundred ticks"); s.update(100);
            h.assertTrue(mine.placed && mine.started<0,"green completes ready but never autonomously activates");
            s.enemy.setPos(mine.center.x,mine.center.y,mine.center.z); s.update(101); h.assertTrue(mine.started<0,"maniac does not trigger green");
            var other=s.player("tc_other_owner","survivors",7,3,4); other.getInventory().add(new ItemStack(org.example.maniacrevolution.ModItems.TECHIES_REMOTE.get()));
            var own=new TechiesManager.Mine(800,other,s.support.offset(3,0,0),MineType.GREEN); own.placed=true; add(own);
            ((Map<UUID,Set<Integer>>)field("GREENS")).computeIfAbsent(other.getUUID(),k->new HashSet<>()).add(own.id);
            s.owner.getInventory().selected=1; s.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(org.example.maniacrevolution.ModItems.TECHIES_REMOTE.get()));
            DownedCapability.get(s.owner).setState(DownedState.DOWNED);
            TechiesManager.action(s.owner,TechiesActionPacket.REMOTE,own.id); h.assertTrue(own.started<0 && mine.started<0,"forged foreign id rejected");
            TechiesManager.action(s.owner,TechiesActionPacket.REMOTE,mine.id);
            long activation=mine.started; h.assertTrue(activation>=0 && ManaUtil.getMana(s.owner)==90,"remote works in downed without charge");
            TechiesManager.update(h.getLevel().getServer(),s.players,activation+3); h.assertTrue(mines().containsKey(mine.id),"green not early at three ticks");
            TechiesManager.update(h.getLevel().getServer(),s.players,activation+4); h.assertTrue(!mines().containsKey(mine.id) && mines().containsKey(own.id),"one green explodes at four; other owner preserved");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void blueDurationReservationsAndDamage(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            s.select(MineType.BLUE); s.place(); var mine=mines().values().iterator().next();
            h.assertTrue(ManaUtil.getMana(s.owner)==80,"blue immediate twenty mana cost");
            var second=s.player("tc_second","survivors",4,3,2);
            var obj=h.getLevel().getScoreboard().getObjective("SurvivorClass"); h.getLevel().getScoreboard().getOrCreatePlayerScore(second.getScoreboardName(),obj).setScore(org.example.maniacrevolution.character.survivor.TechiesClass.SCOREBOARD_ID);
            second.setYRot(s.owner.getYRot()); second.setXRot(s.owner.getXRot());
            TechiesManager.action(second,TechiesActionPacket.SELECT,0); TechiesManager.action(second,TechiesActionPacket.PLACE,0);
            h.assertTrue(mines().size()==1 && ManaUtil.getMana(second)==100,"another owner cannot bypass reserved space");
            s.update(59); h.assertTrue(!mine.placed,"blue needs sixty ticks"); s.update(60); h.assertTrue(mine.placed,"blue completes at sixty");
            s.owner.setXRot((float)Math.toDegrees(Math.atan2(s.owner.getEyeY()-(s.support.getY()+1.002),2)));
            s.select(MineType.GREEN); s.place(); h.assertTrue(!TechiesManager.placing(s.owner),"all types share spacing");
            h.getLevel().setBlockAndUpdate(s.support,Blocks.AIR.defaultBlockState()); s.select(MineType.GREEN); s.place(); h.assertTrue(!TechiesManager.placing(s.owner),"missing support rejected");
        }
        try(var s=new Scenario(h)) {
            s.select(MineType.GREEN); s.place();
            TechiesManager.damaged(new net.minecraftforge.event.entity.living.LivingDamageEvent(s.owner,s.owner.damageSources().generic(),1));
            h.assertTrue(!TechiesManager.placing(s.owner) && mines().isEmpty() && ManaUtil.getMana(s.owner)==90,"received damage cancels without refund");
            s.select(MineType.BLUE); s.place(); h.assertTrue(TechiesManager.placing(s.owner),"independent blue cooldown remains ready");
            h.getLevel().setBlockAndUpdate(s.support,Blocks.AIR.defaultBlockState()); s.update(60);
            h.assertTrue(mines().isEmpty() && ManaUtil.getMana(s.owner)==70,"support loss during installation fails without refund");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void visibilitySpatialIndexAndLifecycle(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            s.select(MineType.RED); s.place(); s.update(20); var mine=mines().values().iterator().next();
            var known=(Map<UUID,Map<Integer,Integer>>)field("KNOWN");
            h.assertTrue(known.get(s.owner.getUUID()).containsKey(mine.id) && !known.get(s.enemy.getUUID()).containsKey(mine.id),"hidden mine is never sent to maniac");
            s.enemy.setPos(mine.center.x,mine.center.y,mine.center.z+1); s.update(21);
            h.assertTrue(known.get(s.enemy.getUUID()).containsKey(mine.id),"activation reveals server mine");
            s.enemy.setPos(mine.center.x+8,mine.center.y,mine.center.z); s.update(22);
            h.assertTrue(!known.get(s.enemy.getUUID()).containsKey(mine.id),"cancellation removes enemy data");
            TechiesManager.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(s.owner));
            h.assertTrue(mines().containsKey(mine.id),"logout preserves deployed mine");
            for(int i=0;i<4096;i++) { var m=new TechiesManager.Mine(10000+i,s.owner,s.support.offset(i*2,0,100),MineType.RED); m.placed=true; add(m); }
            h.assertTrue(mines().size()==4097,"no hard quantity limit");
            var index=(Map<?,?>)field("INDEX"); h.assertTrue(index.size()>100,"mines distributed among spatial cells");
            h.assertTrue(TechiesManager.clear(null)==4097 && index.isEmpty() && ((Set<?>)field("ACTIVE")).isEmpty(),"clear removes every spatial cell and pending activation");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void explosionDamageAndProtection(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            s.owner.setPos(s.support.getX()+.5,s.support.getY()+1,s.support.getZ()+.5); s.enemy.setPos(s.owner.getX(),s.owner.getY(),s.owner.getZ()+1);
            var red=new TechiesManager.Mine(901,s.owner,s.support,MineType.RED);
            s.phase.setScore(1); s.enemy.setInvulnerable(true); TechiesManager.explode(red,h.getLevel().getServer(),s.players);
            h.assertTrue(s.owner.getHealth()==17 && s.enemy.getHealth()==20,"red HP="+s.owner.getHealth()+", maniac HP="+s.enemy.getHealth()+"; expected 17/20 before reversal");
            var slow=s.owner.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            h.assertTrue(slow!=null && slow.getAmplifier()==1 && slow.getDuration()==40,"red starts with slow II for two seconds");
            s.phase.setScore(3); s.enemy.setInvulnerable(false); s.enemy.invulnerableTime=0;
            TechiesManager.explode(new TechiesManager.Mine(902,s.owner,s.support,MineType.GREEN),h.getLevel().getServer(),List.of(s.enemy));
            h.assertTrue(s.enemy.getHealth()==14,"green six damage after reversal");
            h.assertTrue(s.enemy.getDeltaMovement().horizontalDistanceSqr()>=8.99,"green approved impulse strength 3");
            slow=s.enemy.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            h.assertTrue(slow!=null && slow.getAmplifier()==3 && slow.getDuration()==60,"green slow IV for three seconds");
            DownedCapability.get(s.enemy).setState(DownedState.DOWNED); mana(s.enemy,50);
            TechiesManager.explode(new TechiesManager.Mine(903,s.owner,s.support,MineType.BLUE),h.getLevel().getServer(),s.players);
            h.assertTrue(ManaUtil.getMana(s.enemy)==50,"downed immunity retained for effects");
            h.assertTrue(h.getLevel().getBlockState(s.support).is(Blocks.STONE),"explosions never destroy supporting blocks");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void realProtectionAndMineImpulses(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            s.phase.setScore(1);
            s.enemy.setPos(s.support.getX()+.5,s.support.getY()+1,s.support.getZ()+.5);
            s.enemy.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            TechiesManager.explode(new TechiesManager.Mine(920,s.owner,s.support,MineType.RED),h.getLevel().getServer(),List.of(s.enemy));
            h.assertTrue(s.enemy.getHealth()==17,"team maniac in phase one is damaged unless actually invulnerable");
            h.assertTrue(s.enemy.getDeltaMovement().horizontalDistanceSqr()>=.99,"red pushes with approved strength 1 even exactly at mine center");
            s.enemy.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); mana(s.enemy,7); mana(s.owner,20);
            TechiesManager.explode(new TechiesManager.Mine(921,s.owner,s.support,MineType.BLUE),h.getLevel().getServer(),List.of(s.enemy,s.owner));
            h.assertTrue(s.enemy.getHealth()==17 && ManaUtil.getMana(s.enemy)==0,"blue steals actual mana and never damages");
            h.assertTrue(s.enemy.getDeltaMovement().horizontalDistanceSqr()>=.99,"blue pushes with strength 1 during ordinary damage immunity frames");
            s.enemy.setInvulnerable(true); s.enemy.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            TechiesManager.explode(new TechiesManager.Mine(922,s.owner,s.support,MineType.RED),h.getLevel().getServer(),List.of(s.enemy));
            h.assertTrue(s.enemy.getHealth()==17 && s.enemy.getDeltaMovement().lengthSqr()==0,"actual invulnerability blocks red damage and impulse");
            s.enemy.setInvulnerable(false); DownedCapability.get(s.enemy).setState(DownedState.DOWNED);
            TechiesManager.explode(new TechiesManager.Mine(923,s.owner,s.support,MineType.BLUE),h.getLevel().getServer(),List.of(s.enemy));
            h.assertTrue(s.enemy.getDeltaMovement().lengthSqr()==0,"downed blocks blue impulse");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void legalHitWindowAndRange(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            var mine=new TechiesManager.Mine(990,s.owner,s.support,MineType.RED); mine.placed=true; mine.started=s.tick; add(mine);
            s.enemy.setPos(mine.center.x,mine.center.y,mine.center.z+1); s.enemy.setYRot(180);
            s.enemy.setXRot((float)Math.toDegrees(Math.atan2(s.enemy.getEyeY()-(mine.center.y+.1),1))); readyAttack(s.enemy);
            s.enemy.setPos(mine.center.x,mine.center.y,mine.center.z+9);
            TechiesManager.action(s.enemy,TechiesActionPacket.HIT,mine.id); h.assertTrue(mines().containsKey(mine.id),"forged distant hit rejected");
            s.enemy.setPos(mine.center.x,mine.center.y,mine.center.z+1); mine.started=s.tick-4;
            TechiesManager.action(s.enemy,TechiesActionPacket.HIT,mine.id); h.assertTrue(mines().containsKey(mine.id),"tick four is irreversible");
            mine.started=s.tick-3; TechiesManager.action(s.enemy,TechiesActionPacket.HIT,mine.id);
            h.assertTrue(!mines().containsKey(mine.id) && s.enemy.getHealth()==20,"one legal hit in tick three destroys without explosion");
            var weak=new TechiesManager.Mine(992,s.owner,s.support,MineType.BLUE); weak.placed=true; weak.started=s.tick; add(weak);
            TechiesManager.action(s.enemy,TechiesActionPacket.HIT,weak.id);
            h.assertTrue(!mines().containsKey(weak.id),"weak but legal vanilla swing also destroys a mine; no invented strength threshold");
            var green=new TechiesManager.Mine(991,s.owner,s.support,MineType.GREEN); green.placed=true; green.started=s.tick; add(green); readyAttack(s.enemy);
            TechiesManager.action(s.enemy,TechiesActionPacket.HIT,green.id); h.assertTrue(mines().containsKey(green.id),"green cannot be destroyed by a hit");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void offlineManaAndAdministrativeCancellation(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            mana(s.owner,20); mana(s.enemy,7);
            s.enemy.setPos(s.support.getX()+.5,s.support.getY()+1,s.support.getZ()+1.5);
            var mine=new TechiesManager.Mine(995,s.owner,s.support,MineType.BLUE); mine.placed=true; add(mine);
            TechiesManager.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(s.owner));
            TechiesManager.explode(mine,h.getLevel().getServer(),List.of(s.enemy));
            h.assertTrue(ManaUtil.getMana(s.enemy)==0,"offline owner still drains actual target mana");
            TechiesManager.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(s.owner));
            h.assertTrue(ManaUtil.getMana(s.owner)==27,"offline transfer is delivered exactly once on return");
            TechiesManager.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(s.owner));
            h.assertTrue(ManaUtil.getMana(s.owner)==27,"repeat login never duplicates mana");
            mana(s.owner,100); s.select(MineType.GREEN); s.place(); h.assertTrue(TechiesManager.placing(s.owner),"new pending placement");
            TechiesManager.clear(null); s.update(100);
            h.assertTrue(!TechiesManager.placing(s.owner) && mines().isEmpty(),"admin clear cancels pending placement; no later mine appears");
            TechiesManager.resetMatch(h.getLevel().getServer());
            h.assertTrue(((Map<?,?>)field("COOLDOWNS")).isEmpty(),"new match reset removes all installation cooldowns");
            h.assertTrue(((Map<?,?>)field("OFFLINE_MANA")).isEmpty(),"match reset clears offline resource bookkeeping too");
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="techies",timeoutTicks=40)
    public static void computerQteExclusion(GameTestHelper h) {
        try(var s=new Scenario(h)) {
            var manager=org.example.maniacrevolution.hack.HackManager.get();
            try {
                manager.onPlayerActivate(s.owner,s.support,411);
                h.assertTrue(manager.isParticipatingInActiveHack(s.owner),"fixture participates in active computer/QTE");
                s.select(MineType.RED); s.place();
                h.assertTrue(!TechiesManager.placing(s.owner) && mines().isEmpty() && ManaUtil.getMana(s.owner)==100,"active hack/QTE rejects installation without charge");
                manager.resetById(null,411);
                s.select(MineType.RED); s.place(); h.assertTrue(TechiesManager.placing(s.owner),"placement after hack session is removed");
                manager.onPlayerActivate(s.owner,s.support,412);
                h.assertTrue(!manager.getActiveSessions().containsKey(s.support),"installing player cannot start a computer session");
            } finally { manager.resetById(null,411); manager.resetById(null,412); }
        }
        h.succeed();
    }
}
