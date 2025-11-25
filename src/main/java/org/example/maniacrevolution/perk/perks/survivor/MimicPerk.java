package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.example.maniacrevolution.entity.MimicBlockEntity;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MimicPerk extends Perk {

    // Отслеживание активных мимиков
    private static final Map<UUID, MimicData> ACTIVE_MIMICS = new HashMap<>();

    public MimicPerk() {
        super(new Builder("mimic")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.ANY)
                .cooldown(80));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();

        // Находим самый распространённый блок в радиусе 7
        Block mostCommon = findMostCommonBlock(level, pos, 7);
        BlockState blockState = mostCommon.defaultBlockState();

        // Создаём энтити-блок
        MimicBlockEntity mimicEntity = new MimicBlockEntity(level, player, blockState);
        level.addFreshEntity(mimicEntity);

        // Делаем игрока невидимым и неуязвимым
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 7 * 20, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 255, false, false));

        // Сохраняем данные
        ACTIVE_MIMICS.put(player.getUUID(), new MimicData(
                mimicEntity.getUUID(),
                System.currentTimeMillis() + 7000
        ));

        player.displayClientMessage(
                Component.literal("§aВы замаскировались под " + mostCommon.getName().getString() + "!"),
                true
        );
    }

    @Override
    public void onTick(ServerPlayer player) {
        UUID playerId = player.getUUID();
        MimicData data = ACTIVE_MIMICS.get(playerId);

        if (data != null && System.currentTimeMillis() >= data.endTime) {
            endMimicEffect(player);
        }
    }

    @Override
    public void removePassiveEffect(ServerPlayer player) {
        // При снятии перка снимаем и эффект мимика
        if (ACTIVE_MIMICS.containsKey(player.getUUID())) {
            endMimicEffect(player);
        }
    }

    private void endMimicEffect(ServerPlayer player) {
        MimicData data = ACTIVE_MIMICS.remove(player.getUUID());
        if (data == null) return;

        // Снимаем неуязвимость
//        player.setInvulnerable(false);

        // Удаляем энтити блока
        ServerLevel level = player.serverLevel();
        if (level.getEntity(data.entityUUID) instanceof MimicBlockEntity mimic) {
            mimic.discard();
        }

        player.displayClientMessage(
                Component.literal("§7Маскировка снята."),
                true
        );
    }

    private Block findMostCommonBlock(ServerLevel level, BlockPos center, int radius) {
        Map<Block, Integer> counts = new HashMap<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockState state = level.getBlockState(center.offset(x, y, z));
                    Block block = state.getBlock();

                    // Пропускаем воздух, жидкости, блоки с тайл-энтити
                    if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) continue;
                    if (!state.getFluidState().isEmpty()) continue;
                    if (state.hasBlockEntity()) continue;

                    counts.merge(block, 1, Integer::sum);
                }
            }
        }

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Blocks.STONE);
    }

    // Статические методы для внешнего использования
    public static boolean isPlayerMimicking(UUID playerId) {
        return ACTIVE_MIMICS.containsKey(playerId);
    }

    public static void forceEndMimic(ServerPlayer player) {
        MimicData data = ACTIVE_MIMICS.remove(player.getUUID());
        if (data != null) {
            player.setInvulnerable(false);
            if (player.serverLevel().getEntity(data.entityUUID) instanceof MimicBlockEntity mimic) {
                mimic.discard();
            }
        }
    }

    private record MimicData(UUID entityUUID, long endTime) {}
}

