package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class TrapperSurvivorPerk extends Perk {
    public TrapperSurvivorPerk() {
        super(new Builder("trapper_survivor")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME)
                .cooldown(90));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Устанавливает паутину под ногами
        BlockPos pos = player.blockPosition();
        var level = player.serverLevel();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos trapPos = pos.offset(x, 0, z);
                if (level.getBlockState(trapPos).isAir()) {
                    level.setBlock(trapPos, Blocks.COBWEB.defaultBlockState(), 3);
                }
            }
        }

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§7Ловушка установлена!"), true);
    }
}