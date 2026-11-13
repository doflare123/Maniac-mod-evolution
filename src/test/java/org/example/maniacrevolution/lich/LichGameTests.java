package org.example.maniacrevolution.lich;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.character.maniac.LichClass;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.downed.*;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.mana.ManaProvider;

@GameTestHolder("maniacrev_lich")
@PrefixGameTestTemplate(false)
public final class LichGameTests {
    @GameTest(template="scp173_empty",batch="lich_visual_clock",timeoutTicks=40)
    public static void visualClipsOutlastVanillaSwing(GameTestHelper h) {
        for(var kind:LichRules.Kind.values()) {
            var unit=ModEntities.LICH_UNIT.get().create(h.getLevel());unit.configure(UUID.randomUUID(),kind);
            unit.hurtTime=10;unit.swinging=true;
            h.assertTrue(LichUnitVisuals.frame(unit,0).clip().equals("emerge"),"emerge suppresses hurt and attack poses");
            var tag=new CompoundTag();unit.saveWithoutId(tag);tag.putInt("LichEmerge",0);unit.readAdditionalSaveData(tag);
            unit.hurtTime=0;unit.swinging=false;LichUnitVisuals.frame(unit,1);
            unit.swinging=true;unit.swingTime=0;
            String attack=kind==LichRules.Kind.SKELETON?"shoot":"attack";
            h.assertTrue(LichUnitVisuals.frame(unit,2).clip().equals(attack),"confirmed swing starts authored action");
            unit.swinging=false;
            h.assertTrue(LichUnitVisuals.frame(unit,9).clip().equals(attack),"action is not truncated to six vanilla swing ticks");
            h.assertTrue(LichUnitVisuals.frame(unit,40).clip().equals("idle"),"finished action returns to idle");
            unit.swinging=true;
            h.assertTrue(LichUnitVisuals.frame(unit,41).restart(),"next attack restarts one-shot");
            unit.setHealth(0);
            h.assertTrue(LichUnitVisuals.frame(unit,42).clip().equals("death"),"death interrupts visual attack");
            unit.discard();
        }
        h.succeed();
    }
    @GameTest(template="scp173_empty",batch="lich_combat",timeoutTicks=100)
    public static void weakMeleeAndStraightBolt(GameTestHelper h) throws Exception {
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_combat","maniac",2,2);
        var victim=player(h,"lich_combat_target","survivors",2,3);var ally=player(h,"lich_combat_ally","maniac",4,3);
        PlayerDataManager.get(p).setManiacClassId(14);
        var players=List.<ServerPlayer>of(p,victim,ally);LichManager.update(p.server,players);
        // Expire only the freshly constructed fixture's vanilla 3-second spawn shield.
        // Production damage must continue respecting all invulnerability rules.
        var spawnShield=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");spawnShield.setAccessible(true);
        spawnShield.setInt(victim,0);spawnShield.setInt(ally,0);
        var cow=net.minecraft.world.entity.EntityType.COW.create(p.serverLevel());
        cow.setNoAi(true);cow.setPos(p.position().add(1,0,0));p.serverLevel().addFreshEntity(cow);
        var bolts=new ArrayList<LichBolt>();
        try {
            var s=LichManager.owner(p.getUUID());
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            float hp=victim.getHealth();
            var attack=new net.minecraftforge.event.entity.player.AttackEntityEvent(p,victim);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(attack);
            h.assertTrue(attack.isCanceled()&&victim.getHealth()==hp,"vanilla attack cannot bypass the custom Lich swing");
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,victim.getEyePosition());
            h.assertTrue(LichCombat.melee(p),"custom Lich swing is accepted");
            h.assertTrue(Math.abs(victim.getHealth()-(hp-.5f))<.001,"custom Lich melee deals exactly half HP to a player");
            h.assertTrue(s.melee==p.server.getTickCount()+40,"melee interval is two seconds");
            h.assertTrue(!LichCombat.melee(p)&&Math.abs(victim.getHealth()-(hp-.5f))<.001,"repeat melee during cooldown is rejected entirely");
            s.melee=0;hp=cow.getHealth();
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,cow.getEyePosition());
            LichCombat.melee(p);
            h.assertTrue(Math.abs(cow.getHealth()-(hp-.5f))<.001,"melee also works on an ordinary mob");
            s.melee=0;p.setXRot(-90);hp=cow.getHealth();
            h.assertTrue(LichCombat.melee(p)&&s.melee==p.server.getTickCount()+40,"a miss also starts the complete two-second swing cycle");
            h.assertTrue(!LichCombat.melee(p)&&cow.getHealth()==hp,"spamming a miss cannot start additional swings");
            s.melee=0;victim.setPos(p.position().add(0,0,2.5));victim.invulnerableTime=0;hp=victim.getHealth();
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,victim.getEyePosition());
            var meleeWall=p.blockPosition().offset(0,1,1);p.serverLevel().setBlockAndUpdate(meleeWall,Blocks.STONE.defaultBlockState());
            h.assertTrue(LichCombat.melee(p)&&victim.getHealth()==hp,"custom melee swing cannot hit through a wall");
            p.serverLevel().removeBlock(meleeWall,false);
            cow.setPos(p.position().add(10,0,0));
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(ModItems.LICH_BOOK.get()));
            p.getCapability(ManaProvider.MANA).ifPresent(m->{m.setMaxMana(10);m.setMana(3);});
            s.melee=0;h.assertTrue(LichCombat.shoot(p),"book fires a projectile");
            h.assertTrue(p.getCapability(ManaProvider.MANA).orElseThrow(IllegalStateException::new).getMana()==2,"one mana spent per projectile");
            h.assertTrue(!LichCombat.shoot(p)&&p.getCapability(ManaProvider.MANA).orElseThrow(IllegalStateException::new).getMana()==2,"duplicate shot on cooldown does not spend mana");
            var bolt=p.serverLevel().getEntitiesOfClass(LichBolt.class,p.getBoundingBox().inflate(5)).stream().filter(e->e.getOwner()==p).findFirst().orElseThrow();bolts.add(bolt);
            bolt.setPos(p.position().add(0,6,0));bolt.setDeltaMovement(0,0,1.5);double height=bolt.getY();
            for(int i=0;i<3;i++)bolt.tick();
            h.assertTrue(bolt.isNoGravity()&&Math.abs(bolt.getY()-height)<.0001&&bolt.getDeltaMovement().y==0,"projectile flies straight without gravity");
            victim.invulnerableTime=0;hp=victim.getHealth();victim.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            var motion=new net.minecraft.world.phys.Vec3(.03,.08,-.02);victim.setDeltaMovement(motion);
            bolt.setPos(victim.getX(),victim.getY()+1,victim.getZ()-.7);bolt.setDeltaMovement(0,0,1.5);bolt.tick();
            h.assertTrue(bolt.isRemoved()&&Math.abs(victim.getHealth()-(hp-.5f))<.001,"swept projectile collision deals half HP to player");
            var slow=victim.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            h.assertTrue(slow!=null&&slow.getDuration()==20&&slow.getAmplifier()==0,"projectile applies Slowness I for exactly one second");
            h.assertTrue(victim.getDeltaMovement().equals(motion),"projectile does not alter victim velocity");
            for(var target:List.<net.minecraft.world.entity.LivingEntity>of(cow,ally)) {
                target.invulnerableTime=0;hp=target.getHealth();
                var shot=ModEntities.LICH_BOLT.get().create(p.serverLevel());shot.setOwner(p);bolts.add(shot);
                shot.onHit(new net.minecraft.world.phys.EntityHitResult(target));
                h.assertTrue(shot.isRemoved()&&Math.abs(target.getHealth()-(hp-.5f))<.001,"projectile has no team or mob whitelist");
            }
            cow.invulnerableTime=0;cow.setInvulnerable(true);cow.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);hp=cow.getHealth();
            var immune=ModEntities.LICH_BOLT.get().create(p.serverLevel());immune.setOwner(p);bolts.add(immune);
            immune.onHit(new net.minecraft.world.phys.EntityHitResult(cow));
            h.assertTrue(cow.getHealth()==hp&&!cow.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),"invulnerability still rejects damage and slowness");
            s.melee=0;p.getCapability(ManaProvider.MANA).ifPresent(m->m.setMana(0));
            h.assertTrue(!LichCombat.shoot(p),"no mana means no projectile");
            var feedback=LichCombat.feedback(p);
            h.assertTrue(feedback.getString("failure").equals("no_mana")&&!feedback.getBoolean("swing")&&feedback.getInt("cooldown")==0,
                    "attempt without mana sends an explicit warning without a fake swing or cooldown");
            victim.setPos(p.position().add(0,0,5));victim.invulnerableTime=0;hp=victim.getHealth();
            var wall=p.blockPosition().offset(0,1,3);p.serverLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
            var blocked=ModEntities.LICH_BOLT.get().create(p.serverLevel());blocked.setOwner(p);blocked.setPos(p.getX(),p.getY()+1.3,p.getZ()+.8);blocked.setDeltaMovement(0,0,6);p.serverLevel().addFreshEntity(blocked);bolts.add(blocked);
            blocked.tick();h.assertTrue(blocked.isRemoved()&&victim.getHealth()==hp,"projectile cannot tunnel through a solid wall");p.serverLevel().removeBlock(wall,false);
            h.succeed();
        } finally {
            bolts.forEach(net.minecraft.world.entity.Entity::discard);cow.discard();phase.setScore(0);LichManager.update(p.server,List.of());
            for(var player:players) {player.discard();player.getScoreboard().removePlayerFromTeam(player.getScoreboardName());}
        }
    }
    @GameTest(template="scp173_empty",batch="lich_backtracking_maze",timeoutTicks=100)
    public static void leavesDeepDeadEndByWalkingAwayFromGoal(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_maze","maniac",1,1);
        PlayerDataManager.get(p).setManiacClassId(14);LichManager.update(p.server,List.of(p));
        var units=new ArrayList<LichUnit>();
        try {
            for(int x=0;x<=32;x++)for(int z=0;z<=27;z++)for(int y=20;y<=26;y++)h.setBlock(x,y,z,y==20?Blocks.STONE:Blocks.AIR);
            // Deep U: the target is six blocks east, but the only exit is eighteen blocks south.
            for(int y=21;y<=24;y++) {
                for(int z=3;z<=22;z++) {h.setBlock(11,y,z,Blocks.EMERALD_BLOCK);h.setBlock(16,y,z,Blocks.EMERALD_BLOCK);}
                for(int x=11;x<=16;x++)h.setBlock(x,y,3,Blocks.EMERALD_BLOCK);
            }
            var start=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(14,21,5)));
            var goal=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(20,21,5)));
            for(var kind:List.of(LichRules.Kind.GHOUL,LichRules.Kind.BUTCHER)) {
                var unit=ModEntities.LICH_UNIT.get().create(h.getLevel());units.add(unit);unit.setPos(start);unit.configure(p.getUUID(),kind);
                h.getLevel().addFreshEntity(unit);var tag=new CompoundTag();unit.saveWithoutId(tag);tag.putInt("LichEmerge",0);unit.readAdditionalSaveData(tag);
                unit.command(LichRules.Order.MOVE,goal,null);boolean usedExit=false;int reaction=-1;
                for(int tick=0;tick<1500;tick++) {
                    ticks(unit,1);usedExit|=unit.getZ()>=h.absolutePos(new BlockPos(0,0,23)).getZ();
                    if(reaction<0&&unit.position().distanceToSqr(start)>.04)reaction=tick+1;
                    if(unit.order==LichRules.Order.STAND)break;
                }
                h.assertTrue(usedExit&&LichNavigation.arrived(unit.position(),goal,1)&&unit.order==LichRules.Order.STAND,
                        kind+" must walk away from goal through the rear exit, then reach it: "+unit.position());
                h.assertTrue(reaction>0&&reaction<=20,"dead-end planning should start movement within one second: "+kind+" ticks="+reaction);
                unit.discard();
            }
            // Four alternating gates require a serpentine route far longer than the direct distance.
            for(int x=0;x<=32;x++)for(int z=0;z<=27;z++)for(int y=21;y<=24;y++)h.setBlock(x,y,z,Blocks.AIR);
            for(int y=21;y<=24;y++) {
                for(int z=1;z<=26;z++) {h.setBlock(1,y,z,Blocks.EMERALD_BLOCK);h.setBlock(31,y,z,Blocks.EMERALD_BLOCK);}
                for(int x=1;x<=31;x++) {h.setBlock(x,y,1,Blocks.EMERALD_BLOCK);h.setBlock(x,y,26,Blocks.EMERALD_BLOCK);}
                for(int gate=0;gate<4;gate++)for(int z=2;z<=25;z++)
                    if(gate%2==0?z<23:z>4)h.setBlock(6+gate*6,y,z,Blocks.EMERALD_BLOCK);
            }
            start=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,21,4)));
            goal=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(29,21,4)));
            for(var kind:LichRules.Kind.values()) {
                var unit=ModEntities.LICH_UNIT.get().create(h.getLevel());units.add(unit);unit.setPos(start);unit.configure(p.getUUID(),kind);
                h.getLevel().addFreshEntity(unit);var tag=new CompoundTag();unit.saveWithoutId(tag);tag.putInt("LichEmerge",0);unit.readAdditionalSaveData(tag);
                unit.command(LichRules.Order.MOVE,goal,null);boolean visitedFarEnd=false;int reaction=-1;
                for(int tick=0;tick<3200;tick++) {
                    ticks(unit,1);visitedFarEnd|=unit.getZ()>=h.absolutePos(new BlockPos(0,0,23)).getZ();
                    if(reaction<0&&unit.position().distanceToSqr(start)>.04)reaction=tick+1;
                    if(unit.order==LichRules.Order.STAND)break;
                }
                h.assertTrue(visitedFarEnd&&LichNavigation.arrived(unit.position(),goal,1)&&unit.order==LichRules.Order.STAND,
                        kind+" must traverse all four alternating maze gates: "+unit.position());
                h.assertTrue(reaction>0&&reaction<=20,"maze planning should start movement within one second: "+kind+" ticks="+reaction);
                org.example.maniacrevolution.Maniacrev.LOGGER.info("LICH_MAZE_REACTION: {} started in {} ticks",kind,reaction);unit.discard();
            }
            h.succeed();
        } finally {units.forEach(net.minecraft.world.entity.Entity::discard);phase.setScore(0);LichManager.update(p.server,List.of());p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());}
    }
    @GameTest(template="scp173_empty",batch="lich_combat_balance",timeoutTicks=100)
    public static void openingBurstButcherAndRangedSkeleton(GameTestHelper h) {
        var world=h.getLevel();var time=(net.minecraft.world.level.storage.ServerLevelData)world.getLevelData();long clock=world.getGameTime();
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_balance","maniac",1,1);
        var target=player(h,"lich_balance_target","survivors",2,2);
        PlayerDataManager.get(p).setManiacClassId(14);LichManager.update(p.server,List.of(p,target));
        var units=new ArrayList<LichUnit>();var arrows=new ArrayList<net.minecraft.world.entity.projectile.Arrow>();
        try {
            for(int x=0;x<=36;x++)for(int z=0;z<=6;z++)for(int y=20;y<=30;y++)h.setBlock(x,y,z,y==20?Blocks.STONE:Blocks.AIR);
            var base=h.absolutePos(new BlockPos(0,21,0));
            for(var kind:LichRules.Kind.values()) {
                time.setGameTime(clock+kind.ordinal()*100);
                var unit=ModEntities.LICH_UNIT.get().create(world);units.add(unit);
                unit.setPos(base.getX()+2.5,base.getY(),base.getZ()+2.5);unit.configure(p.getUUID(),kind);world.addFreshEntity(unit);
                var tag=new CompoundTag();unit.saveWithoutId(tag);tag.putInt("LichEmerge",0);unit.readAdditionalSaveData(tag);
                unit.command(LichRules.Order.STAND,unit.position(),null);unit.nextAttack=Long.MAX_VALUE;
                target.setPos(base.getX()+5.5,base.getY(),base.getZ()+2.5);
                h.assertTrue(unit.aggroSpeed()==1,"idle unit has no opening bonus");ticks(unit,5);
                h.assertTrue(unit.detecting&&unit.aggroSpeed()==1.25,"every undead type gains 25 percent on first detection");
                unit.setOnGround(true);unit.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);unit.setYRot(0);
                var control=unit.getMoveControl();double expected=kind.speed*1.25*(kind==LichRules.Kind.GHOUL?.8:1);
                for(int i=0;i<3;i++) {
                    control.setWantedPosition(unit.getX(),unit.getY(),unit.getZ()+10,1);control.tick();
                    h.assertTrue(Math.abs(unit.getSpeed()-expected)<.0001,"opening speed multiplies the base/ghoul run-up once: "+kind);
                }
                time.setGameTime(clock+kind.ordinal()*100+60);
                h.assertTrue(unit.aggroSpeed()==1,"opening bonus ends after three seconds");
                h.assertTrue(Math.abs(unit.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-kind.speed)<.0001,"base attribute is preserved");
                unit.discard();
            }
            h.assertTrue(Math.abs(LichRules.Kind.BUTCHER.speed-.21)<.0001,"butcher has the increased base movement speed");
            var burst=new LichAggroBurst();burst.see(100);burst.see(130);
            h.assertTrue(burst.multiplier(159)==1.25&&burst.multiplier(160)==1,"repeated visibility does not extend burst");
            burst.tick(200,false);burst.see(200);
            h.assertTrue(burst.multiplier(200)==1,"brief disengagement or another target does not grant another opening");
            burst.tick(301,false);burst.see(301);h.assertTrue(burst.multiplier(301)==1.25,"a later separate encounter can grant another burst");
            time.setGameTime(clock+1000);
            var skeleton=ModEntities.LICH_UNIT.get().create(world);units.add(skeleton);
            skeleton.setPos(base.getX()+2.5,base.getY(),base.getZ()+2.5);skeleton.configure(p.getUUID(),LichRules.Kind.SKELETON);world.addFreshEntity(skeleton);
            var tag=new CompoundTag();skeleton.saveWithoutId(tag);tag.putInt("LichEmerge",0);skeleton.readAdditionalSaveData(tag);
            target.setPos(base.getX()+28.5,base.getY(),base.getZ()+2.5);
            var previous=world.getEntitiesOfClass(net.minecraft.world.entity.projectile.Arrow.class,skeleton.getBoundingBox().inflate(40)).stream().map(net.minecraft.world.entity.Entity::getId).collect(java.util.stream.Collectors.toSet());
            skeleton.command(LichRules.Order.CHASE,target.position(),target.getUUID());ticks(skeleton,5);
            h.assertTrue(skeleton.isUsingItem()&&skeleton.bowStart()>=0&&!skeleton.swinging,"server starts a synchronized bow draw without a vanilla melee swing");
            var drawing=LichUnitVisuals.frame(skeleton,skeleton.tickCount);
            h.assertTrue(drawing.clip().equals("shoot")&&drawing.seconds()<.05,"authored shoot clip starts before the arrow");
            h.assertTrue(world.getEntitiesOfClass(net.minecraft.world.entity.projectile.Arrow.class,skeleton.getBoundingBox().inflate(40)).stream().allMatch(a->previous.contains(a.getId())),"bow wind-up does not release the arrow early");
            time.setGameTime(clock+1000+LichUnit.BOW_DRAW_TICKS);ticks(skeleton,1);
            var release=LichUnitVisuals.frame(skeleton,skeleton.tickCount);
            h.assertTrue(release.clip().equals("shoot")&&Math.abs(release.seconds()-.3)<.001&&!skeleton.isUsingItem(),"arrow release occurs at the raised-bow pose");
            var fired=world.getEntitiesOfClass(net.minecraft.world.entity.projectile.Arrow.class,skeleton.getBoundingBox().inflate(40)).stream().filter(a->!previous.contains(a.getId())&&a.getPersistentData().getBoolean("lich_arrow")).findFirst();
            h.assertTrue(skeleton.detecting&&fired.isPresent(),"skeleton really fires at an assigned target 26 blocks away");fired.ifPresent(arrows::add);
            skeleton.nextAttack=Long.MAX_VALUE;target.setPos(base.getX()+32.5,base.getY(),base.getZ()+2.5);ticks(skeleton,5);
            h.assertTrue(skeleton.detecting&&skeleton.combatTarget.equals(target.getUUID()),"skeleton retains a visible target at 30 blocks");
            // A newly started draw is canceled when the target disappears before release.
            target.setPos(base.getX()+28.5,base.getY(),base.getZ()+2.5);
            skeleton.nextAttack=clock+1050;time.setGameTime(clock+1044);ticks(skeleton,5);
            h.assertTrue(skeleton.isUsingItem(),"next shot starts another draw");
            target.setPos(base.getX()+80,base.getY(),base.getZ()+2.5);time.setGameTime(clock+1045);ticks(skeleton,1);
            h.assertTrue(!skeleton.isUsingItem()&&skeleton.bowStart()<0,"lost target cancels the bow pose before firing");
            // Verify the actual vanilla arrow trajectory, not just the aim helper's calculation.
            for(int range:new int[]{5,14,28})for(int rise:new int[]{-3,0,3}) {
                var probe=new net.minecraft.world.entity.projectile.Arrow(world,base.getX()+2.5,base.getY()+4,base.getZ()+5.5);arrows.add(probe);
                var direction=LichArrowAim.direction(new net.minecraft.world.phys.Vec3(range,rise,0));
                probe.shoot(direction.x,direction.y,direction.z,LichArrowAim.SPEED,0);
                double plane=probe.getX()+range,wantedY=probe.getY()+rise,error=Double.POSITIVE_INFINITY;
                for(int i=0;i<80&&!probe.isRemoved();i++) {
                    var before=probe.position();probe.tick();
                    if(probe.getX()>=plane) {
                        double fraction=(plane-before.x)/(probe.getX()-before.x);
                        error=Math.abs(before.y+(probe.getY()-before.y)*fraction-wantedY);break;
                    }
                }
                h.assertTrue(error<.05,"real arrow reaches the desired range/height before spread: "+range+"/"+rise+" error="+error);
            }
            h.succeed();
        } finally {
            arrows.forEach(net.minecraft.world.entity.Entity::discard);units.forEach(net.minecraft.world.entity.Entity::discard);time.setGameTime(clock);
            phase.setScore(0);LichManager.update(p.server,List.of());for(var player:List.of(p,target)){player.discard();player.getScoreboard().removePlayerFromTeam(player.getScoreboardName());}
        }
    }
    @GameTest(template="scp173_empty",batch="lich_directional_search",timeoutTicks=100)
    public static void remembersDirectionAndFindsTargetAroundCover(GameTestHelper h) {
        var world=h.getLevel();long clock=world.getGameTime();
        var time=(net.minecraft.world.level.storage.ServerLevelData)world.getLevelData();
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_search","maniac",1,1);
        var target=player(h,"lich_search_target","survivors",2,2);
        PlayerDataManager.get(p).setManiacClassId(14);LichManager.update(p.server,List.of(p,target));
        var unit=ModEntities.LICH_UNIT.get().create(world);
        try {
            var memory=new LichPursuit();var id=UUID.randomUUID();
            memory.observe(id,new net.minecraft.world.phys.Vec3(0,21,0),net.minecraft.world.phys.Vec3.ZERO,10);
            memory.observe(id,new net.minecraft.world.phys.Vec3(0,21,1),net.minecraft.world.phys.Vec3.ZERO,15);
            h.assertTrue(memory.direction().z>.99&&memory.candidate(0).z==4&&memory.candidate(1).z==6,"visible movement predicts three/five blocks ahead");
            memory.lose(20);memory.lose(100);
            h.assertTrue(!memory.expired(180)&&memory.approaching(),"travel to last observation does not consume the five-second search");
            memory.startSearch(200);
            h.assertTrue(!memory.expired(299)&&memory.expired(300),"search lasts five seconds after arrival");
            memory.observe(id,memory.position(),net.minecraft.world.phys.Vec3.ZERO,301);
            h.assertTrue(memory.direction().equals(net.minecraft.world.phys.Vec3.ZERO)&&memory.candidate(0).distanceToSqr(memory.position())==4,
                    "stationary target leads to local search instead of stale running direction");
            for(int x=0;x<=20;x++)for(int z=0;z<=15;z++)for(int y=20;y<=25;y++)h.setBlock(x,y,z,y==20?Blocks.STONE:Blocks.AIR);
            for(int z=0;z<=7;z++)for(int y=21;y<=24;y++)h.setBlock(9,y,z,Blocks.STONE);
            var base=h.absolutePos(new BlockPos(0,21,0));
            unit.setPos(base.getX()+3.5,base.getY(),base.getZ()+4.5);unit.configure(p.getUUID(),LichRules.Kind.ZOMBIE);
            world.addFreshEntity(unit);var tag=new CompoundTag();unit.saveWithoutId(tag);tag.putInt("LichEmerge",0);unit.readAdditionalSaveData(tag);
            p.setPos(unit.position().add(0,0,-2));
            unit.command(LichRules.Order.FOLLOW,p.position(),null);unit.nextAttack=Long.MAX_VALUE;
            target.setPos(base.getX()+6.5,base.getY(),base.getZ()+5);
            for(int i=1;i<=15;i++) {
                target.setPos(target.position().add(0,0,.1));time.setGameTime(clock+i);ticks(unit,1);
            }
            h.assertTrue(unit.detecting&&unit.lastSeen!=null,"target is first observed moving toward the corner");
            var observed=unit.lastSeen;
            target.setPos(base.getX()+11.5,base.getY(),base.getZ()+8.5);
            h.assertTrue(!unit.hasLineOfSight(target),"fixture actually hides target behind the wall");
            p.setPos(base.getX()+18.5,base.getY(),base.getZ()+14.5);
            for(int i=16;i<=20;i++) {time.setGameTime(clock+i);ticks(unit,1);}
            h.assertTrue(!unit.detecting&&unit.lastSeen.equals(observed)&&unit.combatTarget.equals(target.getUUID()),
                    "occlusion and a moving owner preserve observation and target without updating hidden coordinates");
            boolean found=false;var trace=new StringBuilder();
            for(int i=21;i<=240;i++) {
                time.setGameTime(clock+i);ticks(unit,1);
                if(i%20==0)trace.append(i).append(":").append(unit.position()).append(" -> ").append(unit.getNavigation().getTargetPos())
                        .append(" until=").append(unit.searchUntil).append(" last=").append(unit.lastSeen).append(";");
                if(unit.detecting) {found=true;break;}
            }
            h.assertTrue(found&&unit.lastSeen.equals(target.position()),"directional search moves around cover and reacquires target: "+unit.position()+" observed="+observed+" target="+target.position()+" trace="+trace);
            // Repeat with no way to see the target again: search must finish and restore the order.
            unit.setPos(base.getX()+3.5,base.getY(),base.getZ()+12.5);unit.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            unit.command(LichRules.Order.STAND,unit.position(),null);
            target.setPos(base.getX()+6.5,base.getY(),base.getZ()+12.5);
            for(int i=250;i<=260;i++) {time.setGameTime(clock+i);ticks(unit,1);}
            h.assertTrue(unit.detecting,"stationary target is observed before the second loss");
            target.setPos(base.getX()+60,base.getY(),base.getZ()+60);long deadline=0;boolean expired=false;
            for(int i=261;i<=500;i++) {
                time.setGameTime(clock+i);ticks(unit,1);
                if(unit.searchUntil>0)deadline=unit.searchUntil;
                if(unit.combatTarget==null) {expired=true;break;}
            }
            h.assertTrue(deadline>0&&expired&&world.getGameTime()>=deadline&&unit.lastSeen==null&&unit.order==LichRules.Order.STAND,
                    "unresolved search expires and returns to the original stand order");
            h.succeed();
        } finally {
            unit.discard();time.setGameTime(clock);phase.setScore(0);LichManager.update(p.server,List.of());
            for(var player:List.of(p,target)) {player.discard();player.getScoreboard().removePlayerFromTeam(player.getScoreboardName());}
        }
    }
    @GameTest(template="scp173_empty",batch="lich_navigation",timeoutTicks=100)
    public static void routesAroundWallsAndBetweenFloors(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_navigation","maniac",1,1);
        PlayerDataManager.get(p).setManiacClassId(14);LichManager.update(p.server,List.of(p));
        var units=new ArrayList<LichUnit>();
        try {
            for(int x=0;x<=24;x++)for(int z=0;z<=16;z++)for(int y=20;y<=30;y++)
                h.setBlock(x,y,z,y==20?Blocks.STONE:Blocks.AIR);
            for(int z=0;z<=7;z++)for(int y=21;y<=24;y++)h.setBlock(8,y,z,Blocks.STONE);
            var unit=ModEntities.LICH_UNIT.get().create(p.serverLevel());
            var start=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,21,3)));
            unit.setPos(start);unit.configure(p.getUUID(),LichRules.Kind.ZOMBIE);p.serverLevel().addFreshEntity(unit);units.add(unit);
            var tag=new CompoundTag();unit.saveWithoutId(tag);tag.putInt("LichEmerge",0);unit.readAdditionalSaveData(tag);
            var goal=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(14,21,3)));
            unit.command(LichRules.Order.MOVE,goal,null);
            boolean wentAround=false;
            for(int i=0;i<300;i++) {ticks(unit,1);if(unit.getZ()>=h.absolutePos(new BlockPos(0,0,8)).getZ())wentAround=true;}
            h.assertTrue(wentAround&&LichNavigation.arrived(unit.position(),goal,1)&&unit.order==LichRules.Order.STAND,
                    "unit physically follows an indirect path around the wall: "+unit.position());
            unit.setPos(start);unit.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);unit.setOnGround(true);
            unit.command(LichRules.Order.MOVE,goal,null);ticks(unit,6);
            for(int z=0;z<=7;z++)for(int y=21;y<=24;y++)h.setBlock(5,y,z,Blocks.STONE);
            ticks(unit,340);
            h.assertTrue(LichNavigation.arrived(unit.position(),goal,1),"a newly blocked path is rebuilt and the unit still arrives: "+unit.position());
            // A stair of full blocks leads to a four-block-high platform.
            for(int x=4;x<=8;x++)for(int y=21;y<=21+x-4;y++)for(int z=11;z<=13;z++)h.setBlock(x,y,z,Blocks.STONE);
            for(int x=8;x<=16;x++)for(int z=11;z<=13;z++)h.setBlock(x,25,z,Blocks.STONE);
            unit.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,21,12))));
            unit.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);unit.setOnGround(true);
            goal=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(14,26,12)));
            unit.command(LichRules.Order.MOVE,goal,null);ticks(unit,320);
            h.assertTrue(LichNavigation.arrived(unit.position(),goal,1)&&unit.order==LichRules.Order.STAND,
                    "unit reaches the requested upper floor using stairs: "+unit.position());
            // A point directly overhead is not reached merely by standing below it.
            unit.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(20,21,12))));
            unit.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);unit.setOnGround(true);
            goal=unit.position().add(0,7,0);unit.command(LichRules.Order.MOVE,goal,null);ticks(unit,120);
            h.assertTrue(unit.order==LichRules.Order.MOVE&&!LichNavigation.arrived(unit.position(),goal,1)&&unit.getY()<goal.y-3,
                    "unreachable height neither completes the order nor teleports the mob");
            h.assertTrue(!LichNavigation.arrived(goal.add(0,-2,0),goal,9),"follow distance cannot hide a floor mismatch");
            for(int x=25;x<=54;x++)for(int z=14;z<=16;z++)for(int y=20;y<=24;y++)h.setBlock(x,y,z,y==20?Blocks.STONE:Blocks.AIR);
            unit.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4,21,15))));
            unit.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);unit.setOnGround(true);
            goal=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(50,21,15)));
            unit.command(LichRules.Order.MOVE,goal,null);ticks(unit,650);
            h.assertTrue(LichNavigation.arrived(unit.position(),goal,1)&&unit.order==LichRules.Order.STAND,
                    "partial routes continue toward a destination beyond the planning radius: "+unit.position());
            h.succeed();
        } finally {units.forEach(net.minecraft.world.entity.Entity::discard);phase.setScore(0);LichManager.update(p.server,List.of());p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());}
    }
    @GameTest(template="scp173_empty",batch="lich_adventure_place",timeoutTicks=40)
    public static void adventurePhylacteryPlacement(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_adventure","maniac",2,2);
        PlayerDataManager.get(p).setManiacClassId(14);p.setGameMode(GameType.ADVENTURE);LichManager.update(p.server,List.of(p));
        try {
            var army=LichManager.owner(p.getUUID());var block=h.absolutePos(new BlockPos(2,1,2));
            h.setBlock(2,1,2,Blocks.STONE);h.setBlock(2,2,2,Blocks.AIR);h.setBlock(2,3,2,Blocks.AIR);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(ModItems.LICH_PHYLACTERY.get()));
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(block).add(0,.5,0),Direction.UP,block,false);
            h.assertTrue(!p.getAbilities().mayBuild&&!p.getMainHandItem().hasTag(),"real adventure fixture has no CanPlaceOn permission");
            var canceled=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.MAIN_HAND,block,hit);
            canceled.setCanceled(true);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(canceled);
            h.assertTrue(army.phylactery==null,"pre-canceled block interaction must not place a phylactery");
            var side=new net.minecraft.world.phys.BlockHitResult(hit.getLocation(),Direction.EAST,block,false);
            p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,side);
            h.assertTrue(army.phylactery==null&&p.getMainHandItem().is(ModItems.LICH_PHYLACTERY.get()),"side click preserves carried phylactery");
            h.setBlock(2,2,2,Blocks.STONE);
            p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(army.phylactery==null,"occupied space still rejects entity placement");h.setBlock(2,2,2,Blocks.AIR);
            var result=p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(result.consumesAction()&&army.phylactery!=null&&!army.phylactery.isRemoved()
                    &&army.phylactery.owner.equals(p.getUUID()),"normal adventure block-use path places the owned phylactery");
            h.assertTrue(!p.getInventory().contains(new net.minecraft.world.item.ItemStack(ModItems.LICH_PHYLACTERY.get()))
                    &&p.level().getBlockState(block).is(Blocks.STONE)&&p.level().getBlockState(block.above()).isAir(),
                    "placement consumes carried item and does not modify blocks");
            var placed=army.phylactery;
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(ModItems.LICH_PHYLACTERY.get()));
            p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(army.phylactery==placed,"repeated use cannot duplicate placed phylactery");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT));
            p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(p.level().getBlockState(block.above()).isAir(),"ordinary adventure block placement stays forbidden");
            h.succeed();
        } finally {phase.setScore(0);LichManager.update(p.server,List.of());p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());}
    }
    @GameTest(template="scp173_empty",batch="lich_movement",timeoutTicks=80)
    public static void idleUndeadFloatAndGhoulRun(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_movement","maniac",2,2);
        PlayerDataManager.get(p).setManiacClassId(14);LichManager.update(p.server,List.of(p));
        var units=new ArrayList<LichUnit>();
        try {
            // Deep still pool, no survivors or combat targets. Each actual mob must swim upward.
            for(int x=0;x<=14;x++)for(int z=0;z<=5;z++)for(int y=3;y<=11;y++)
                h.setBlock(x,y,z,y==3?Blocks.STONE:y<=8?Blocks.WATER:Blocks.AIR);
            for(var kind:LichRules.Kind.values()) {
                var unit=ModEntities.LICH_UNIT.get().create(p.serverLevel());
                var pos=h.absolutePos(new BlockPos(2+kind.ordinal()*3,4,2));
                unit.setPos(pos.getX()+.5,pos.getY()+.25,pos.getZ()+.5);unit.configure(p.getUUID(),kind);
                p.serverLevel().addFreshEntity(unit);units.add(unit);
                var data=new CompoundTag();unit.saveWithoutId(data);data.putInt("LichEmerge",0);unit.readAdditionalSaveData(data);
                unit.command(LichRules.Order.STAND,unit.position(),null);double start=unit.getY();
                h.assertTrue(unit.getNavigation().canFloat(),"all undead navigation can float");
                ticks(unit,120);
                h.assertTrue(!unit.isRemoved()&&unit.combatTarget==null&&unit.getY()>start+1
                        &&unit.getEyeY()>=h.absolutePos(new BlockPos(0,9,0)).getY()-.5,
                        "idle "+kind+" surfaces instead of remaining on the bottom: "+unit.position());
            }
            // Exercise the real move controller on dry ground, independent of path reachability.
            var ghoul=ModEntities.LICH_UNIT.get().create(p.serverLevel());
            var dry=h.absolutePos(new BlockPos(2,15,2));h.setBlock(2,14,2,Blocks.STONE);
            ghoul.setPos(dry.getX()+.5,dry.getY(),dry.getZ()+.5);ghoul.configure(p.getUUID(),LichRules.Kind.GHOUL);
            p.serverLevel().addFreshEntity(ghoul);units.add(ghoul);
            var data=new CompoundTag();ghoul.saveWithoutId(data);data.putInt("LichEmerge",0);ghoul.readAdditionalSaveData(data);
            var control=ghoul.getMoveControl();ghoul.setOnGround(true);ghoul.setYRot(0);ghoul.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            control.setWantedPosition(ghoul.getX(),ghoul.getY(),ghoul.getZ()+20,1);control.tick();
            h.assertTrue(Math.abs(ghoul.getSpeed()-.24)<.0001,"ghoul starts at 80 percent of its previous speed");
            for(int i=0;i<60;i++) {
                ghoul.setDeltaMovement(0,0,.1);control.setWantedPosition(ghoul.getX(),ghoul.getY(),ghoul.getZ()+20,1);control.tick();
            }
            h.assertTrue(Math.abs(ghoul.getSpeed()-.45)<.0001,"straight three-second run reaches 150 percent");
            double before=ghoul.getSpeed();float heading=ghoul.getYRot();
            control.setWantedPosition(ghoul.getX()+20,ghoul.getY(),ghoul.getZ(),1);control.tick();
            h.assertTrue(ghoul.getSpeed()<before&&Math.abs(net.minecraft.util.Mth.wrapDegrees(ghoul.getYRot()-heading))<=8.01,
                    "fast ghoul loses speed and cannot instantly turn at a right angle");
            ghoul.horizontalCollision=true;control.setWantedPosition(ghoul.getX(),ghoul.getY(),ghoul.getZ()+20,1);control.tick();
            h.assertTrue(Math.abs(ghoul.getSpeed()-.24)<.0001,"collision resets run-up rather than charging against a wall");
            h.assertTrue(Math.abs(ghoul.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-.30)<.0001,
                    "run-up scales navigation speed without replacing base attributes or other modifiers");
            var run=new LichGhoulRun();for(int i=0;i<120;i++)run.step(true,true,0);
            h.assertTrue(Math.abs(run.multiplier()-1.5)<.0001,"run-up remains capped");
            run.step(false,true,0);h.assertTrue(run.multiplier()==.8,"stopping removes stored run-up");
            for(int i=0;i<80;i++)run.step(true,false,0);
            h.assertTrue(run.multiplier()==.8,"air time cannot charge a sprint");
            h.succeed();
        } finally {
            units.forEach(net.minecraft.world.entity.Entity::discard);phase.setScore(0);LichManager.update(p.server,List.of());
            p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());
        }
    }
    @GameTest(template="scp173_empty",batch="lich_running_chase",timeoutTicks=100)
    public static void chasesRunningPlayerWithoutPlanningStops(GameTestHelper h) {
        var world=h.getLevel();long clock=world.getGameTime();
        var time=(net.minecraft.world.level.storage.ServerLevelData)world.getLevelData();
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_runner_owner","maniac",1,1);
        var target=player(h,"lich_runner_target","survivors",2,2);
        PlayerDataManager.get(p).setManiacClassId(14);LichManager.update(p.server,List.of(p,target));
        var unit=ModEntities.LICH_UNIT.get().create(world);
        try {
            for(int x=0;x<=78;x++)for(int z=0;z<=8;z++)for(int y=20;y<=24;y++)h.setBlock(x,y,z,y==20?Blocks.STONE:Blocks.AIR);
            var start=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,21,4)));
            unit.setPos(start);unit.configure(p.getUUID(),LichRules.Kind.GHOUL);world.addFreshEntity(unit);
            var tag=new CompoundTag();unit.saveWithoutId(tag);tag.putInt("LichEmerge",0);unit.readAdditionalSaveData(tag);
            target.setPos(start.add(8,0,0));unit.nextAttack=Long.MAX_VALUE;
            unit.command(LichRules.Order.CHASE,target.position(),target.getUUID());
            int pause=0,maxPause=0;double initial=unit.distanceTo(target);
            for(int i=1;i<=180;i++) {
                var before=unit.position();target.setPos(target.position().add(.28,0,0));
                target.setDeltaMovement(.28,0,0);time.setGameTime(clock+i);ticks(unit,1);
                if(i>40&&unit.distanceToSqr(target)>6.25) {
                    pause=unit.position().subtract(before).horizontalDistanceSqr()<.0004?pause+1:0;
                    maxPause=Math.max(maxPause,pause);
                }
            }
            h.assertTrue(target.getUUID().equals(unit.combatTarget),"running visible player remains the combat target");
            h.assertTrue(maxPause<=2,"straight pursuit must not stop for planning: longest pause="+maxPause);
            h.assertTrue(unit.distanceTo(target)<initial,"ghoul closes the gap to a sprinting player: "+unit.distanceTo(target));
            h.assertTrue(unit.routing.plansStarted()==0,"a supported straight corridor needs no A* queries");
            // The shortcut must reject unsupported ground and low ceilings, including for a small ghoul.
            var end=unit.position().add(4,0,0);var hole=BlockPos.containing(unit.position().add(2,-1,0));
            world.setBlockAndUpdate(hole,Blocks.AIR.defaultBlockState());
            h.assertTrue(!unit.routing.corridor(unit.position(),end),"direct steering must reject a gap in the floor");
            world.setBlockAndUpdate(hole,Blocks.STONE.defaultBlockState());
            world.setBlockAndUpdate(hole.above(2),Blocks.STONE.defaultBlockState());
            h.assertTrue(!unit.routing.corridor(unit.position(),end),"direct steering respects full body height");
            org.example.maniacrevolution.Maniacrev.LOGGER.info("LICH_RUNNING_CHASE: gap {} -> {}, longest pause {} ticks, searches {}",initial,unit.distanceTo(target),maxPause,unit.routing.plansStarted());
            h.succeed();
        } finally {
            unit.routing.stop();unit.discard();time.setGameTime(clock);phase.setScore(0);LichManager.update(p.server,List.of());
            for(var player:List.of(p,target)){player.discard();player.getScoreboard().removePlayerFromTeam(player.getScoreboardName());}
        }
    }
    @GameTest(template="scp173_empty",batch="lich_planning_queue",timeoutTicks=100)
    public static void movingGoalsDoNotStarveSharedPathQueue(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_queue_owner","maniac",1,1);
        PlayerDataManager.get(p).setManiacClassId(14);LichManager.update(p.server,List.of(p));
        var units=new ArrayList<LichUnit>();
        try {
            for(int x=0;x<=32;x++)for(int z=0;z<=27;z++)for(int y=20;y<=25;y++)h.setBlock(x,y,z,y==20?Blocks.STONE:Blocks.AIR);
            for(int y=21;y<=24;y++) {
                for(int z=3;z<=22;z++){h.setBlock(11,y,z,Blocks.STONE);h.setBlock(16,y,z,Blocks.STONE);}
                for(int x=11;x<=16;x++)h.setBlock(x,y,3,Blocks.STONE);
            }
            var start=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(14,21,5)));
            var goal=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(20,21,5)));
            for(int i=0;i<32;i++) {
                var unit=ModEntities.LICH_UNIT.get().create(h.getLevel());units.add(unit);
                unit.setPos(start);unit.configure(p.getUUID(),LichRules.Kind.GHOUL);unit.setNoAi(true);
                // Exercise both priority queues without target acquisition or entity collisions.
                if(i<24)unit.combatTarget=UUID.randomUUID();
                unit.routing.move(goal);
            }
            int peak=0,finishedAt=-1;
            for(int frame=1;frame<=180;frame++) {
                for(var unit:units) {
                    unit.tickCount++;
                    // A running goal changes by more than the former restart threshold each update.
                    unit.routing.move(goal.add(0,0,(frame%8)*.4));
                }
                LichPathScheduler.run();peak=Math.max(peak,LichPathScheduler.lastPolls());
                h.assertTrue(LichPathScheduler.lastPolls()<=LichPathScheduler.BUDGET,"shared planning budget bounds total node polls");
                if(units.stream().allMatch(unit->unit.routing.plansCompleted()>0)){finishedAt=frame;break;}
            }
            h.assertTrue(finishedAt>0,"all 32 continuously changing goals finish a plan, including normal-priority orders");
            for(var unit:units) {
                h.assertTrue(unit.routing.plansStarted()<=2,"moving goal must not restart the pending search");
                h.assertTrue(!unit.getNavigation().isDone(),"finished query produces a usable route out of the dead end");
            }
            // Updating a destination starts a replacement without stopping the accepted route.
            var unit=units.get(31);var path=unit.getNavigation().getPath();unit.tickCount+=20;
            unit.routing.move(goal.add(0,0,6));
            h.assertTrue(unit.routing.pending()&&unit.getNavigation().getPath()==path&&!unit.getNavigation().isDone(),"old route stays active while replacement is calculated");
            org.example.maniacrevolution.Maniacrev.LOGGER.info("LICH_PLANNING_QUEUE: 32 moving goals completed in {} frames, peak {} polls/frame",finishedAt,peak);
            for(var queued:units){queued.routing.stop();queued.discard();}
            // Physical regression: a changing destination behind the U-wall used to restart A*
            // before completion. The unit must first go away from it, through the distant exit.
            var walker=ModEntities.LICH_UNIT.get().create(h.getLevel());units.add(walker);
            walker.setPos(start);walker.configure(p.getUUID(),LichRules.Kind.GHOUL);h.getLevel().addFreshEntity(walker);
            var tag=new CompoundTag();walker.saveWithoutId(tag);tag.putInt("LichEmerge",0);walker.readAdditionalSaveData(tag);
            walker.command(LichRules.Order.MOVE,goal,null);boolean exited=false;int movingWhilePending=0;
            for(int frame=0;frame<1500;frame++) {
                walker.anchor=frame<700?goal.add(0,0,((frame/5)%4)*1.4):goal;
                var before=walker.position();boolean pending=walker.routing.pending();ticks(walker,1);
                if(pending&&walker.position().distanceToSqr(before)>.0004)movingWhilePending++;
                exited|=walker.getZ()>=h.absolutePos(new BlockPos(0,0,23)).getZ();
                if(frame>=700&&walker.order==LichRules.Order.STAND&&LichNavigation.arrived(walker.position(),goal,1))break;
            }
            h.assertTrue(exited&&LichNavigation.arrived(walker.position(),goal,1),"moving destinations still physically reach the goal through the rear exit: "+walker.position());
            h.assertTrue(movingWhilePending>0,"unit physically keeps moving while replacement paths are pending");
            org.example.maniacrevolution.Maniacrev.LOGGER.info("LICH_MOVING_MAZE: reached goal, {} ticks moved with pending replacement, {} completed / {} started searches",movingWhilePending,walker.routing.plansCompleted(),walker.routing.plansStarted());
            h.succeed();
        } finally {
            for(var unit:units){unit.routing.stop();unit.discard();}
            phase.setScore(0);LichManager.update(p.server,List.of());p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());
        }
    }
    private static void ticks(LichUnit unit,int count){for(int i=0;i<count;i++){unit.tickCount++;unit.tick();LichPathScheduler.run();}}
    private static FakePlayer player(GameTestHelper h,String name,String team,int x,int z) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),name)){
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
            @Override public boolean canHarmPlayer(net.minecraft.world.entity.player.Player target){return true;}
        };
        var pos=h.absolutePos(new BlockPos(x,2,z));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);p.setGameMode(GameType.SURVIVAL);
        var board=p.getScoreboard();if(board.getPlayerTeam(team)==null)board.addPlayerTeam(team);board.addPlayerToTeam(p.getScoreboardName(),board.getPlayerTeam(team));
        h.getLevel().addNewPlayer(p);return p;
    }
    private static net.minecraft.world.scores.Score score(GameTestHelper h,String name,String entry,int value){
        var board=h.getLevel().getScoreboard();var objective=board.getObjective(name);
        if(objective==null)objective=board.addObjective(name,ObjectiveCriteria.DUMMY,Component.literal(name),ObjectiveCriteria.RenderType.INTEGER);
        var score=board.getOrCreatePlayerScore(entry,objective);score.setScore(value);return score;
    }
    @GameTest(template="scp173_empty",batch="lich_spawn_command",timeoutTicks=40)
    public static void spawnCorpsesCommandAcrossMaps(GameTestHelper h) throws Exception {
        var server=h.getLevel().getServer();var phase=score(h,"phaseGame","game",0);
        LichManager.update(server,List.of());LichManager.clearCorpses();
        var points=CorpsePoints.get(server);var original=List.copyOf(points.points);points.points.clear();
        String dimension=h.getLevel().dimension().location().toString();
        for(int i=0;i<4;i++) {
            int x=1+i*2;h.setBlock(x,1,2,Blocks.STONE);h.setBlock(x,2,2,Blocks.AIR);
            points.toggle(i%2==0?"mansion":"test_other_map",dimension,h.absolutePos(new BlockPos(x,2,2)));
        }
        points.toggle("duplicate_map",dimension,h.absolutePos(new BlockPos(1,2,2)));
        points.toggle("missing_dimension","maniacrev:missing",h.absolutePos(new BlockPos(1,2,2)));
        h.setBlock(1,2,4,Blocks.STONE);
        points.toggle("blocked",dimension,h.absolutePos(new BlockPos(1,2,4)));
        var source=server.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        String command="maniacrev lich spawn_corpses";
        try {
            int count=server.getCommands().getDispatcher().execute(command,source);
            h.assertTrue(count==4&&LichManager.corpseCount()==4,"command spawns all safe points across maps without baseline limit");
            h.assertTrue(LichManager.corpses().stream().anyMatch(c->c.map.equals("test_other_map")),"inactive map is included");
            var ids=LichManager.corpses().stream().map(LichObject::getUUID).collect(java.util.stream.Collectors.toSet());
            count=server.getCommands().getDispatcher().execute(command,source);
            h.assertTrue(count==0&&LichManager.corpseCount()==4,"repeated command skips occupied and duplicate points");
            for(var corpse:LichManager.corpses())corpse.tick();
            LichManager.update(server,List.of());
            h.assertTrue(LichManager.corpses().stream().allMatch(c->ids.contains(c.getUUID())&&!c.isRemoved()),"test corpses remain valid outside a match");
            count=server.getCommands().getDispatcher().execute("maniacrev lich clear_corpses",source);
            h.assertTrue(count==4&&LichManager.corpseCount()==0,"existing cleanup command removes test corpses");
            h.assertTrue(server.getCommands().getDispatcher().parse(command,source.withPermission(0)).getReader().canRead(),"command requires operator permission");
            h.succeed();
        } finally {
            phase.setScore(0);LichManager.clearCorpses();LichManager.pauseGuaranteedCorpses(false);
            points.points.clear();points.points.addAll(original);points.setDirty();
        }
    }
    @GameTest(template="scp173_empty",batch="lich_economy",timeoutTicks=100)
    public static void economyCastingAndOwnership(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);score(h,"map","Game",1);
        var p=player(h,"lich_fixture","maniac",2,2);var other=player(h,"lich_enemy","survivors",2,4);
        PlayerDataManager.get(p).setManiacClassId(LichClass.SCOREBOARD_ID);
        p.getCapability(ManaProvider.MANA).ifPresent(m->{m.setMaxMana(100);m.setMana(100);});
        var points=CorpsePoints.get(p.server);var original=List.copyOf(points.points);points.points.clear();
        for(int x=1;x<=5;x+=2){h.setBlock(x,1,2,Blocks.STONE);points.toggle("mansion",p.level().dimension().location().toString(),h.absolutePos(new BlockPos(x,2,2)));}
        var players=List.<ServerPlayer>of(p,other);
        LichManager.pauseGuaranteedCorpses(true); // A lobby cleanup must not disable a new match.
        LichManager.update(p.server,players);
        h.assertTrue(LichManager.active(p)&&LichManager.owner(p.getUUID())!=null,"numeric class ID starts Lich");
        h.assertTrue(LichManager.corpseCount()==3,"three authored corpses at match start");
        LichManager.update(p.server,players);h.assertTrue(LichManager.corpseCount()==3,"no duplicate baseline on next update");
        var corpse=LichManager.corpses().stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElseThrow();
        p.setPos(corpse.position().add(0,0,1.2));
        var request=new CompoundTag();request.putUUID("corpse",corpse.getUUID());request.putInt("kind",0);
        LichManager.request(other,"raise",request);h.assertTrue(LichManager.owner(other.getUUID())==null,"survivor cannot raise");
        LichManager.request(p,"raise",request);
        h.assertTrue(LichManager.owner(p.getUUID()).castSerial==1&&corpse.visualAction().equals("raise"),"cast feedback serial and corpse action begin together");
        h.assertTrue(LichManager.corpseCount()==3&&LichManager.owner(p.getUUID()).units.isEmpty(),"cast does not spend corpse early");
        p.setPos(p.position().add(.1,0,0));LichManager.update(p.server,players);
        h.assertTrue(LichManager.owner(p.getUUID()).cast==null&&LichManager.corpseCount()==3,"movement interrupts without consumption");
        h.assertTrue(corpse.visualAction().isEmpty(),"interrupted corpse ritual stops its visual action");
        p.setPos(corpse.position().add(0,0,1.2));
        long oldClock=p.level().getGameTime();
        LichManager.owner(p.getUUID()).nextRequest=0;
        LichManager.request(p,"raise",request);
        ((net.minecraft.world.level.storage.ServerLevelData)p.level().getLevelData()).setGameTime(oldClock+10);
            try{
                LichManager.update(p.server,players);var s=LichManager.owner(p.getUUID());
                h.assertTrue(s.units.size()==1&&LichManager.corpseCount()==2,"one unit plus two corpses preserve baseline");
                var u=s.units.values().iterator().next();
                h.assertTrue(s.castSerial==2&&u.syncedOwner().filter(p.getUUID()::equals).isPresent(),"new ritual serial and client-visible owner survive successful raising");
                h.assertTrue(u.emerging()>0&&u.order==LichRules.Order.FOLLOW&&!u.aggressive,"new vulnerable emerging unit holds follow order");
                float hp=u.getHealth();u.hurt(u.damageSources().generic(),2);h.assertTrue(u.getHealth()<hp,"emerging unit receives damage");
                double x=u.getDeltaMovement().x;u.knockback(2,1,0);h.assertTrue(u.getDeltaMovement().x==x,"emerging unit rejects knockback");
                LichManager.request(p,"raise",request);LichManager.update(p.server,players);h.assertTrue(s.units.size()==1,"spent corpse cannot be reused");
                DownedCapability.get(other).setState(DownedState.DOWNED);h.assertTrue(!LichManager.survivor(other),"AI ignores downed survivor");
                u.kill();LichManager.update(p.server,players);h.assertTrue(LichManager.corpseCount()==3,"unit death immediately restores baseline");
                LichManager.pauseGuaranteedCorpses(true);LichManager.clearCorpses();LichManager.update(p.server,players);
                h.assertTrue(LichManager.corpseCount()==0,"admin deletion is not immediately undone by baseline");
                LichManager.pauseGuaranteedCorpses(false);LichManager.update(p.server,players);h.assertTrue(LichManager.corpseCount()==3,"admin resume restores baseline");
                h.succeed();
            }finally{
                ((net.minecraft.world.level.storage.ServerLevelData)p.level().getLevelData()).setGameTime(oldClock);phase.setScore(0);LichManager.update(p.server,List.of());points.points.clear();points.points.addAll(original);points.setDirty();
                for(var entity:players){entity.discard();entity.getScoreboard().removePlayerFromTeam(entity.getScoreboardName());}
            }
    }
    @GameTest(template="scp173_empty",batch="lich_targeted_repair",timeoutTicks=60)
    public static void targetedPhylacteryRepairAndPickup(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_repair_click","maniac",2,4);
        PlayerDataManager.get(p).setManiacClassId(14);LichManager.update(p.server,List.of(p));
        var time=(net.minecraft.world.level.storage.ServerLevelData)p.level().getLevelData();long clock=p.level().getGameTime();
        try {
            for(int z=1;z<=5;z++)h.setBlock(2,1,z,Blocks.STONE);
            LichManager.hitPhylactery(p.getUUID());var s=LichManager.owner(p.getUUID());
            LichManager.place(p,h.absolutePos(new BlockPos(2,1,2)),Direction.UP);var object=s.phylactery;
            h.assertTrue(object!=null&&object.syncedOwner().filter(p.getUUID()::equals).isPresent(),"placed phylactery exposes its owner to client entity data");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            p.getCapability(ManaProvider.MANA).ifPresent(m->{m.setMaxMana(100);m.setMana(5);});
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,object.position().add(0,.4,0));
            var request=new CompoundTag();request.putUUID("phylactery",object.getUUID());
            s.nextRequest=0;LichManager.request(p,"repair",request);
            h.assertTrue(s.cast!=null&&s.hp==9&&p.getCapability(ManaProvider.MANA).orElseThrow(IllegalStateException::new).getMana()==5,
                    "targeted empty-hand click starts repair without early mana or HP changes");
            time.setGameTime(clock+10);LichManager.update(p.server,List.of(p));
            h.assertTrue(s.hp==10&&p.getCapability(ManaProvider.MANA).orElseThrow(IllegalStateException::new).getMana()==0,"successful half-second repair restores one HP for five mana");
            s.nextRequest=0;LichManager.request(p,"repair",request);h.assertTrue(s.cast==null,"full phylactery cannot begin repair");
            p.removeEffect(org.example.maniacrevolution.effect.ModEffects.LICH_PHYLACTERY_GUARD.get());LichManager.hitPhylactery(p.getUUID());
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(ModItems.LICH_BOOK.get()));
            s.nextRequest=0;LichManager.request(p,"repair",request);time.setGameTime(clock+20);LichManager.update(p.server,List.of(p));
            h.assertTrue(s.hp==9&&p.getCapability(ManaProvider.MANA).orElseThrow(IllegalStateException::new).getMana()==0,"book repair attempt without mana cannot heal");
            p.getCapability(ManaProvider.MANA).ifPresent(m->m.setMana(10));
            var foreign=new CompoundTag();foreign.putUUID("phylactery",UUID.randomUUID());
            s.nextRequest=0;LichManager.request(p,"repair",foreign);h.assertTrue(s.cast==null,"foreign/stale target UUID is rejected");
            p.setYRot(0);p.setXRot(0);s.nextRequest=0;LichManager.request(p,"repair",request);
            h.assertTrue(s.cast==null,"targeted repair requires actually aiming at the object");
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,object.position().add(0,.4,0));
            h.setBlock(2,2,3,Blocks.STONE);h.setBlock(2,3,3,Blocks.STONE);
            s.nextRequest=0;LichManager.request(p,"repair",request);h.assertTrue(s.cast==null,"wall blocks targeted repair");
            h.setBlock(2,2,3,Blocks.AIR);h.setBlock(2,3,3,Blocks.AIR);
            s.nextRequest=0;LichManager.request(p,"repair",request);h.assertTrue(s.cast!=null,"valid target can be repaired again");
            var original=p.position();p.setPos(original.add(.1,0,0));LichManager.update(p.server,List.of(p));
            h.assertTrue(s.cast==null&&s.hp==9,"moving cancels the repair cast");p.setPos(original);
            s.nextRequest=0;LichManager.request(p,"repair",request);h.assertTrue(s.cast!=null,"another targeted cast begins");
            object.interact(p,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(s.phylactery==null&&object.isRemoved()&&p.getInventory().contains(new net.minecraft.world.item.ItemStack(ModItems.LICH_PHYLACTERY.get())),"normal right click takes back the phylactery");
            time.setGameTime(clock+30);LichManager.update(p.server,List.of(p));
            h.assertTrue(s.hp==9&&p.getCapability(ManaProvider.MANA).orElseThrow(IllegalStateException::new).getMana()==10,
                    "picking up the repair target prevents delayed healing and mana consumption");
            h.succeed();
        } finally {time.setGameTime(clock);phase.setScore(0);LichManager.update(p.server,List.of());p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());}
    }
    @GameTest(template="scp173_empty",batch="lich_phylactery",timeoutTicks=40)
    public static void carriedPhylacteryDoesNotMultiplyWhenTaggedOrMoved(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);
        var p=player(h,"lich_phyl_copies","maniac",2,2);
        PlayerDataManager.get(p).setManiacClassId(14);
        var players=List.<ServerPlayer>of(p);
        var item=ModItems.LICH_PHYLACTERY.get();
        var inventory=p.getInventory();
        try {
            LichManager.update(p.server,players);
            int slot=inventory.findSlotMatchingItem(new net.minecraft.world.item.ItemStack(item));
            h.assertTrue(slot>=0,"the active Lich receives the carried item");
            var original=inventory.getItem(slot);
            LichManager.hitPhylactery(p.getUUID());
            LichManager.request(p,"repair",new CompoundTag());
            h.assertTrue(original.hasTag()&&"repair".equals(original.getTag().getString("LichCast")),"repair tags the actual carried item");
            for(int i=0;i<3;i++)LichManager.update(p.server,players);
            h.assertTrue(inventory.countItem(item)==1&&inventory.getItem(slot)==original,"repair NBT must not trigger a new issue");

            inventory.add(new net.minecraft.world.item.ItemStack(item));
            inventory.add(original.copy());
            LichManager.update(p.server,players);
            h.assertTrue(inventory.countItem(item)==1&&inventory.getItem(slot)==original,"existing duplicates are removed without replacing the original");

            inventory.removeItemNoUpdate(slot);p.containerMenu.setCarried(original);
            LichManager.update(p.server,players);
            h.assertTrue(inventory.countItem(item)==0&&p.containerMenu.getCarried()==original,"a stack held by the menu cursor remains the only copy");
            var storage=new net.minecraft.world.SimpleContainer(1);
            storage.setItem(0,original);
            p.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
            for(int i=0;i<3;i++)LichManager.update(p.server,players);
            h.assertTrue(inventory.countItem(item)==0,"moving the issued stack out of the player inventory cannot farm more copies");

            inventory.setItem(slot,storage.removeItemNoUpdate(0));
            h.setBlock(2,1,2,Blocks.STONE);
            LichManager.place(p,h.absolutePos(new BlockPos(2,1,2)),Direction.UP);
            var army=LichManager.owner(p.getUUID());
            h.assertTrue(army.phylactery!=null&&inventory.countItem(item)==0,"placement converts the carried item to a single entity");
            inventory.add(new net.minecraft.world.item.ItemStack(item));LichManager.update(p.server,players);
            h.assertTrue(inventory.countItem(item)==0,"no carried copy can coexist with the placed entity");
            var placed=army.phylactery;
            for(int i=0;i<inventory.items.size();i++)inventory.setItem(i,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE,64));
            LichManager.pickup(p,placed);
            h.assertTrue(army.phylactery==placed&&!placed.isRemoved(),"a full inventory leaves the placed phylactery intact");
            inventory.setItem(slot,net.minecraft.world.item.ItemStack.EMPTY);
            LichManager.pickup(p,placed);LichManager.update(p.server,players);
            h.assertTrue(army.phylactery==null&&inventory.countItem(item)==1,"pickup restores exactly one carried item");
            h.succeed();
        } finally {
            phase.setScore(0);LichManager.update(p.server,List.of());p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());
        }
    }

    @GameTest(template="scp173_empty",batch="lich_phylactery",timeoutTicks=40)
    public static void phylacteryCooldownRepairAndTransfer(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);
        var p=player(h,"lich_phyl","maniac",2,2);var enemy=player(h,"lich_thief","survivors",2,3);
        PlayerDataManager.get(p).setManiacClassId(14);var players=List.<ServerPlayer>of(p,enemy);
        LichManager.update(p.server,players);var s=LichManager.owner(p.getUUID());
        try{
            LichManager.hitPhylactery(p.getUUID());h.assertTrue(s.hp==9,"accepted hit removes exactly one HP");
            var effect=org.example.maniacrevolution.effect.ModEffects.LICH_PHYLACTERY_GUARD.get();
            var cooldown=p.getEffect(effect);
            h.assertTrue(cooldown!=null&&cooldown.getDuration()==100&&cooldown.showIcon()&&!cooldown.isVisible(),"five-second guard is an icon-visible status without ambient particles");
            LichManager.hitPhylactery(p.getUUID());h.assertTrue(s.hp==9&&p.getEffect(effect)==cooldown,"effect blocks duplicate hit without refreshing duration");
            h.setBlock(2,1,2,Blocks.STONE);
            LichManager.place(p,h.absolutePos(new BlockPos(2,1,2)),Direction.UP);
            h.assertTrue(s.phylactery!=null&&s.hp==9&&p.getEffect(effect)==cooldown,"placement preserves HP and effect");
            LichManager.pickup(enemy,s.phylactery);h.assertTrue(s.phylactery!=null,"other player cannot pick up");
            LichManager.pickup(p,s.phylactery);h.assertTrue(s.phylactery==null&&s.hp==9&&p.getEffect(effect)==cooldown,"owner pickup preserves HP and effect");
            int cleared=p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),"effect clear @s");
            h.assertTrue(cleared>0&&!p.hasEffect(effect),"real effect clear command removes the guard");
            LichManager.hitPhylactery(p.getUUID());h.assertTrue(s.hp==8&&p.hasEffect(effect),"clearing effects immediately permits another phylactery hit");
            p.removeEffect(effect);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,1,0,false,false,true));p.baseTick();
            h.assertTrue(!p.hasEffect(effect),"guard expires through normal player effect ticking");
            LichManager.hitPhylactery(p.getUUID());h.assertTrue(s.hp==7&&p.hasEffect(effect),"expiration releases cooldown without any hidden timestamp");
            p.getCapability(ManaProvider.MANA).ifPresent(m->m.setMana(0));
            var hurt=new net.minecraftforge.event.entity.living.LivingHurtEvent(p,p.damageSources().generic(),8);
            LichManager.hurt(hurt);h.assertTrue(hurt.getAmount()==4,"only additional reduction is one half");
            p.setGameMode(GameType.CREATIVE);h.assertTrue(!LichManager.active(p),"creative keeps vanilla functions");
            LichManager.update(p.server,players);h.assertTrue(LichManager.owner(p.getUUID())==null,"mode change cleans army");
            h.succeed();
        }finally{phase.setScore(0);LichManager.update(p.server,List.of());for(var entity:players){entity.discard();entity.getScoreboard().removePlayerFromTeam(entity.getScoreboardName());}}
    }
    @GameTest(template="scp173_empty",batch="lich_absorb",timeoutTicks=40)
    public static void absorptionRepairAndGroups(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);score(h,"map","Game",1);
        var p=player(h,"lich_absorb","maniac",2,2);PlayerDataManager.get(p).setManiacClassId(14);
        var points=CorpsePoints.get(p.server);var saved=List.copyOf(points.points);points.points.clear();
        for(int x=1;x<=5;x+=2){h.setBlock(x,1,2,Blocks.STONE);points.toggle("mansion",p.level().dimension().location().toString(),h.absolutePos(new BlockPos(x,2,2)));}
        long clock=p.level().getGameTime();var world=(net.minecraft.world.level.storage.ServerLevelData)p.level().getLevelData();var players=List.<ServerPlayer>of(p);
        try {
            LichManager.update(p.server,players);var s=LichManager.owner(p.getUUID());
            var corpse=LichManager.corpses().get(0);p.setPos(corpse.position().add(0,0,1));
            p.getCapability(ManaProvider.MANA).ifPresent(m->{m.setMaxMana(100);m.setMana(10);});
            var d=new CompoundTag();d.putUUID("corpse",corpse.getUUID());
            LichManager.request(p,"absorb",d);h.assertTrue(LichManager.corpseCount()==3,"absorb waits for cast");
            world.setGameTime(clock+10);LichManager.update(p.server,players);
            h.assertTrue(LichManager.corpseCount()==2,"absorption delays guaranteed replenishment");
            h.assertTrue(p.getCapability(ManaProvider.MANA).orElseThrow(IllegalStateException::new).getMana()==30,"successful absorb gives twenty mana once");
            LichManager.hitPhylactery(p.getUUID());s.nextRequest=0;
            LichManager.request(p,"repair",new CompoundTag());world.setGameTime(clock+20);LichManager.update(p.server,players);
            h.assertTrue(s.hp==10&&p.getCapability(ManaProvider.MANA).orElseThrow(IllegalStateException::new).getMana()==25,"repair restores one for five mana");
            var u=ModEntities.LICH_UNIT.get().create(p.serverLevel());u.setPos(p.position().add(2,0,0));u.configure(p.getUUID(),LichRules.Kind.BUTCHER);p.serverLevel().addFreshEntity(u);
            s.nextRequest=0;LichManager.request(p,"all",new CompoundTag());
            var group=new CompoundTag();group.putInt("key",3);group.putBoolean("save",true);s.nextRequest=0;LichManager.request(p,"group",group);
            s.selected.clear();group.putBoolean("save",false);s.nextRequest=0;LichManager.request(p,"group",group);
            h.assertTrue(s.selected.contains(u.getUUID()),"RTS group restores authoritative selection");
            for(int i=0;i<12;i++) {
                var extra=ModEntities.LICH_UNIT.get().create(p.serverLevel());extra.setPos(p.position());
                extra.configure(p.getUUID(),LichRules.Kind.ZOMBIE);p.serverLevel().addFreshEntity(extra);
                extra.order=LichRules.Order.values()[i%LichRules.Order.values().length];
            }
            s.page=1;s.nextRequest=0;LichManager.request(p,"all",new CompoundTag());
            h.assertTrue(s.selected.size()==13,"select all spans multiple pages");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(ModItems.LICH_BOOK.get()));
            p.setShiftKeyDown(true);LichManager.quick(p);
            h.assertTrue(s.selected.isEmpty()&&s.units.size()==13&&s.groups.get(3).contains(u.getUUID())&&u.order==LichRules.Order.FOLLOW,
                    "shift book-use deselects every page without changing army, orders or groups");
            LichManager.quick(p);h.assertTrue(s.selected.isEmpty(),"repeat shift book-use does not reselect anything");
            p.setShiftKeyDown(false);s.nextRequest=0;LichManager.request(p,"all",new CompoundTag());
            h.assertTrue(Arrays.stream(LichManager.selectedOrders(s)).sum()==13&&Arrays.stream(LichManager.selectedOrders(s)).allMatch(count->count>0),"reticle order summary includes every order across pages");
            var click=new CompoundTag();click.putUUID("id",u.getUUID());
            s.nextRequest=0;LichManager.request(p,"select",click);
            h.assertTrue(s.selected.size()==12&&!s.selected.contains(u.getUUID()),"plain click on selected unit toggles only that unit off");
            click.putBoolean("add",true);s.nextRequest=0;LichManager.request(p,"select",click);
            h.assertTrue(s.selected.size()==13&&s.selected.contains(u.getUUID()),"shift click adds to current selection");
            s.nextRequest=0;LichManager.request(p,"select",click);
            h.assertTrue(s.selected.size()==12&&!s.selected.contains(u.getUUID()),"shift click on selected unit toggles it off");
            click.putBoolean("add",false);s.nextRequest=0;LichManager.request(p,"select",click);
            h.assertTrue(s.selected.size()==1&&s.selected.contains(u.getUUID()),"plain click on unselected unit replaces selection");
            s.nextRequest=0;LichManager.request(p,"select",click);
            h.assertTrue(s.selected.isEmpty(),"second plain click on sole selected unit deselects it");
            s.nextRequest=0;LichManager.request(p,"all",new CompoundTag());
            s.nextRequest=0;LichManager.request(p,"none",new CompoundTag());
            h.assertTrue(s.selected.isEmpty()&&s.units.size()==13&&s.page==1,"deselect clears every page without removing units or changing page");
            h.assertTrue(u.order==LichRules.Order.FOLLOW&&s.groups.get(3).contains(u.getUUID()),"deselect preserves orders and saved groups");
            s.nextRequest=0;LichManager.request(p,"none",new CompoundTag());
            h.assertTrue(s.selected.isEmpty(),"deselect is safe when already empty");
            s.nextRequest=0;LichManager.request(p,"group",group);
            h.assertTrue(s.selected.size()==1&&s.selected.contains(u.getUUID()),"saved group can be recalled after deselecting all");
            var order=new CompoundTag();order.putInt("order",LichRules.Order.STAND.ordinal());s.nextRequest=0;LichManager.request(p,"order",order);
            h.assertTrue(u.order==LichRules.Order.STAND&&u.anchor.equals(u.position()),"stand anchors each unit at its own position");
            u.command(LichRules.Order.RETURN,p.position(),null);p.setPos(p.position().add(3,0,0));
            h.assertTrue(u.task(p,p.level().getGameTime()).equals(p.position()),"return approaches current owner position rather than stale point");
            // The owner was moved outside the small template: give this arrival fixture real support.
            p.serverLevel().setBlockAndUpdate(p.blockPosition().below(),Blocks.STONE.defaultBlockState());
            u.setPos(p.position());u.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);u.setOnGround(true);
            var ready=new CompoundTag();u.saveWithoutId(ready);ready.putInt("LichEmerge",0);u.readAdditionalSaveData(ready);ticks(u,5);
            h.assertTrue(u.order==LichRules.Order.STAND,"return ends in stand after reaching owner");
            h.assertTrue(u.getMaxHealth()==40&&Math.abs(u.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)-.75)<.001,"butcher attributes");
            h.succeed();
        } finally {
            world.setGameTime(clock);phase.setScore(0);LichManager.update(p.server,List.of());points.points.clear();points.points.addAll(saved);points.setDirty();p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());
        }
    }
    @GameTest(template="scp173_empty",batch="lich_sight",timeoutTicks=40)
    public static void unlimitedSelectionAndWallOcclusion(GameTestHelper h) {
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_sight","maniac",2,2);PlayerDataManager.get(p).setManiacClassId(14);
        try {
            var start=p.getEyePosition();var end=start.add(0,0,160);
            for(int i=0;i<=160;i+=8)p.serverLevel().getChunk(BlockPos.containing(start.add(0,0,i)));
            // Place the ray above terrain without relying on the test template's size.
            p.setPos(p.getX(),p.getY()+20,p.getZ());start=p.getEyePosition();end=start.add(0,0,160);
            for(int i=0;i<=160;i++)p.serverLevel().setBlockAndUpdate(BlockPos.containing(start.add(0,0,i)),Blocks.AIR.defaultBlockState());
            var ray=p.level().clip(new net.minecraft.world.level.ClipContext(start,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            h.assertTrue(LichManager.clearSight(p,end),"clear selection ray exceeds vanilla 128-block cap: "+ray.getType()+" "+ray.getLocation()+" from "+start);
            LichManager.update(p.server,List.of(p));var unit=ModEntities.LICH_UNIT.get().create(p.serverLevel());unit.setPos(p.position().add(0,0,160));unit.configure(p.getUUID(),LichRules.Kind.ZOMBIE);p.serverLevel().addFreshEntity(unit);
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,unit.getEyePosition());LichManager.quick(p);
            h.assertTrue(LichManager.owner(p.getUUID()).selected.contains(unit.getUUID()),"book selects server-owned unit at 160 blocks");
            LichManager.quick(p);h.assertTrue(LichManager.owner(p.getUUID()).selected.isEmpty(),"second book click toggles only that unit");
            var wall=BlockPos.containing(start.add(0,0,8));p.serverLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
            h.assertTrue(!LichManager.clearSight(p,end),"wall blocks remote selection");p.serverLevel().removeBlock(wall,false);
            h.succeed();
        } finally {phase.setScore(0);LichManager.update(p.server,List.of());p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());}
    }
    @GameTest(template="scp173_empty",batch="lich_ai",timeoutTicks=60)
    public static void sightMemoryDoorsAndFriendlyArrows(GameTestHelper h){
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_ai","maniac",1,1);var target=player(h,"lich_ai_target","survivors",3,3);
        PlayerDataManager.get(p).setManiacClassId(14);var players=List.<ServerPlayer>of(p,target);LichManager.update(p.server,players);
        try{
            var unit=ModEntities.LICH_UNIT.get().create(p.serverLevel());var pos=h.absolutePos(new BlockPos(2,2,2));h.setBlock(2,1,2,Blocks.STONE);unit.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);unit.configure(p.getUUID(),LichRules.Kind.GHOUL);p.serverLevel().addFreshEntity(unit);
            var data=new CompoundTag();unit.saveWithoutId(data);data.putInt("LichEmerge",0);unit.readAdditionalSaveData(data);unit.command(LichRules.Order.STAND,unit.position(),null);
            target.setShiftKeyDown(true);ticks(unit,5);
            h.assertTrue(unit.detecting&&unit.lastSeen!=null,"stationary crouching survivor is detected; eligible="+LichManager.survivor(target)+", sight="+unit.hasLineOfSight(target)+", removed="+unit.isRemoved()+", nearby="+p.serverLevel().getEntitiesOfClass(ServerPlayer.class,unit.getBoundingBox().inflate(16)).size()+", unit="+unit.position()+", target="+target.position());var known=unit.lastSeen;
            for(int x=2;x<=3;x++)for(int y=2;y<=3;y++)h.setBlock(x,y,3,Blocks.STONE);
            var hidden=h.absolutePos(new BlockPos(5,2,5));target.setPos(hidden.getX()+.5,hidden.getY(),hidden.getZ()+.5);ticks(unit,5);
            h.assertTrue(!unit.detecting&&known.equals(unit.lastSeen),"hidden movement does not refresh last known position");
            DownedCapability.get(target).setState(DownedState.DOWNED);ticks(unit,5);h.assertTrue(!unit.detecting,"downed target is ignored");
            var trap=Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN,false);
            var iron=Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN,false);
            h.setBlock(1,2,2,trap);h.setBlock(1,2,1,iron);ticks(unit,5);
            h.assertTrue(h.getBlockState(new BlockPos(1,2,2)).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN),"hand-openable trapdoor opens");
            h.assertTrue(!h.getBlockState(new BlockPos(1,2,1)).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN),"iron trapdoor is not forced open");
            var arrow=new net.minecraft.world.entity.projectile.Arrow(p.level(),unit);arrow.setOwner(p);arrow.getPersistentData().putBoolean("lich_arrow",true);float hp=p.getHealth();
            LichManager.projectile(new net.minecraftforge.event.entity.ProjectileImpactEvent(arrow,new net.minecraft.world.phys.EntityHitResult(p)));
            h.assertTrue(arrow.isRemoved()&&p.getHealth()==hp,"allied arrow cannot hurt Lich");h.succeed();
        }finally{phase.setScore(0);LichManager.update(p.server,List.of());for(var entity:players){entity.discard();entity.getScoreboard().removePlayerFromTeam(entity.getScoreboardName());}}
    }
    @GameTest(template="scp173_empty",batch="lich_destroy",timeoutTicks=40)
    public static void invulnerableHitAndDestruction(GameTestHelper h){
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_destroy","maniac",2,2);var enemy=player(h,"lich_attacker","survivors",2,3);
        PlayerDataManager.get(p).setManiacClassId(14);LichManager.update(p.server,List.of(p,enemy));var s=LichManager.owner(p.getUUID());
        try{
            var attack=new net.minecraftforge.event.entity.player.AttackEntityEvent(enemy,p);attack.setCanceled(true);LichManager.carriedMelee(attack);
            var guard=org.example.maniacrevolution.effect.ModEffects.LICH_PHYLACTERY_GUARD.get();
            h.assertTrue(s.hp==9&&p.getEffect(guard)!=null&&p.getEffect(guard).getDuration()==100,"canceled/invulnerable melee registers exactly one carried hit with 5s guard");
            var repeated=new net.minecraftforge.event.entity.living.LivingAttackEvent(p,p.damageSources().playerAttack(enemy),10);LichManager.attack(repeated);
            h.assertTrue(s.hp==9,"attack and hurt paths do not double-count same hit");
            h.setBlock(2,1,2,Blocks.STONE);LichManager.place(p,h.absolutePos(new BlockPos(2,1,2)),Direction.UP);p.removeEffect(guard);
            LichManager.carriedMelee(attack);h.assertTrue(s.hp==9,"hitting owner does not damage placed phylactery");
            s.phylactery.hurt(p.damageSources().lava(),100);h.assertTrue(s.hp==9,"environment cannot damage phylactery");
            s.phylactery.hurt(p.damageSources().playerAttack(enemy),100);h.assertTrue(s.hp==8,"large survivor hit still removes one HP");
            phase.setScore(3);p.removeEffect(guard);LichManager.hitPhylactery(p.getUUID());h.assertTrue(p.getEffect(guard).getDuration()==40,"reversal guard lasts two seconds");
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,p,0,"f_8921_");
            while(s.hp>0){p.removeEffect(guard);LichManager.hitPhylactery(p.getUUID());}
            h.assertTrue(!p.isAlive()&&LichManager.owner(p.getUUID())==null,"destroyed phylactery kills owner immediately and cleans army");
            h.assertTrue(!p.hasEffect(guard),"match cleanup removes phylactery guard");
            h.succeed();
        }finally{phase.setScore(0);LichManager.update(p.server,List.of());for(var entity:List.of(p,enemy)){entity.discard();entity.getScoreboard().removePlayerFromTeam(entity.getScoreboardName());}}
    }
    @GameTest(template="scp173_empty",batch="unique_maniacs",timeoutTicks=40)
    public static void uniqueManiacRoster(GameTestHelper h){
        var first=player(h,"unique_first","maniac",1,1);var second=player(h,"unique_second","maniac",2,1);var survivor=player(h,"unique_survivor","survivors",3,1);
        var players=List.<ServerPlayer>of(first,second,survivor);
        try{
            PlayerDataManager.get(first).setManiacClassId(14);PlayerDataManager.get(second).setManiacClassId(9);PlayerDataManager.get(survivor).setManiacClassId(14);
            h.assertTrue(org.example.maniacrevolution.character.UniqueManiacSelection.occupiedBy(second,14,players)==first,"occupied Lich is rejected for another maniac");
            h.assertTrue(org.example.maniacrevolution.character.UniqueManiacSelection.occupiedBy(first,14,players)==null,"own re-selection and survivor stored preference do not conflict");
            PlayerDataManager.get(first).setManiacClassId(9);
            h.assertTrue(!org.example.maniacrevolution.character.UniqueManiacSelection.duplicates(players).isEmpty(),"match-start validation detects duplicate non-Lich maniacs");
            h.assertTrue(org.example.maniacrevolution.character.UniqueManiacSelection.occupiedBy(second,14,players)==null,"changing class releases previous slot");
            second.setGameMode(GameType.SPECTATOR);
            h.assertTrue(org.example.maniacrevolution.character.UniqueManiacSelection.duplicates(players).isEmpty(),"spectators do not reserve maniac slots");
            h.assertTrue(PlayerDataManager.setSelectedClass(survivor,org.example.maniacrevolution.character.CharacterType.SURVIVOR,4),"survivor selection remains unrestricted");
            h.succeed();
        }finally{for(var p:players){p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());}}
    }
    @GameTest(template="scp173_empty",batch="lich_army",timeoutTicks=60)
    public static void largeArmyAndStaleEntityCleanup(GameTestHelper h){
        var phase=score(h,"phaseGame","game",1);var p=player(h,"lich_army","maniac",2,2);PlayerDataManager.get(p).setManiacClassId(14);
        var players=List.<ServerPlayer>of(p);LichManager.update(p.server,players);long start=System.nanoTime();
        try{
            CompoundTag saved=null;
            for(int i=0;i<80;i++){
                var u=ModEntities.LICH_UNIT.get().create(p.serverLevel());u.setPos(p.position().add((i%10)*2,0,4+(i/10)*2));u.configure(p.getUUID(),LichRules.Kind.values()[i%4]);
                p.serverLevel().addFreshEntity(u);var tag=new CompoundTag();u.saveWithoutId(tag);tag.putInt("LichEmerge",0);u.readAdditionalSaveData(tag);if(i==0)saved=tag;
            }
            var s=LichManager.owner(p.getUUID());h.assertTrue(s.units.size()==80,"army is not capped at baseline or HUD page size");
            for(int step=0;step<10;step++)for(var u:List.copyOf(s.units.values()))ticks(u,1);
            org.example.maniacrevolution.Maniacrev.LOGGER.info("LICH_ARMY_PROBE: 80 units x 10 server ticks plus spawn: {} ms",(System.nanoTime()-start)/1_000_000.0);
            h.assertTrue(LichManager.clearUnits()==80&&s.units.isEmpty(),"admin cleanup removes the complete army");
            var stale=ModEntities.LICH_UNIT.get().create(p.serverLevel());stale.load(saved);
            h.assertTrue(!p.serverLevel().addFreshEntity(stale),"cleared unit cannot return through chunk reload");
            h.succeed();
        }finally{phase.setScore(0);LichManager.update(p.server,List.of());p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());}
    }
}
