package org.example.maniacrevolution.client.visual;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.VisualEffectsConfig;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class VisualEffectsCommands {
    private VisualEffectsCommands() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        var root = Commands.literal("mrvisual").executes(context -> {
            context.getSource().sendSuccess(() -> Component.literal(
                    "Визуал: " + VisualEffectsConfig.ENABLED.get()
                            + "; сила: " + VisualEffectsConfig.INTENSITY.get()
                            + "; камера: " + VisualEffectsConfig.CAMERA.get()
                            + "; постобработка: " + VisualEffectsConfig.POST_PROCESSING.get()
                            + "; искажение: " + VisualEffectsConfig.DISTORTION.get()
                            + ". /mrvisual preview survivor|maniac|damage|off"), false);
            return 1;
        });
        root.then(toggle("enabled", VisualEffectsConfig.ENABLED));
        root.then(toggle("camera", VisualEffectsConfig.CAMERA));
        root.then(toggle("post", VisualEffectsConfig.POST_PROCESSING));
        root.then(toggle("distortion", VisualEffectsConfig.DISTORTION));
        root.then(Commands.literal("intensity").then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 1))
                .executes(context -> {
                    double value = DoubleArgumentType.getDouble(context, "value");
                    VisualEffectsConfig.INTENSITY.set(value);
                    VisualEffectsConfig.SPEC.save();
                    context.getSource().sendSuccess(() -> Component.literal("Сила визуальных эффектов: " + value), false);
                    return 1;
                })));
        var preview = Commands.literal("preview");
        for (String mode : new String[]{"survivor", "maniac", "damage", "off"}) {
            preview.then(Commands.literal(mode).executes(context -> {
                if (!VisualEffectsController.preview(mode)) {
                    context.getSource().sendFailure(Component.literal(
                            "Предпросмотр: нужен живой игрок в выживании/приключении, enabled=true и intensity > 0."));
                    return 0;
                }
                context.getSource().sendSuccess(() -> Component.literal(mode.equals("off")
                        ? "Предпросмотр выключен." : "Предпросмотр " + mode + " на 10 секунд. Закройте чат; вид от первого лица."), false);
                return 1;
            }));
        }
        root.then(preview);
        event.getDispatcher().register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, ForgeConfigSpec.BooleanValue setting) {
        return Commands.literal(name).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
            boolean value = BoolArgumentType.getBool(context, "value");
            setting.set(value);
            VisualEffectsConfig.SPEC.save();
            context.getSource().sendSuccess(() -> Component.literal(name + ": " + value), false);
            return 1;
        }));
    }
}
