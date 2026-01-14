package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterRegistry;

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

            // Устанавливаем scoreboard в зависимости от типа персонажа
            String scoreboardName = characterClass.getType().getScoreboardName();
            int classId = characterClass.getScoreboardId();

            // Устанавливаем scoreboard
            player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withSuppressedOutput(),
                    String.format("scoreboard players set %s %s %d",
                            player.getName().getString(),
                            scoreboardName,
                            classId)
            );

            Maniacrev.LOGGER.info("Player {} selected character: {} - ID: {}",
                    player.getName().getString(),
                    characterClass.getName(),
                    classId);
        });
        ctx.get().setPacketHandled(true);
    }
}