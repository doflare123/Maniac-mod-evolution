package org.example.maniacrevolution.character;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.example.maniacrevolution.data.PlayerDataManager;

/** Main-thread selection and match-start guard. Survivor preferences do not reserve maniac slots. */
public final class UniqueManiacSelection {
    private UniqueManiacSelection() {}
    private static boolean participating(ServerPlayer p) {
        return !p.isSpectator() && p.getTeam()!=null && "maniac".equals(p.getTeam().getName());
    }
    public static ServerPlayer occupiedBy(ServerPlayer requester,int classId,Collection<ServerPlayer> players) {
        if(classId<0)return null;
        return players.stream().filter(p->!p.getUUID().equals(requester.getUUID())&&participating(p)
                && PlayerDataManager.get(p).getManiacClassId()==classId).findFirst().orElse(null);
    }
    public static boolean allow(ServerPlayer player,int classId) {
        var occupied=occupiedBy(player,classId,player.server.getPlayerList().getPlayers());
        if(occupied==null)return true;
        player.sendSystemMessage(Component.literal("§cЭтот маньяк уже выбран игроком "+occupied.getGameProfile().getName()+". Выберите другого."));
        return false;
    }
    public static String duplicates(Collection<ServerPlayer> players) {
        var classes=new LinkedHashMap<Integer,List<String>>();
        for(var p:players)if(participating(p)){
            int id=PlayerDataManager.get(p).getManiacClassId();if(id<0)continue;
            classes.computeIfAbsent(id,key->new ArrayList<>()).add(p.getGameProfile().getName());
        }
        var conflicts=new ArrayList<String>();
        classes.forEach((id,names)->{if(names.size()>1){
            String title=CharacterRegistry.getAllClasses().stream().filter(c->c.getType()==CharacterType.MANIAC&&c.getScoreboardId()==id).map(CharacterClass::getName).findFirst().orElse("#"+id);
            conflicts.add(title+": "+String.join(", ",names));
        }});
        return String.join("; ",conflicts);
    }
    public static boolean validateMatch(MinecraftServer server) {
        String conflicts=duplicates(server.getPlayerList().getPlayers());if(conflicts.isEmpty())return true;
        server.getPlayerList().broadcastSystemMessage(Component.literal("§cНачало матча отменено: одинаковые маньяки — "+conflicts+". Измените выбор."),false);return false;
    }
}
