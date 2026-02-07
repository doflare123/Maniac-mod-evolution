package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.example.maniacrevolution.block.entity.FNAFGeneratorBlockEntity;

public class GeneratorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("fnaf_generator")
                        .then(Commands.literal("default")
                                .executes(GeneratorCommand::resetGenerator))
                        .then(Commands.literal("change")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(GeneratorCommand::changeGenerator)))
                        .then(Commands.literal("info")
                                .executes(GeneratorCommand::getGeneratorInfo))
                        .then(Commands.literal("toggle")
                                .executes(GeneratorCommand::toggleGenerator))
                        .then(Commands.literal("set_powered")
                                .then(Commands.literal("true")
                                        .executes(ctx -> setPowered(ctx, true)))
                                .then(Commands.literal("false")
                                        .executes(ctx -> setPowered(ctx, false))))
                )
        );
    }

    private static int resetGenerator(CommandContext<CommandSourceStack> context) {
        FNAFGeneratorBlockEntity generator = findGenerator(context);
        if (generator == null) {
            context.getSource().sendFailure(Component.literal("§cГенератор не найден в мире!"));
            return 0;
        }

        generator.setCharge(generator.getMaxCharge());
        generator.setPowered(true);
        context.getSource().sendSuccess(() -> Component.literal("§aГенератор сброшен до начального состояния (100% заряд, включен)"), true);
        return 1;
    }

    private static int changeGenerator(CommandContext<CommandSourceStack> context) {
        FNAFGeneratorBlockEntity generator = findGenerator(context);
        if (generator == null) {
            context.getSource().sendFailure(Component.literal("§cГенератор не найден в мире!"));
            return 0;
        }

        int percentage = IntegerArgumentType.getInteger(context, "value");
        int newCharge = (int) (generator.getMaxCharge() * (percentage / 100.0f));
        generator.setCharge(newCharge);

        context.getSource().sendSuccess(() -> Component.literal(
                String.format("§aЗаряд генератора установлен на %d%% (%d/%d тиков)",
                        percentage, newCharge, generator.getMaxCharge())), true);
        return 1;
    }

    private static int getGeneratorInfo(CommandContext<CommandSourceStack> context) {
        FNAFGeneratorBlockEntity generator = findGenerator(context);
        if (generator == null) {
            context.getSource().sendFailure(Component.literal("§cГенератор не найден в мире!"));
            return 0;
        }

        float percentage = generator.getChargePercentage() * 100;
        boolean powered = generator.isPowered();
        String status = powered ? "§aВКЛЮЧЕН" : "§cВЫКЛЮЧЕН";

        context.getSource().sendSuccess(() -> Component.literal(
                String.format("§6=== Информация о генераторе ===\n" +
                                "§eЗаряд: §f%.1f%% (%d/%d)\n" +
                                "§eСтатус: %s\n" +
                                "§eПозиция: §f%s",
                        percentage, generator.getCharge(), generator.getMaxCharge(),
                        status, findGeneratorPos(context))), false);
        return 1;
    }

    private static int toggleGenerator(CommandContext<CommandSourceStack> context) {
        FNAFGeneratorBlockEntity generator = findGenerator(context);
        if (generator == null) {
            context.getSource().sendFailure(Component.literal("§cГенератор не найден в мире!"));
            return 0;
        }

        boolean newState = !generator.isPowered();
        generator.setPowered(newState);
        String status = newState ? "§aвключен" : "§cвыключен";

        context.getSource().sendSuccess(() -> Component.literal(
                String.format("§eГенератор %s", status)), true);
        return 1;
    }

    private static int setPowered(CommandContext<CommandSourceStack> context, boolean powered) {
        FNAFGeneratorBlockEntity generator = findGenerator(context);
        if (generator == null) {
            context.getSource().sendFailure(Component.literal("§cГенератор не найден в мире!"));
            return 0;
        }

        generator.setPowered(powered);
        String status = powered ? "§aвключен" : "§cвыключен";

        context.getSource().sendSuccess(() -> Component.literal(
                String.format("§eГенератор %s", status)), true);
        return 1;
    }

    private static FNAFGeneratorBlockEntity findGenerator(CommandContext<CommandSourceStack> context) {
        // Получаем инстанс генератора из статического поля
        return FNAFGeneratorBlockEntity.getInstance();
    }

    private static BlockPos findGeneratorPos(CommandContext<CommandSourceStack> context) {
        FNAFGeneratorBlockEntity generator = findGenerator(context);
        return generator != null ? generator.getBlockPos() : BlockPos.ZERO;
    }
}