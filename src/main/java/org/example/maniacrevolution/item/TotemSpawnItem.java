package org.example.maniacrevolution.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.entity.TotemEntity;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Предмет-расходник для спавна тотема шамана.
 * ПКМ по любому блоку — тотем появляется на верхней грани.
 *
 * Регистрация в ModItems:
 *   ITEMS.register("totem_spawn_item", () -> new TotemSpawnItem(
 *       new Item.Properties().stacksTo(16)));
 */
public class TotemSpawnItem extends Item {

    public TotemSpawnItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        // На клиенте — просто сигнализируем что действие принято.
        // Возвращаем SUCCESS чтобы сервер тоже вызвал useOn.
        // НЕ делаем ничего на клиенте — вся логика на сервере.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // ── Серверная логика ──────────────────────────────────────────────────
        if (!(level instanceof ServerLevel sl)) return InteractionResult.FAIL;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.FAIL;

        // Спавним на верхней грани кликнутого блока
        // relative() со стороной ALWAYS ставит тотем НА блок, не в него
        BlockPos spawnPos = ctx.getClickedPos().relative(ctx.getClickedFace());
        double x = spawnPos.getX() + 0.5;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5;

        TotemEntity totem = ModEntities.SHAMAN_TOTEM.get().create(sl);
        if (totem == null) {
            sp.sendSystemMessage(Component.literal("§cОшибка: не удалось создать тотем (entity null)"));
            return InteractionResult.FAIL;
        }

        totem.setOwner(player);
        totem.setPos(x, y, z);
        totem.setYRot(player.getYRot());
        totem.setYHeadRot(player.getYRot());

        boolean added = sl.addFreshEntity(totem);
        if (!added) {
            sp.sendSystemMessage(Component.literal("§cОшибка: тотем не удалось заспавнить"));
            return InteractionResult.FAIL;
        }

        sp.displayClientMessage(Component.literal("§6✦ Тотем заспавнен"), true);

        if (!player.isCreative()) {
            ctx.getItemInHand().shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§6Тотем шамана").withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal("  §7ПКМ по блоку — установить тотем")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Подсвечивает маньяков в радиусе §f"
                        + (int) TotemEntity.GLOW_RADIUS + " §7блоков")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Умирает от §f" + TotemEntity.MAX_HITS + " §7ударов маньяка")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  §8Только маньяки могут его уничтожить")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}