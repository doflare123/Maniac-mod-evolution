package org.example.maniacrevolution.lich.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.lich.*;

/** Per-listener audio budgets only; never limits entities, damage or commands. */
@Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT)
public final class LichAudio {
    private static final class Seen {int emerge=100,hurt;boolean swinging,dead;long step;Following background;}
    private static final class Cast {long serial;Following sound;Cast(long serial,Following sound){this.serial=serial;this.sound=sound;}}
    private static final Map<UUID,Seen> UNITS=new HashMap<>();
    private static final Map<UUID,Cast> CASTS=new HashMap<>();
    private static final List<Following> VOICES=new ArrayList<>();
    private static final int[] USED=new int[4];
    private static long window,uiUntil;
    private static Object level;
    private static final class Following extends AbstractTickableSoundInstance {
        final Entity entity;final long until;final int family;
        Following(Entity entity,SoundEvent sound,float volume,long until,int family){super(sound,SoundSource.HOSTILE,RandomSource.create());this.entity=entity;this.volume=volume;this.pitch=1;this.until=until;this.family=family;position();}
        private void position(){x=entity.getX();y=entity.getY()+.6;z=entity.getZ();}
        @Override public void tick(){if(entity.isRemoved()||Minecraft.getInstance().level!=entity.level()||entity.level().getGameTime()>=until)stop();else position();}
    }
    public static void ui(String name){var mc=Minecraft.getInstance();if(mc.level==null||mc.level.getGameTime()<uiUntil)return;uiUntil=mc.level.getGameTime()+3;
        var event=ForgeRegistries.SOUND_EVENTS.getValue(Maniacrev.loc("lich."+name));if(event!=null)mc.getSoundManager().play(SimpleSoundInstance.forUI(event,1,.45F));}
    private static Following play(Entity entity,String id,float volume,int ticks,int family){
        var event=ForgeRegistries.SOUND_EVENTS.getValue(id.contains(":")?new net.minecraft.resources.ResourceLocation(id):Maniacrev.loc("lich."+id));if(event==null)return null;
        var sound=new Following(entity,event,volume,entity.level().getGameTime()+ticks,family);VOICES.add(sound);Minecraft.getInstance().getSoundManager().play(sound);return sound;
    }
    private static void stop(Following sound){if(sound!=null)Minecraft.getInstance().getSoundManager().stop(sound);}
    private static boolean budget(int family,int maximum,int simultaneous){
        var mc=Minecraft.getInstance();long now=mc.level.getGameTime();if(now-window>=20||now<window){window=now;Arrays.fill(USED,0);}
        VOICES.removeIf(s->!mc.getSoundManager().isActive(s));
        if(USED[family]>=maximum||VOICES.stream().filter(s->s.family==family).count()>=simultaneous)return false;USED[family]++;return true;
    }
    public static void forms(CompoundTag tag){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;var present=new HashSet<UUID>();
        for(var value:tag.getList("rows",Tag.TAG_COMPOUND)){
            var row=(CompoundTag)value;UUID id=row.getUUID("id");String action=row.getString("cast");var p=mc.level.getPlayerByUUID(id);
            if(action.isEmpty()||p==null)continue;present.add(id);long serial=row.getLong("finish");var old=CASTS.get(id);
            if(old!=null&&old.serial==serial)continue;if(old!=null)stop(old.sound);
            Following sound=null;if(p.distanceToSqr(mc.player)<=256&&serial-mc.level.getGameTime()>=5)sound=play(p,"cast."+action,.65F,(int)(serial-mc.level.getGameTime()),-1);
            CASTS.put(id,new Cast(serial,sound));
        }
        CASTS.entrySet().removeIf(e->{if(present.contains(e.getKey()))return false;stop(e.getValue().sound);return true;});
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level!=level){reset();level=mc.level;}if(mc.level==null||mc.player==null)return;
        VOICES.removeIf(sound->!mc.getSoundManager().isActive(sound));
        var live=new HashSet<UUID>();
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof LichUnit u){
            live.add(u.getUUID());boolean first=!UNITS.containsKey(u.getUUID());var seen=UNITS.computeIfAbsent(u.getUUID(),id->new Seen());long now=mc.level.getGameTime();
            boolean ghoul=u.kind()==LichRules.Kind.GHOUL,butcher=u.kind()==LichRules.Kind.BUTCHER,skeleton=u.kind()==LichRules.Kind.SKELETON;String prefix=ghoul?"ghoul":butcher?"butcher":skeleton?"minecraft:entity.skeleton":"minecraft:entity.zombie";
            if(u.emerging()>0&&!u.isDeadOrDying()&&seen.background==null&&seen.emerge==100&&u.emerging()>=95&&u.distanceToSqr(mc.player)<=144&&budget(3,2,2))seen.background=play(u,"emerge",.5F,u.emerging(),3);
            if(u.emerging()==0||u.isDeadOrDying()){stop(seen.background);seen.background=null;}
            if(!first&&seen.emerge>0&&u.emerging()==0&&u.distanceToSqr(mc.player)<=144&&budget(0,8,4))play(u,"emerge.ready",.5F,12,0);
            if(!seen.dead&&u.isDeadOrDying()&&u.distanceToSqr(mc.player)<=144&&budget(1,3,2))play(u,prefix+".death",.6F,24,1);
            if(u.hurtTime>seen.hurt&&!u.isDeadOrDying()&&u.distanceToSqr(mc.player)<=144&&budget(0,8,4))play(u,prefix+".hurt",.5F,8,0);
            if(u.swinging&&!seen.swinging&&u.emerging()==0&&u.distanceToSqr(mc.player)<=144&&budget(0,8,4)){
                if(ghoul||butcher)play(u,prefix+".attack",.6F,butcher?30:14,0);
                else if(skeleton)play(u,"minecraft:entity.skeleton.shoot",.4F,12,0);
            }
            if(!u.isDeadOrDying()&&u.emerging()==0&&u.walkAnimation.speed()>.03&&now>=seen.step&&u.distanceToSqr(mc.player)<=64&&budget(2,6,2)){
                play(u,prefix+".step",.4F,8,2);seen.step=now+(butcher?12:6);
            }
            seen.emerge=u.emerging();seen.hurt=u.hurtTime;seen.swinging=u.swinging;seen.dead=u.isDeadOrDying();
        }
        UNITS.entrySet().removeIf(entry->{if(live.contains(entry.getKey()))return false;stop(entry.getValue().background);return true;});
    }
    private static void reset(){VOICES.forEach(LichAudio::stop);VOICES.clear();CASTS.clear();UNITS.clear();Arrays.fill(USED,0);window=uiUntil=0;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){reset();level=null;}
}
