package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterRegistry;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.data.PlayerDataManager;

import java.util.function.Supplier;

/**
 * Пакет для выбора класса персонажа (Client -> Server)
 */
public class SelectCharacterPacket {
    private final String characterId;

    public SelectCharacterPacket(String characterId) {
        this.characterId = characterId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(characterId);
    }

    public static SelectCharacterPacket decode(FriendlyByteBuf buf) {
        return new SelectCharacterPacket(buf.readUtf());
    }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            CharacterClass characterClass = CharacterRegistry.getClass(characterId);
            if (characterClass == null) {
                Maniacrev.LOGGER.warn("Player {} tried to select unknown character: {}",
                        player.getName().getString(), characterId);
                return;
            }

            String scoreboardName = characterClass.getType().getScoreboardName();
            int classId = characterClass.getScoreboardId();
            PlayerData data = PlayerDataManager.get(player);
            if (characterClass.getType() == CharacterType.MANIAC) {
                data.setManiacClassId(classId);
            } else {
                data.setSurvivorClassId(classId);
            }

            Scoreboard scoreboard = player.getServer().getScoreboard();
            Objective objective = scoreboard.getObjective(scoreboardName);
            if (objective == null) {
                objective = scoreboard.addObjective(
                        scoreboardName,
                        ObjectiveCriteria.DUMMY,
                        net.minecraft.network.chat.Component.literal(scoreboardName),
                        ObjectiveCriteria.RenderType.INTEGER
                );
            }
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(classId);

            // Клиентские данные — для мода
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncPlayerClassPacket(characterClass.getType(), classId)
            );
            PlayerDataManager.syncPlayerClassToAll(player);
            // Серверные и клиентские данные — для логики мода без зависимости от scoreboard
            PlayerDataManager.setSelectedClass(player, characterClass.getType(), classId);

            Maniacrev.LOGGER.info("Player {} selected character: {} (type={}, id={})",
                    player.getName().getString(),
                    characterClass.getName(),
                    characterClass.getType(),
                    classId);
        });
        ctx.get().setPacketHandled(true);
    }
}
