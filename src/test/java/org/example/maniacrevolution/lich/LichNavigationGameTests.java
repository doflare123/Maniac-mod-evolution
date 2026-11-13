package org.example.maniacrevolution.lich;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.entity.ModEntities;

/** Deterministic server-only navigation regressions; no client or external benchmark dependencies. */
@GameTestHolder("maniacrev_lich")
@PrefixGameTestTemplate(false)
public final class LichNavigationGameTests {
    private static final class Arena implements AutoCloseable {
        final GameTestHelper h;
        final FakePlayer owner;
        final net.minecraft.world.scores.Score phase;
        final List<LichUnit> units=new ArrayList<>();
        final List<FakePlayer> survivors=new ArrayList<>();
        Arena(GameTestHelper h,String name,int width,int depth) {
            this.h=h;var world=h.getLevel();var board=world.getScoreboard();
            var objective=board.getObjective("phaseGame");
            if(objective==null)objective=board.addObjective("phaseGame",ObjectiveCriteria.DUMMY,Component.literal("phaseGame"),ObjectiveCriteria.RenderType.INTEGER);
            phase=board.getOrCreatePlayerScore("game",objective);phase.setScore(1);
            owner=new FakePlayer(world,new GameProfile(UUID.randomUUID(),name));owner.setGameMode(GameType.SURVIVAL);
            if(board.getPlayerTeam("maniac")==null)board.addPlayerTeam("maniac");
            board.addPlayerToTeam(owner.getScoreboardName(),board.getPlayerTeam("maniac"));
            owner.setPos(point(1,1));world.addNewPlayer(owner);PlayerDataManager.get(owner).setManiacClassId(14);
            LichManager.update(owner.server,List.of(owner));
            for(int x=0;x<width;x++)for(int z=0;z<depth;z++)for(int y=20;y<=26;y++)h.setBlock(x,y,z,y==20?Blocks.STONE:Blocks.AIR);
        }
        Vec3 point(int x,int z) {return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(x,21,z)));}
        LichUnit unit(LichRules.Kind kind,int x,int z,boolean physical) {
            var unit=ModEntities.LICH_UNIT.get().create(h.getLevel());units.add(unit);unit.setPos(point(x,z));unit.configure(owner.getUUID(),kind);
            var tag=new CompoundTag();unit.saveWithoutId(tag);tag.putInt("LichEmerge",0);unit.readAdditionalSaveData(tag);
            if(physical)h.getLevel().addFreshEntity(unit);else unit.setNoAi(true);
            return unit;
        }
        void wall(int x,int z) {for(int y=21;y<=24;y++)h.setBlock(x,y,z,Blocks.STONE);}
        FakePlayer survivor(Vec3 position) {
            var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"nav_survivor")){
                @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
            };
            p.setPos(position);p.setGameMode(GameType.SURVIVAL);
            var board=p.getScoreboard();
            if(board.getPlayerTeam("survivors")==null)board.addPlayerTeam("survivors");
            board.addPlayerToTeam(p.getScoreboardName(),board.getPlayerTeam("survivors"));
            h.getLevel().addNewPlayer(p);survivors.add(p);
            return p;
        }
        void tick(LichUnit unit) {unit.tickCount++;unit.tick();LichPathScheduler.run();}
        void decide(LichUnit unit) {
            unit.tickCount+=Math.floorMod(-unit.getId()-unit.tickCount-1,5);
            tick(unit);
        }
        @Override public void close() {
            for(var unit:units){unit.routing.stop();unit.discard();}
            for(var p:survivors){p.discard();p.getScoreboard().removePlayerFromTeam(p.getScoreboardName());}
            phase.setScore(0);LichManager.update(owner.server,List.of());owner.discard();owner.getScoreboard().removePlayerFromTeam(owner.getScoreboardName());
        }
    }
    private record Cell(int x,int z,double cost) {}
    /** Independent Dijkstra on the known flat map: eight neighbours, no diagonal corner cutting. */
    private static double shortest(boolean[][] open,int sx,int sz,int tx,int tz) {
        int n=open.length;double[][] distance=new double[n][n];for(var row:distance)Arrays.fill(row,Double.POSITIVE_INFINITY);
        var queue=new PriorityQueue<Cell>(Comparator.comparingDouble(Cell::cost));distance[sx][sz]=0;queue.add(new Cell(sx,sz,0));
        while(!queue.isEmpty()) {
            var c=queue.remove();if(c.cost!=distance[c.x][c.z])continue;
            if(c.x==tx&&c.z==tz)return c.cost;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                if(dx==0&&dz==0)continue;int x=c.x+dx,z=c.z+dz;
                if(x<0||z<0||x>=n||z>=n||!open[x][z])continue;
                if(dx!=0&&dz!=0&&(!open[x][c.z]||!open[c.x][z]))continue;
                double cost=c.cost+Math.hypot(dx,dz);
                if(cost<distance[x][z]){distance[x][z]=cost;queue.add(new Cell(x,z,cost));}
            }
        }
        return Double.POSITIVE_INFINITY;
    }
    private static boolean[][] maze(int n,long seed) {
        var open=new boolean[n][n];var random=new Random(seed);var stack=new ArrayDeque<BlockPos>();
        open[1][1]=true;stack.push(new BlockPos(1,0,1));int[][] directions={{2,0},{-2,0},{0,2},{0,-2}};
        while(!stack.isEmpty()) {
            var cell=stack.peek();var choices=new ArrayList<int[]>();
            for(var d:directions){int x=cell.getX()+d[0],z=cell.getZ()+d[1];if(x>0&&z>0&&x<n-1&&z<n-1&&!open[x][z])choices.add(d);}
            if(choices.isEmpty()){stack.pop();continue;}
            var d=choices.get(random.nextInt(choices.size()));int x=cell.getX()+d[0],z=cell.getZ()+d[1];
            open[cell.getX()+d[0]/2][cell.getZ()+d[1]/2]=true;open[x][z]=true;stack.push(new BlockPos(x,0,z));
        }
        // Rooms and cycles prevent this from testing only a unique corridor.
        for(int i=0;i<n*n/12;i++)open[1+random.nextInt(n-2)][1+random.nextInt(n-2)]=true;
        return open;
    }
    private static net.minecraft.world.level.pathfinder.Path search(GameTestHelper h,LichUnit unit,Vec3 target,int slice) {
        var search=new LichPathSearch(unit,BlockPos.containing(target));int iterations=0;
        while(!search.done()&&iterations++<40000) {
            int before=search.polls();search.step(slice);
            h.assertTrue(search.polls()-before<=slice,"each resumable search honours its node-poll slice");
        }
        h.assertTrue(search.done(),"bounded search must terminate");return search.result();
    }
    private static double length(net.minecraft.world.level.pathfinder.Path path) {
        double result=0;for(int i=1;i<path.getNodeCount();i++)result+=path.getNode(i).distanceTo(path.getNode(i-1));return result;
    }
    private static void report(String name,Map<String,?> data) throws Exception {
        String json=new Gson().toJson(data);Maniacrev.LOGGER.info("LICH_NAV_BENCH {} {}",name,json);
        var directory=Path.of("navigation-reports");Files.createDirectories(directory);Files.writeString(directory.resolve(name+".json"),json+System.lineSeparator());
    }

    @GameTest(template="scp173_empty",batch="lich_nav_seeded_oracle",timeoutTicks=200)
    public static void seededMazesMatchIndependentShortestPaths(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_oracle",25,25)) {
            int checked=0;double worstRatio=0;long started=System.nanoTime();
            for(long seed:new long[]{7,19,42,81,137,2026}) {
                var open=maze(25,seed);
                for(int x=0;x<25;x++)for(int z=0;z<25;z++)for(int y=21;y<=24;y++)h.setBlock(x,y,z,open[x][z]?Blocks.AIR:Blocks.STONE);
                double optimal=shortest(open,1,1,23,23);h.assertTrue(Double.isFinite(optimal),"oracle maze must connect start and finish");
                for(var kind:LichRules.Kind.values()) {
                    var unit=arena.unit(kind,1,1,false);var path=search(h,unit,arena.point(23,23),37);
                    h.assertTrue(path!=null&&path.canReach(),"seed "+seed+" "+kind+" finds the complete maze route");
                    double cost=length(path);worstRatio=Math.max(worstRatio,cost/optimal);
                    h.assertTrue(Math.abs(cost-optimal)<.002,"A* should match independent Dijkstra on a flat map: seed="+seed+" kind="+kind+" cost="+cost+" optimal="+optimal);
                    for(int i=0;i<path.getNodeCount();i++) {
                        var point=path.getEntityPosAtNode(unit,i);var body=unit.getBoundingBox().move(point.subtract(unit.position())).deflate(.001);
                        h.assertTrue(h.getLevel().noCollision(unit,body),"every path node respects unit body clearance");
                    }
                    unit.discard();checked++;
                }
            }
            report("seeded-oracle",Map.of("seedCount",6,"queries",checked,"worstCostRatio",worstRatio,"elapsedMs",(System.nanoTime()-started)/1e6));h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_slices",timeoutTicks=100)
    public static void searchSlicesPreserveResultsAndImpossibleTargetsTerminate(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_slices",25,25)) {
            var open=maze(25,503);for(int x=0;x<25;x++)for(int z=0;z<25;z++)if(!open[x][z])arena.wall(x,z);
            var unit=arena.unit(LichRules.Kind.ZOMBIE,1,1,false);double expected=-1;int checked=0;
            for(int slice:new int[]{1,7,37,128,2048}) {
                var path=search(h,unit,arena.point(23,23),slice);h.assertTrue(path!=null&&path.canReach(),"slice "+slice+" completes the route");
                if(expected<0)expected=length(path);h.assertTrue(Math.abs(length(path)-expected)<.002,"slice size must not change path quality");checked++;
            }
            // Enclose the destination fully; proximity to its wall must not be returned as success.
            for(int x=21;x<=24;x++)for(int z=21;z<=24;z++)arena.wall(x,z);
            var impossible=search(h,unit,arena.point(23,23),17);
            h.assertTrue(impossible==null,"unreachable in-region target must terminate without a partial route to the wall");
            report("search-slices",Map.of("sliceSizes",List.of(1,7,37,128,2048),"reachableQueries",checked,"pathLength",expected,"unreachableTerminated",true));h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_stairs",timeoutTicks=100)
    public static void butcherTraversesRealStairsInBothDirections(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_stairs",19,13)) {
            var stairs=Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(
                    net.minecraft.world.level.block.StairBlock.FACING,net.minecraft.core.Direction.EAST);
            for(int x=6;x<10;x++) {
                for(int y=21;y<21+x-6;y++)h.setBlock(x,y,6,Blocks.STONE);
                h.setBlock(new BlockPos(x,21+x-6,6),stairs);
            }
            for(int x=10;x<=16;x++)for(int y=21;y<25;y++)h.setBlock(x,y,6,Blocks.STONE);
            for(int x=2;x<=16;x++)for(int z:new int[]{5,7})for(int y=21;y<=28;y++)h.setBlock(x,y,z,Blocks.STONE);
            var lower=arena.point(4,6);
            var upper=arena.point(14,6).add(0,4,0);
            var butcher=arena.unit(LichRules.Kind.BUTCHER,4,6,true);
            for(var goal:List.of(upper,lower)) {
                butcher.command(LichRules.Order.MOVE,goal,null);
                for(int tick=0;tick<400&&butcher.order!=LichRules.Order.STAND;tick++)arena.tick(butcher);
                var path=butcher.getNavigation().getPath();
                report("real-stairs",Map.of("goal",goal.toString(),"actual",butcher.position().toString(),"plans",
                        butcher.routing.plansCompleted(),"next",path==null||path.isDone()?"none":path.getNextNode().toString()));
                h.assertTrue(butcher.order==LichRules.Order.STAND&&LichNavigation.arrived(butcher.position(),goal,1),
                        "butcher reaches "+goal+" on real stairs, actual "+butcher.position());
            }
            h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_turn_stairs",timeoutTicks=100)
    public static void butcherTraversesTurningStairsToSlabLanding(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_turn_stairs",17,17)) {
            for(int x=2;x<=11;x++)for(int z=3;z<=15;z++)
                if(!(z==5&&x>=3&&x<=9||x==9&&z>=6&&z<=14))
                    for(int y=21;y<=29;y++)h.setBlock(x,y,z,Blocks.STONE);
            var stairs=Blocks.STONE_BRICK_STAIRS.defaultBlockState();
            for(int x=6;x<=8;x++) {
                for(int y=21;y<21+x-6;y++)h.setBlock(x,y,5,Blocks.STONE);
                h.setBlock(new BlockPos(x,21+x-6,5),stairs.setValue(
                        net.minecraft.world.level.block.StairBlock.FACING,net.minecraft.core.Direction.EAST));
            }
            for(int z=5;z<=14;z++) {
                int floor=z==5?24:z<=7?18+z:26;
                for(int y=21;y<floor;y++)h.setBlock(9,y,z,Blocks.STONE);
                if(z==6||z==7)h.setBlock(new BlockPos(9,floor,z),stairs.setValue(
                        net.minecraft.world.level.block.StairBlock.FACING,net.minecraft.core.Direction.SOUTH));
                if(z>=9)h.setBlock(9,26,z,Blocks.STONE_SLAB);
            }
            var butcher=arena.unit(LichRules.Kind.BUTCHER,4,5,true);
            for(var goal:List.of(arena.point(9,13).add(0,5.5,0),arena.point(4,5))) {
                butcher.command(LichRules.Order.MOVE,goal,null);
                for(int tick=0;tick<450&&butcher.order!=LichRules.Order.STAND;tick++)arena.tick(butcher);
                report("turning-stairs",Map.of("goal",goal.toString(),"actual",butcher.position().toString(),
                        "plans",butcher.routing.plansCompleted()));
                h.assertTrue(butcher.order==LichRules.Order.STAND&&LichNavigation.arrived(butcher.position(),goal,1),
                        "butcher reaches "+goal+" through turn and slabs, actual "+butcher.position());
            }
            h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_stair_clearance",timeoutTicks=100)
    public static void butcherChasesUpTallStairsAndRecoversWhenCeilingOpens(GameTestHelper h) {
        try(var arena=new Arena(h,"nav_stair_roof",18,17)) {
            var stair=Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(
                    net.minecraft.world.level.block.StairBlock.FACING,net.minecraft.core.Direction.EAST);
            for(int x=0;x<=17;x++)for(int z=0;z<=16;z++) {
                if(x==0||x==17||z==0||z==16) {
                    for(int y=21;y<=28;y++)h.setBlock(x,y,z,Blocks.STONE);
                } else if(x>=6) {
                    boolean lane=x<10&&(z==4||z==12);
                    for(int y=21;y<(lane?21+x-6:25);y++)h.setBlock(x,y,z,Blocks.STONE);
                    if(lane)h.setBlock(new BlockPos(x,21+x-6,z),stair);
                }
            }
            h.setBlock(7,25,4,Blocks.STONE);
            var target=arena.survivor(arena.point(14,4).add(0,4,0));
            var butcher=arena.unit(LichRules.Kind.BUTCHER,4,4,true);
            butcher.command(LichRules.Order.CHASE,target.position(),target.getUUID());
            boolean usedTall=false;
            for(int tick=0;tick<600&&butcher.distanceToSqr(target)>4;tick++) {
                arena.tick(butcher);
                usedTall|=butcher.getY()>target.getY()-1&&butcher.getZ()>arena.point(0,11).z;
            }
            h.assertTrue(usedTall&&butcher.distanceToSqr(target)<=4,"chase uses the stairs with 2.2 blocks of clearance: "+butcher.position());
            target.discard();butcher.discard();butcher.routing.stop();
            h.setBlock(7,25,12,Blocks.STONE);
            var waiting=arena.unit(LichRules.Kind.BUTCHER,4,4,true);
            var goal=arena.point(14,4).add(0,4,0);
            waiting.command(LichRules.Order.MOVE,goal,null);
            for(int tick=0;tick<300;tick++)arena.tick(waiting);
            h.assertTrue(waiting.order==LichRules.Order.MOVE&&waiting.routing.plansStarted()<=10,
                    "both low stairways leave the order pending with bounded retries");
            h.setBlock(7,25,4,Blocks.AIR);
            for(int tick=0;tick<500&&waiting.order!=LichRules.Order.STAND;tick++)arena.tick(waiting);
            h.assertTrue(waiting.order==LichRules.Order.STAND&&LichNavigation.arrived(waiting.position(),goal,1),
                    "removing a stair ceiling lets the existing order finish");
            h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_body",timeoutTicks=100)
    public static void lowDoorwayUsesDifferentRoutesForDifferentBodies(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_bodies",23,21)) {
            for(int z=0;z<21;z++)if(z!=5&&z!=16)arena.wall(10,z);
            h.setBlock(10,23,5,Blocks.STONE);h.setBlock(10,24,5,Blocks.STONE);
            int arrived=0;
            for(var kind:LichRules.Kind.values()) {
                var unit=arena.unit(kind,4,5,true);var goal=arena.point(17,5);unit.command(LichRules.Order.MOVE,goal,null);
                boolean tallGate=false;
                for(int tick=0;tick<650&&unit.order!=LichRules.Order.STAND;tick++) {
                    arena.tick(unit);tallGate|=unit.getZ()>=arena.point(0,15).z;
                    h.assertTrue(unit.getY()>goal.y-.5,"doorway route must remain on its supported floor");
                }
                h.assertTrue(LichNavigation.arrived(unit.position(),goal,1),kind+" physically reaches the other side: "+unit.position());
                h.assertTrue(tallGate==(kind==LichRules.Kind.BUTCHER),"only 2.2-block butcher needs the distant full-height doorway");
                unit.routing.stop();unit.discard();arrived++;
            }
            report("body-clearance",Map.of("physicalArrivals",arrived,"butcherUsedTallDetour",true));h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_lunge",timeoutTicks=100)
    public static void lungeUsesOrdinaryDamageAndAbortsWhenTargetDodgesOrIsHidden(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_lunge",20,16)) {
            var time=(net.minecraft.world.level.storage.ServerLevelData)h.getLevel().getLevelData();
            long start=time.getGameTime();
            var position=arena.point(5,7);
            var target=arena.survivor(position.add(3,0,0));
            var spawnShield=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnShield.setAccessible(true);spawnShield.setInt(target,0);
            var ordinaryDamage=new EnumMap<LichRules.Kind,Float>(LichRules.Kind.class);
            try {
                for(var kind:LichRules.Kind.values()) {
                    time.setGameTime(start+kind.ordinal()*20);
                    boolean canLunge=kind==LichRules.Kind.ZOMBIE||kind==LichRules.Kind.GHOUL;
                    if(canLunge) {
                        var reference=arena.unit(kind,5,7,true);
                        reference.setOnGround(true);reference.setDeltaMovement(0,-.08,0);
                        reference.command(LichRules.Order.STAND,position,null);
                        target.setPos(position.add(2,0,0));
                        target.setHealth(target.getMaxHealth());target.invulnerableTime=0;target.hurtTime=0;
                        arena.decide(reference);
                        float damage=target.getMaxHealth()-target.getHealth();
                        h.assertTrue(damage>0&&reference.nextAttack==time.getGameTime()+kind.interval,
                                "the fixture accepts ordinary melee through the existing damage rules");
                        ordinaryDamage.put(kind,damage);
                        reference.routing.stop();reference.discard();
                    }
                    var unit=arena.unit(kind,5,7,true);
                    unit.setOnGround(true);unit.setDeltaMovement(0,-.08,0);
                    unit.command(LichRules.Order.STAND,position,null);
                    target.setPos(position.add(3,0,0));target.setHealth(target.getMaxHealth());target.invulnerableTime=0;
                    target.hurtTime=0;
                    arena.decide(unit);
                    h.assertTrue(unit.lunge.active()==canLunge,"only zombie and ghoul start a lunge at the 3-block boundary: "+kind
                            +" ground="+unit.onGround()+" sight="+unit.trackedVisible(target)+" distance²="+unit.distanceToSqr(target));
                    if(canLunge) {
                        long hitCooldown=0;
                        for(int tick=0;tick<8;tick++) {
                            time.setGameTime(time.getGameTime()+1);arena.tick(unit);
                            if(hitCooldown==0)hitCooldown=unit.nextAttack;
                            h.assertTrue(unit.nextAttack==hitCooldown,"a lunge and ordinary melee share one attack timer");
                        }
                        h.assertTrue(target.getHealth()==target.getMaxHealth()-ordinaryDamage.get(kind)&&unit.nextAttack>time.getGameTime(),
                                "lunge crosses into normal reach and hits once: "+kind+" hp="+target.getHealth()
                                        +" attack timer="+unit.nextAttack+" distance²="+unit.distanceToSqr(target));
                    }
                    unit.routing.stop();unit.discard();
                }
                for(String obstacle:List.of("dodge","wall","missing support","different floor","beyond 3","normal reach","lost target")) {
                    var unit=arena.unit(LichRules.Kind.ZOMBIE,5,7,true);
                    unit.setOnGround(true);unit.setDeltaMovement(0,-.08,0);unit.command(LichRules.Order.STAND,position,null);
                    target.setPos(switch(obstacle) {
                        case "different floor" -> position.add(1,2,0);
                        case "beyond 3" -> position.add(3.01,0,0);
                        case "normal reach" -> position.add(2,0,0);
                        default -> position.add(2.8,0,0);
                    });
                    target.setHealth(target.getMaxHealth());target.invulnerableTime=0;target.hurtTime=0;
                    time.setGameTime(time.getGameTime()+20);
                    arena.decide(unit);
                    boolean started=Set.of("dodge","wall","missing support","lost target").contains(obstacle);
                    h.assertTrue(unit.lunge.active()==started,"initial lunge eligibility: "+obstacle);
                    if(obstacle.equals("normal reach"))h.assertTrue(unit.nextAttack>time.getGameTime()
                            &&target.getHealth()==target.getMaxHealth()-ordinaryDamage.get(unit.kind()),"ordinary melee applies inside two blocks");
                    if(started) {
                        if(obstacle.equals("dodge"))target.setPos(position.add(2.8,0,2.8));
                        if(obstacle.equals("wall"))arena.wall(6,7);
                        if(obstacle.equals("missing support"))h.setBlock(6,20,7,Blocks.AIR);
                        if(obstacle.equals("lost target"))target.setGameMode(GameType.SPECTATOR);
                        for(int tick=0;tick<5;tick++){time.setGameTime(time.getGameTime()+1);arena.tick(unit);}
                        h.assertTrue(!unit.lunge.active()&&unit.lunge.cooldown()>0&&target.getHealth()==target.getMaxHealth(),
                                "a failed lunge causes no damage and still spends its cooldown: "+obstacle);
                        if(obstacle.equals("wall"))for(int y=21;y<=24;y++)h.setBlock(6,y,7,Blocks.AIR);
                        if(obstacle.equals("missing support"))h.setBlock(6,20,7,Blocks.STONE);
                        target.setGameMode(GameType.SURVIVAL);
                    }
                    unit.routing.stop();unit.discard();
                }
            } finally {time.setGameTime(start);}
            h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_lunge_recovery",timeoutTicks=100)
    public static void lungeCooldownUsesIndividualVisibleSprintTime(GameTestHelper h) {
        try(var arena=new Arena(h,"nav_lunge_cd",20,16)) {
            var p=arena.point(5,7);
            var target=arena.survivor(p.add(2.8,0,0));
            for(String mode:List.of("stationary","sprinting","occluded","out of range")) {
                var unit=arena.unit(LichRules.Kind.GHOUL,5,7,true);
                unit.setOnGround(true);unit.setDeltaMovement(0,-.08,0);target.setPos(p.add(2.8,0,0));target.setSprinting(false);
                unit.command(LichRules.Order.CHASE,target.position(),target.getUUID());
                arena.decide(unit);
                h.assertTrue(unit.lunge.active()&&unit.lunge.cooldown()==400,"each ghoul starts with its own full cooldown");
                unit.lunge.cancel();
                unit.nextAttack=Long.MAX_VALUE;
                boolean sprint=mode.equals("sprinting");
                if(mode.equals("occluded"))arena.wall(6,7);
                int duration=sprint?80:100;
                target.setSprinting(!mode.equals("stationary"));
                for(int tick=1;tick<=duration;tick++) {
                    unit.setPos(p);unit.setDeltaMovement(Vec3.ZERO);unit.setOnGround(true);
                    target.setPos(p.add(mode.equals("out of range")?4:2.8,0,mode.equals("stationary")?0:tick%2*.2));
                    arena.tick(unit);
                    h.assertTrue((unit.lunge.cooldown()==0)==(tick==duration),"expected "+duration+" recovery ticks: "+mode+" at "+tick);
                }
                if(mode.equals("occluded"))for(int y=21;y<=24;y++)h.setBlock(6,y,7,Blocks.AIR);
                unit.routing.stop();unit.discard();
            }
            h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_hidden",timeoutTicks=100)
    public static void tracksCloseCoverWithoutUpdatingDirectSightOrAttacking(GameTestHelper h) {
        try(var arena=new Arena(h,"nav_hidden",18,16)) {
            var time=(net.minecraft.world.level.storage.ServerLevelData)h.getLevel().getLevelData();
            long start=time.getGameTime();
            var unit=arena.unit(LichRules.Kind.GHOUL,2,6,true);
            unit.setNoAi(true);
            var hidden=arena.point(6,6).add(-.2,0,0);
            var target=arena.survivor(hidden);
            var visible=hidden.add(0,0,2);
            arena.wall(5,6);
            try {
                var memory=new LichPursuit();
                memory.observe(target.getUUID(),visible,Vec3.ZERO,start);
                h.assertTrue(memory.remembers(target.getUUID(),visible.add(3,0,0),start+39)
                        &&!memory.remembers(target.getUUID(),visible.add(3.01,0,0),start+39)
                        &&!memory.remembers(UUID.randomUUID(),visible,start+39),"memory is bounded to three blocks and one target");
                unit.command(LichRules.Order.STAND,unit.position(),null);
                arena.decide(unit);
                h.assertTrue(unit.combatTarget==null,"a distant hidden player cannot be acquired");
                target.setPos(visible);arena.decide(unit);
                var observed=unit.lastSeen;
                h.assertTrue(visible.equals(observed)&&unit.detecting,"direct sight records the actual observation");
                target.setPos(hidden);
                time.setGameTime(start+39);arena.decide(unit);
                h.assertTrue(hidden.equals(unit.routing.goal())&&observed.equals(unit.lastSeen)&&!unit.detecting,
                        "within 3 blocks of the last sighting the same hidden target steers movement only");
                time.setGameTime(start+40);arena.decide(unit);
                h.assertTrue(!hidden.equals(unit.routing.goal())&&observed.equals(unit.lastSeen),
                        "after 40 ticks the normal lost-target search resumes");
                unit.setPos(arena.point(4,6).add(-.1,0,0));unit.setDeltaMovement(Vec3.ZERO);
                unit.command(LichRules.Order.STAND,unit.position(),null);arena.decide(unit);
                h.assertTrue(target.getUUID().equals(unit.combatTarget)&&hidden.equals(unit.routing.goal())&&unit.lastSeen==null
                        &&unit.nextAttack==0&&!unit.lunge.active(),"a nearby hidden player gives a movement hint but no hit or direct observation");
                var floor=unit.position();
                h.setBlock(new BlockPos(4,22,6),Blocks.STONE_SLAB.defaultBlockState().setValue(
                        net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.TOP));
                target.setPos(floor.add(0,2,0));
                unit.command(LichRules.Order.STAND,unit.position(),null);arena.decide(unit);
                h.assertTrue(unit.combatTarget==null,"a player separated vertically by a floor is not detected");
            } finally {time.setGameTime(start);}
            h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_bridge",timeoutTicks=100)
    public static void allKindsCrossNarrowBridgeInsteadOfFallingIntoPit(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_bridge",25,21)) {
            for(int x=8;x<=15;x++)for(int z=0;z<21;z++)if(z!=14&&z!=15)h.setBlock(x,20,z,Blocks.AIR);
            int arrivals=0;
            for(var kind:LichRules.Kind.values()) {
                var unit=arena.unit(kind,3,5,true);var goal=arena.point(20,5);unit.command(LichRules.Order.MOVE,goal,null);boolean bridge=false;
                for(int tick=0;tick<650&&unit.order!=LichRules.Order.STAND;tick++) {
                    arena.tick(unit);bridge|=unit.getX()>arena.point(8,0).x&&unit.getX()<arena.point(15,0).x&&unit.getZ()>=arena.point(0,14).z-.5;
                    h.assertTrue(unit.getY()>=goal.y-.2,"unit must not take an unsupported shortcut into the pit: "+kind);
                }
                h.assertTrue(bridge&&LichNavigation.arrived(unit.position(),goal,1),kind+" must physically cross the offset bridge");
                unit.routing.stop();unit.discard();arrivals++;
            }
            report("bridge",Map.of("physicalArrivals",arrivals,"falls",0));h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_dynamic",timeoutTicks=100)
    public static void newWallInterruptsDirectSteeringAndOpeningRestoresIt(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_dynamic",30,19)) {
            for(var kind:LichRules.Kind.values()) {
                var unit=arena.unit(kind,3,5,true);var goal=arena.point(25,5);unit.command(LichRules.Order.MOVE,goal,null);
                for(int i=0;i<12;i++)arena.tick(unit);
                h.assertTrue(unit.routing.plansStarted()==0,"initial flat movement must avoid A*");
                int wallX=12;for(int z=0;z<=11;z++)arena.wall(wallX,z);boolean detour=false;
                for(int tick=0;tick<600&&unit.order!=LichRules.Order.STAND;tick++) {
                    arena.tick(unit);detour|=unit.getZ()>=arena.point(0,12).z-.5;
                }
                h.assertTrue(detour&&LichNavigation.arrived(unit.position(),goal,1),kind+" rebuilds after a wall appears during direct movement");
                for(int z=0;z<=11;z++)for(int y=21;y<=24;y++)h.setBlock(wallX,y,z,Blocks.AIR);
                unit.setDeltaMovement(Vec3.ZERO);unit.setPos(arena.point(3,5));unit.setOnGround(true);
                int before=unit.routing.plansStarted();unit.command(LichRules.Order.MOVE,goal,null);
                for(int tick=0;tick<500&&unit.order!=LichRules.Order.STAND;tick++)arena.tick(unit);
                h.assertTrue(LichNavigation.arrived(unit.position(),goal,1)&&unit.routing.plansStarted()==before,"removed wall restores zero-search direct travel");
                unit.routing.stop();unit.discard();
            }
            report("dynamic-wall",Map.of("kinds",4,"newWallsDetoured",4,"openingsWithoutSearch",4));h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_corridor_mutation",timeoutTicks=100)
    public static void corridorSeesBlockAndEntityChangesWithinTheSameTick(GameTestHelper h) {
        try(var arena=new Arena(h,"nav_mutations",23,15)) {
            var unit=arena.unit(LichRules.Kind.ZOMBIE,4,7,false);
            var start=unit.position();
            var end=arena.point(18,7);
            long now=h.getLevel().getGameTime();
            h.assertTrue(unit.routing.corridor(start,end),"floor initially clear");
            h.setBlock(10,21,7,Blocks.STONE);
            h.assertTrue(!unit.routing.corridor(start,end),"new obstruction takes effect on the same tick");
            h.setBlock(10,21,7,Blocks.AIR);
            h.setBlock(10,20,7,Blocks.AIR);
            h.assertTrue(!unit.routing.corridor(start,end),"missing support takes effect on the same tick");
            h.setBlock(10,20,7,Blocks.MAGMA_BLOCK);
            h.assertTrue(!unit.routing.corridor(start,end),"new hazard takes effect on the same tick");
            h.setBlock(10,20,7,Blocks.STONE);
            var boat=net.minecraft.world.entity.EntityType.BOAT.create(h.getLevel());
            boat.setPos(arena.point(10,10));h.getLevel().addFreshEntity(boat);
            try {
                h.assertTrue(unit.routing.corridor(start,end),"restored floor and boat off route");
                boat.setPos(arena.point(10,7));
                h.assertTrue(!unit.routing.corridor(start,end),"moving entity invalidates live body clearance");
                boat.setPos(arena.point(10,10));
                h.assertTrue(unit.routing.corridor(start,end)&&h.getLevel().getGameTime()==now,"the corridor clears within the same world tick");
            } finally {boat.discard();}
            h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_cancel",timeoutTicks=100)
    public static void cancellationsAndRemovedUnitsLeaveNoQueuedWork(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_cancel",25,25)) {
            var open=maze(25,919);for(int x=0;x<25;x++)for(int z=0;z<25;z++)if(!open[x][z])arena.wall(x,z);
            var jobs=new ArrayList<LichUnit>();
            for(int i=0;i<64;i++){var unit=arena.unit(LichRules.Kind.ZOMBIE,1,1,false);jobs.add(unit);unit.routing.move(arena.point(23,23));}
            h.assertTrue(LichPathScheduler.queuedCount()==64,"all requests enqueue exactly once");
            for(int i=0;i<64;i++) {
                var unit=jobs.get(i);
                if(i%3==0)unit.command(LichRules.Order.STAND,unit.position(),null);
                else if(i%3==1)unit.discard();
                else {unit.routing.move(arena.point(23,21));unit.routing.move(arena.point(23,19));}
            }
            int rounds=0;while(LichPathScheduler.queuedCount()>0&&rounds++<600)LichPathScheduler.run();
            h.assertTrue(LichPathScheduler.queuedCount()==0,"cancelled, removed and finished jobs all drain from the scheduler");
            for(int i=0;i<64;i++) {
                var unit=jobs.get(i);
                h.assertTrue(unit.routing.plansStarted()==1,"coalescing must not duplicate pending tasks");
                if(i%3!=2)h.assertTrue(unit.routing.searchPolls()==0,"cancelled/removed jobs must perform no subsequent search work");
                else h.assertTrue(unit.routing.plansCompleted()==1,"remaining jobs complete despite cancellation churn");
            }
            report("cancellation",Map.of("jobs",64,"drainedInFrames",rounds,"queuedAfter",LichPathScheduler.queuedCount()));h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_hazards",timeoutTicks=100)
    public static void allKindsAvoidLavaAndMagma(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_hazards",26,21)) {
            for(int x=9;x<=15;x++)for(int z=5;z<=13;z++)h.setBlock(x,20,z,Blocks.LAVA);
            for(int x=8;x<=16;x++){h.setBlock(x,20,4,Blocks.MAGMA_BLOCK);h.setBlock(x,20,14,Blocks.MAGMA_BLOCK);}
            for(var kind:LichRules.Kind.values()) {
                var unit=arena.unit(kind,3,9,true);var goal=arena.point(22,9);unit.command(LichRules.Order.MOVE,goal,null);
                float health=unit.getHealth();boolean around=false;
                for(int tick=0;tick<700&&unit.order!=LichRules.Order.STAND;tick++) {
                    arena.tick(unit);around|=unit.getZ()<arena.point(0,4).z||unit.getZ()>arena.point(0,14).z;
                    h.assertTrue(!unit.isInLava()&&!unit.isOnFire()&&unit.getHealth()==health,"hazard route must avoid lava/fire/magma damage: "+kind);
                }
                h.assertTrue(around&&LichNavigation.arrived(unit.position(),goal,1),kind+" physically takes the safe detour");
                unit.routing.stop();unit.discard();
            }
            report("hazards",Map.of("physicalArrivals",4,"damageTaken",0));h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_distant",timeoutTicks=100)
    public static void allKindsContinueBeyondSingleSearchRadius(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_distant",105,9)) {
            var metrics=new LinkedHashMap<String,Map<String,Integer>>();
            for(boolean prefetch:new boolean[]{false,true})for(var kind:LichRules.Kind.values()) {
                var unit=arena.unit(kind,3,4,true);var goal=arena.point(98,4);
                unit.tickCount=Math.floorMod(-unit.getId(),5);
                unit.command(LichRules.Order.MOVE,goal,null);
                unit.routing.prefetchEnabled=prefetch;
                int firstStep=-1,paused=0,tick=0;
                for(;tick<1400&&unit.order!=LichRules.Order.STAND;tick++) {
                    Vec3 before=unit.position();
                    arena.tick(unit);
                    boolean moved=before.subtract(unit.position()).horizontalDistanceSqr()>.0004;
                    if(moved&&firstStep<0)firstStep=tick+1;
                    if(!moved&&firstStep>=0&&unit.order!=LichRules.Order.STAND)paused++;
                }
                h.assertTrue(LichNavigation.arrived(unit.position(),goal,1),kind+" continues through partial routes for a 95-block trip: "+unit.position());
                h.assertTrue(unit.routing.plansCompleted()>=2,"destination outside ±64 needs more than one bounded search");
                metrics.put(kind.name()+"/"+prefetch,Map.of("plans",unit.routing.plansCompleted(),"polls",unit.routing.searchPolls(),
                        "firstStep",firstStep,"pausedTicks",paused,"arrivalTick",tick));
                unit.routing.stop();unit.discard();
            }
            for(var kind:LichRules.Kind.values()) {
                var before=metrics.get(kind.name()+"/false");var after=metrics.get(kind.name()+"/true");
                h.assertTrue(after.get("plans")<=before.get("plans"),"prefetch does not duplicate searches: "+kind);
            }
            report("distant-target",Map.of("distanceBlocks",95,"prefetchComparison",metrics));h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_prefetch_priority",timeoutTicks=100)
    public static void prefetchYieldsToCombatAndOrders(GameTestHelper h) {
        try(var arena=new Arena(h,"nav_prefetch",105,9)) {
            var goal=arena.point(98,4);
            var traveller=arena.unit(LichRules.Kind.GHOUL,3,4,true);
            traveller.command(LichRules.Order.MOVE,goal,null);
            for(int tick=0;tick<900&&!traveller.routing.speculative();tick++) {
                traveller.tickCount++;traveller.tick();
                if(!traveller.routing.speculative())LichPathScheduler.run();
            }
            h.assertTrue(traveller.routing.speculative(),"a second segment is queued while the first is still in use");
            var combat=arena.unit(LichRules.Kind.ZOMBIE,3,4,false);
            combat.combatTarget=UUID.randomUUID();
            var order=arena.unit(LichRules.Kind.ZOMBIE,3,4,false);
            combat.routing.move(goal);order.routing.move(goal);
            int work=traveller.routing.searchPolls(),frames=0;
            while((combat.routing.pending()||order.routing.pending())&&frames++<100) {
                LichPathScheduler.run();
                if(combat.routing.pending()||order.routing.pending())
                    h.assertTrue(traveller.routing.searchPolls()==work,"speculation waits while regular work needs the budget");
            }
            h.assertTrue(combat.routing.plansCompleted()==1&&order.routing.plansCompleted()==1,"both regular queues finish");
            for(int i=0;i<100&&traveller.routing.pending();i++)LichPathScheduler.run();
            h.assertTrue(traveller.routing.plansCompleted()==2,"spare capacity completes the speculative segment");
            h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_unreachable_retry",timeoutTicks=100)
    public static void unreachableGoalBacksOffAndRecoversWhenDoorOpens(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_retry",21,21)) {
            for(int z=0;z<21;z++)arena.wall(10,z);
            // Bound the reachable region: the wall cannot be circumvented outside the arena.
            for(int x=0;x<21;x++){arena.wall(x,0);arena.wall(x,20);}
            for(int z=0;z<21;z++){arena.wall(0,z);arena.wall(20,z);}
            var unit=arena.unit(LichRules.Kind.ZOMBIE,4,9,true);var goal=arena.point(16,9);unit.command(LichRules.Order.MOVE,goal,null);
            for(int i=0;i<300;i++)arena.tick(unit);
            h.assertTrue(unit.order==LichRules.Order.MOVE&&!LichNavigation.arrived(unit.position(),goal,1),"closed wall must not complete the order");
            h.assertTrue(unit.routing.plansStarted()<=10,"unreachable route retries must back off instead of searching every decision: "+unit.routing.plansStarted());
            int retries=unit.routing.plansStarted();
            for(int y=21;y<=24;y++)h.setBlock(10,y,9,Blocks.AIR);
            for(int i=0;i<400&&unit.order!=LichRules.Order.STAND;i++)arena.tick(unit);
            h.assertTrue(LichNavigation.arrived(unit.position(),goal,1),"opening a doorway must revive a failed movement order");
            report("retry-backoff",Map.of("closedTicks",300,"searchesWhileClosed",retries,"recoveredAfterOpening",true));h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_load",timeoutTicks=200)
    public static void loadSweepBoundsWorkAndDoesNotStarveOrders(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_load",25,25)) {
            var open=maze(25,137);for(int x=0;x<25;x++)for(int z=0;z<25;z++)if(!open[x][z])arena.wall(x,z);
            var results=new ArrayList<Map<String,Object>>();int pollsPerGroupOf16=-1;
            for(int count:new int[]{16,64,128}) {
                var jobs=new ArrayList<LichUnit>();
                for(int i=0;i<count;i++) {
                    var unit=arena.unit(LichRules.Kind.values()[i%4],1,1,false);jobs.add(unit);
                    if(i%4!=3)unit.combatTarget=UUID.randomUUID();unit.routing.move(arena.point(23,23));
                }
                var timings=new ArrayList<Double>();int frames=0,totalPolls=0,peakPolls=0,firstNormal=-1,firstCombat=-1;
                while(jobs.stream().anyMatch(unit->unit.routing.plansCompleted()==0)&&frames<2000) {
                    frames++;long start=System.nanoTime();LichPathScheduler.run();timings.add((System.nanoTime()-start)/1e6);
                    int polls=LichPathScheduler.lastPolls();totalPolls+=polls;peakPolls=Math.max(peakPolls,polls);
                    h.assertTrue(polls<=LichPathScheduler.BUDGET,"total budget applies across the whole army");
                    for(int i=0;i<count;i++)if(jobs.get(i).routing.searchPolls()>0) {
                        if(i%4==3&&firstNormal<0)firstNormal=frames;
                        if(i%4!=3&&firstCombat<0)firstCombat=frames;
                    }
                    for(var unit:jobs) {unit.tickCount++;if(unit.routing.plansCompleted()==0)unit.routing.move(arena.point(23,23).add(0,0,(frames%3)*-.4));}
                }
                h.assertTrue(frames<2000&&firstNormal>0&&firstCombat>0,"both priority classes must receive service and complete");
                for(var unit:jobs)h.assertTrue(unit.routing.plansStarted()==1&&unit.routing.plansCompleted()==1,"every load request completes once, with no restart storm");
                h.assertTrue(LichPathScheduler.queuedCount()==0,"completed load does not retain queued jobs");
                if(pollsPerGroupOf16<0)pollsPerGroupOf16=totalPolls;
                h.assertTrue(totalPolls==pollsPerGroupOf16*(count/16),"increasing army size must scale total search work linearly, without duplicated calculations");
                for(int idle=0;idle<10;idle++){LichPathScheduler.run();h.assertTrue(LichPathScheduler.lastPolls()==0,"an idle planner performs no search work");}
                Collections.sort(timings);var metrics=new LinkedHashMap<String,Object>();
                metrics.put("units",count);metrics.put("frames",frames);metrics.put("totalPolls",totalPolls);metrics.put("peakPolls",peakPolls);
                metrics.put("firstCombatFrame",firstCombat);metrics.put("firstOrderFrame",firstNormal);
                metrics.put("schedulerMsP50",timings.get(timings.size()/2));metrics.put("schedulerMsP95",timings.get(Math.min(timings.size()-1,(int)(timings.size()*.95))));
                metrics.put("schedulerMsMax",timings.get(timings.size()-1));results.add(metrics);
                for(var unit:jobs){unit.routing.stop();unit.discard();}
            }
            report("load-sweep",Map.of("results",results,"note","Server-thread synthetic planning workload; times include JVM warmup/GC; not a TPS or crowd physics benchmark."));h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_direct_load",timeoutTicks=200)
    public static void openGroundArmyUsesNoSearchAndReportsSteeringCost(GameTestHelper h) throws Exception {
        try(var arena=new Arena(h,"nav_direct_load",27,11)) {
            var jobs=new ArrayList<LichUnit>();
            for(int i=0;i<128;i++)jobs.add(arena.unit(LichRules.Kind.values()[i%4],3,2+i%6,false));
            var decisions=new ArrayList<Double>();var refreshes=new ArrayList<Double>();
            for(int frame=0;frame<100;frame++) {
                long start=System.nanoTime();
                for(int i=0;i<jobs.size();i++) {
                    var unit=jobs.get(i);unit.tickCount++;
                    if(frame%5==0)unit.routing.move(arena.point(23,2+i%6).add(0,0,Math.sin(frame*.1)*.25));
                    unit.routing.tick();
                    h.assertTrue(unit.routing.plansStarted()==0,"flat-ground army must never enqueue A*");
                }
                double ms=(System.nanoTime()-start)/1e6;
                if(frame>=20)(frame%5==0?decisions:refreshes).add(ms);
                h.assertTrue(LichPathScheduler.queuedCount()==0,"direct steering leaves the planner idle");
            }
            Collections.sort(decisions);Collections.sort(refreshes);
            var staggered=new ArrayList<Double>();
            for(int frame=0;frame<100;frame++) {
                long start=System.nanoTime();
                for(int i=0;i<jobs.size();i++) {
                    var unit=jobs.get(i);unit.tickCount++;
                    if((frame+i)%5==0)unit.routing.move(arena.point(23,2+i%6).add(0,0,Math.sin(frame*.1)*.25));
                    unit.routing.tick();
                }
                if(frame>=20)staggered.add((System.nanoTime()-start)/1e6);
            }
            h.assertTrue(jobs.stream().allMatch(unit->unit.routing.plansStarted()==0)&&LichPathScheduler.queuedCount()==0,"staggered direct steering must also remain search-free");
            Collections.sort(staggered);
            var metrics=new LinkedHashMap<String,Object>(Map.of("units",128,"framesPerMode",100,"searches",0,
                    "decisionFrameMsP50",decisions.get(decisions.size()/2),"decisionFrameMsMax",decisions.get(decisions.size()-1),
                    "refreshFrameMsP50",refreshes.get(refreshes.size()/2),"refreshFrameMsP95",refreshes.get((int)(refreshes.size()*.95)),
                    "note","Stationary isolated entities; measures corridor validation and steering only, without crowd physics or network."));
            metrics.put("staggeredFrameMsP50",staggered.get(staggered.size()/2));metrics.put("staggeredFrameMsP95",staggered.get((int)(staggered.size()*.95)));
            metrics.put("decisionFrameMsP95",decisions.get((int)(decisions.size()*.95)));
            metrics.put("staggeredFrameMsMax",staggered.get(staggered.size()-1));report("direct-steering-load",metrics);
            h.succeed();
        }
    }

    @GameTest(template="scp173_empty",batch="lich_nav_real_server_ticks",timeoutTicks=100)
    public static void forgeServerTicksAdvanceQueueWithoutManualPumping(GameTestHelper h) {
        var arena=new Arena(h,"nav_real_ticks",25,25);
        try {
            var open=maze(25,137);for(int x=0;x<25;x++)for(int z=0;z<25;z++)if(!open[x][z])arena.wall(x,z);
            var jobs=new ArrayList<LichUnit>();
            for(int i=0;i<16;i++) {
                var unit=arena.unit(LichRules.Kind.values()[i%4],1,1,false);jobs.add(unit);unit.routing.move(arena.point(23,23));
            }
            // No helper tick()/scheduler.run(): this must be serviced by Forge's actual END event.
            h.runAfterDelay(40,()->{
                try {
                    for(var unit:jobs)h.assertTrue(unit.routing.plansCompleted()==1,"registered Forge server tick advances every pending query");
                    h.assertTrue(LichPathScheduler.queuedCount()==0,"real server event drains the queue");
                    Maniacrev.LOGGER.info("LICH_NAV_REAL_TICKS: 16 queries completed through actual Forge events without manual pumping");h.succeed();
                } finally {arena.close();}
            });
        } catch(RuntimeException|Error failure) {arena.close();throw failure;}
    }
}
