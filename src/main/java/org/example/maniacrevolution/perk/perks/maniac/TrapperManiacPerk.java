package org.example.maniacrevolution.perk.perks.maniac;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;

public class TrapperManiacPerk extends Perk {
    public TrapperManiacPerk() {
        super(new Builder("trapper_maniac")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.MANIAC)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME)
                .cooldown(60));
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Капкан - невидимая нажимная плита + редстоун
        BlockPos pos = player.blockPosition();
        var level = player.serverLevel();

        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 3);
            player.addTag("maniacrev_trap_" + pos.asLong());
        }

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§cКапкан установлен!"), true);
    }
}
