package org.example.maniacrevolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.GameRulesConfig;
import org.example.maniacrevolution.config.HudConfig;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.mana.ManaProvider;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ClosePerkScreenPacket;
import org.example.maniacrevolution.network.packets.OpenGuiPacket;
import org.example.maniacrevolution.network.packets.SyncManaPacket;
import org.example.maniacrevolution.perk.perks.common.BigmoneyPerk;
import org.example.maniacrevolution.perk.perks.common.MegamindPerk;
import org.example.maniacrevolution.perk.perks.maniac.HighlightPerk;
import org.example.maniacrevolution.util.ManaUtil;

import javax.annotation.Nullable;
import java.util.Collection;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maniacrev")
                .requires(src -> src.hasPermission(2))

                // /maniacrev start
                .then(Commands.literal("start")
                        .executes(ctx -> {
                            GameManager.startGame(ctx.getSource());
                            return 1;
                        }))

                // /maniacrev stop
                .then(Commands.literal("stop")
                        .executes(ctx -> {
                            GameManager.stopGame(ctx.getSource());
                            return 1;
                        }))

                .then(Commands.literal("glowing_perks")
                        .executes(ctx -> {
                            return executeGlowingPerks(ctx.getSource());
                        }))

                // /maniacrev timer ...
                .then(Commands.literal("timer")
                        .then(Commands.literal("start")
                                .executes(ctx -> {
                                    GameManager.startTimer();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aТаймер запущен"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("stop")
                                .executes(ctx -> {
                                    GameManager.stopTimer();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§cТаймер остановлен"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                            GameManager.setTime(sec);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§eТаймер установлен на " + sec + " сек"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("add")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                            GameManager.addTime(sec);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§eДобавлено " + sec + " сек к таймеру"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("maxtime")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                                            GameManager.setMaxTime(sec);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§eМакс. время установлено: " + sec + " сек"), true);
                                            return 1;
                                        }))))

                .then(Commands.literal("phase")
                        .then(Commands.argument("phase", IntegerArgumentType.integer(0, 3))
                                .executes(ctx -> {
                                    int phase = IntegerArgumentType.getInteger(ctx, "phase");
                                    GameManager.setPhase(phase);

                                    if (phase == 3) {
                                        MinecraftServer server = ctx.getSource().getServer();
                                        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                            player.playNotifySound(
                                                    SoundEvents.ENDER_DRAGON_GROWL,
                                                    SoundSource.MASTER,
                                                    5.0F,
                                                    1.0F
                                            );
                                        }
                                    }

                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§eФаза установлена: " + phase), true);
                                    return 1;
                                })))

                // /maniacrev addexp <targets> <amount>
                .then(Commands.literal("addexp")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addExp(ctx)))))

                // /maniacrev addmoney <targets> <amount>
                .then(Commands.literal("addmoney")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> addMoney(ctx)))))

                // /maniacrev perks clear [targets]
                .then(Commands.literal("perks")
                        .then(Commands.literal("clear")
                                .executes(ctx -> clearPerks(ctx, null))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> clearPerks(ctx, EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("open")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> openPerkGui(ctx))))
                        .then(Commands.literal("close")
                                .executes(ctx -> closePerkGui(ctx, null))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> closePerkGui(ctx, EntityArgument.getPlayers(ctx, "targets"))))))

                // HUD команды
                .then(Commands.literal("hud")
                        .then(Commands.literal("toggle")
                                .executes(ModCommands::toggleHud))
                        .then(Commands.literal("enable")
                                .executes(ModCommands::enableHud))
                        .then(Commands.literal("disable")
                                .executes(ModCommands::disableHud)))

                // Item drop команды
                .then(Commands.literal("itemdrop")
                        .then(Commands.literal("allow")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ModCommands::setItemDrop)))
                        .then(Commands.literal("toggle")
                                .executes(ModCommands::toggleItemDrop)))

                // Hitbox debug команды
                .then(Commands.literal("hitboxdebug")
                        .then(Commands.literal("allow")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ModCommands::setHitboxDebug))))

                // Mana команды с поддержкой селекторов
                .then(Commands.literal("mana")
                        .then(Commands.literal("regen")
                                .then(Commands.literal("enable")
                                        .executes(ModCommands::enablePassiveRegenSelf) // Для себя
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ModCommands::enablePassiveRegenTargets))) // Для других
                                .then(Commands.literal("disable")
                                        .executes(ModCommands::disablePassiveRegenSelf) // Для себя
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ModCommands::disablePassiveRegenTargets)))) // Для других
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ModCommands::setManaSelf) // Для себя
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ModCommands::setManaTargets)))) // Для других
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ModCommands::addManaSelf) // Для себя
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ModCommands::addManaTargets))))) // Для других

                // /maniacrev guide
                .then(Commands.literal("guide")
                        .executes(ctx -> openGuide(ctx)))

                .then(DownedCommand.build())
        );
    }

    private static int addExp(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int baseAmount = IntegerArgumentType.getInteger(ctx, "amount");

        for (ServerPlayer player : targets) {
            PlayerData data = PlayerDataManager.get(player);

            int actualAmount = baseAmount;
            if (MegamindPerk.hasActivePerk(player)) {
                actualAmount = MegamindPerk.applyBonus(baseAmount);
                player.displayClientMessage(
                        Component.literal("§a+" + actualAmount + " опыта! §7(Megamind: +" + (actualAmount - baseAmount) + ")"),
                        true
                );
            } else {
                player.displayClientMessage(Component.literal("§a+" + actualAmount + " опыта!"), true);
            }

            data.addExperience(actualAmount);
            PlayerDataManager.syncToClient(player);
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("§aДобавлено " + baseAmount + " опыта " + targets.size() + " игрокам"), true);
        return targets.size();
    }

    private static int addMoney(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int baseAmount = IntegerArgumentType.getInteger(ctx, "amount");

        for (ServerPlayer player : targets) {
            PlayerData data = PlayerDataManager.get(player);

            int actualAmount = baseAmount;
            if (BigmoneyPerk.hasActivePerk(player)) {
                actualAmount = BigmoneyPerk.applyBonus(baseAmount);
                if (baseAmount > 0) {
                    player.displayClientMessage(
                            Component.literal("§6+" + actualAmount + " монет! §7(Bigmoney: +" + (actualAmount - baseAmount) + ")"),
                            true
                    );
                } else {
                    player.displayClientMessage(Component.literal("§6" + actualAmount + " монет!"), true);
                }
            } else {
                if (baseAmount > 0) {
                    player.displayClientMessage(Component.literal("§6+" + actualAmount + " монет!"), true);
                } else {
                    player.displayClientMessage(Component.literal("§6" + actualAmount + " монет!"), true);
                }
            }

            data.addCoins(actualAmount);
            PlayerDataManager.syncToClient(player);
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("§aДобавлено " + baseAmount + " монет " + targets.size() + " игрокам"), true);
        return targets.size();
    }

    private static int executeGlowingPerks(CommandSourceStack source) {
        try {
            int highlightedCount = HighlightPerk.activateGlowing(source.getServer());

            if (highlightedCount > 0) {
                source.sendSuccess(
                        () -> Component.literal("Подсвечено выживших: " + highlightedCount),
                        true
                );
            } else {
                source.sendSuccess(
                        () -> Component.literal("Нет активных перков подсветки или нет доступных выживших"),
                        false
                );
            }

            return highlightedCount;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Ошибка при активации перка: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static int clearPerks(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (targets == null) {
            targets = ctx.getSource().getServer().getPlayerList().getPlayers();
        }

        for (ServerPlayer player : targets) {
            PlayerData data = PlayerDataManager.get(player);
            data.clearPerks(player);
            PlayerDataManager.syncToClient(player);
        }

        int count = targets.size();
        ctx.getSource().sendSuccess(() ->
                Component.literal("§cПерки сняты у " + count + " игроков"), true);
        return count;
    }

    private static int openPerkGui(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

        for (ServerPlayer target : targets) {
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> target),
                    new OpenGuiPacket(OpenGuiPacket.GuiType.PERK_SELECTION)
            );
        }

        int count = targets.size();
        ctx.getSource().sendSuccess(() ->
                Component.literal("§aОткрыто меню выбора перков для " + count + " игроков"), true);
        return count;
    }

    private static int openGuide(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenGuiPacket(OpenGuiPacket.GuiType.GUIDE)
            );
        }
        return 1;
    }

    private static int closePerkGui(CommandContext<CommandSourceStack> ctx, @Nullable Collection<ServerPlayer> targets) {
        if (targets == null) {
            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                ModNetworking.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new ClosePerkScreenPacket()
                );
                ctx.getSource().sendSuccess(() -> Component.literal("§aЭкран выбора перков закрыт"), false);
                return 1;
            }
            return 0;
        }

        for (ServerPlayer player : targets) {
            ModNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ClosePerkScreenPacket()
            );
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("§aЭкран выбора перков закрыт у " + targets.size() + " игроков"), false);
        return targets.size();
    }

    private static int toggleHud(CommandContext<CommandSourceStack> context) {
        HudConfig.toggleCustomHud();
        boolean enabled = HudConfig.isCustomHudEnabled();

        context.getSource().sendSuccess(
                () -> Component.literal(enabled ?
                        "§aCustom HUD enabled" :
                        "§cCustom HUD disabled (vanilla HUD active)"),
                true
        );

        return enabled ? 1 : 0;
    }

    private static int enableHud(CommandContext<CommandSourceStack> context) {
        HudConfig.setCustomHudEnabled(true);

        context.getSource().sendSuccess(
                () -> Component.literal("§aCustom HUD enabled"),
                true
        );

        return 1;
    }

    private static int disableHud(CommandContext<CommandSourceStack> context) {
        HudConfig.setCustomHudEnabled(false);

        context.getSource().sendSuccess(
                () -> Component.literal("§cCustom HUD disabled (vanilla HUD active)"),
                true
        );

        return 1;
    }

    private static int setItemDrop(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        GameRulesConfig.setItemDropAllowed(enabled);

        context.getSource().sendSuccess(
                () -> Component.literal(enabled ?
                        "§aItem dropping enabled" :
                        "§cItem dropping disabled"),
                true
        );

        return enabled ? 1 : 0;
    }

    private static int toggleItemDrop(CommandContext<CommandSourceStack> context) {
        boolean current = GameRulesConfig.isItemDropAllowed();
        GameRulesConfig.setItemDropAllowed(!current);

        context.getSource().sendSuccess(
                () -> Component.literal(!current ?
                        "§aItem dropping enabled" :
                        "§cItem dropping disabled"),
                true
        );

        return !current ? 1 : 0;
    }

    private static int setHitboxDebug(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        GameRulesConfig.setHitboxDebugAllowed(enabled);

        context.getSource().sendSuccess(
                () -> Component.literal(enabled ?
                        "§aHitbox debug allowed" :
                        "§cHitbox debug blocked"),
                true
        );

        return enabled ? 1 : 0;
    }

    // ============================================
// PASSIVE REGEN ENABLE
// ============================================

    private static int enablePassiveRegenSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ManaUtil.setPassiveRegenEnabled(player, true);

        context.getSource().sendSuccess(
                () -> Component.literal("§aPassive mana regeneration enabled"),
                true
        );
        return 1;
    }

    private static int enablePassiveRegenTargets(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer player : targets) {
            ManaUtil.setPassiveRegenEnabled(player, true);
        }

        int count = targets.size();
        context.getSource().sendSuccess(
                () -> Component.literal("§aPassive mana regeneration enabled for " + count + " player(s)"),
                true
        );
        return count;
    }

// ============================================
// PASSIVE REGEN DISABLE
// ============================================

    private static int disablePassiveRegenSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ManaUtil.setPassiveRegenEnabled(player, false);

        context.getSource().sendSuccess(
                () -> Component.literal("§cPassive mana regeneration disabled"),
                true
        );
        return 1;
    }

    private static int disablePassiveRegenTargets(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer player : targets) {
            player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                // НОВОЕ: Логирование
                Maniacrev.LOGGER.info("Disabling passive regen for {}: base={}, bonus={}, passive={}",
                        player.getName().getString(),
                        mana.getBaseRegenRate(),
                        mana.getBonusRegenRate(),
                        mana.isPassiveRegenEnabled());

                mana.setPassiveRegenEnabled(false);

                // НОВОЕ: Принудительно сбрасываем базовый реген
                mana.setBaseRegenRate(0.0f);

                ModNetworking.sendToPlayer(
                        new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                        player
                );
            });
        }

        int count = targets.size();
        context.getSource().sendSuccess(
                () -> Component.literal("§cPassive mana regeneration disabled for " + count + " player(s)"),
                true
        );
        return count;
    }

// ============================================
// SET MANA
// ============================================

    private static int setManaSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        float amount = FloatArgumentType.getFloat(context, "amount");

        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            mana.setMana(amount);
            ModNetworking.sendToPlayer(
                    new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                    player
            );
        });

        context.getSource().sendSuccess(
                () -> Component.literal("§aMana set to: §e" + amount),
                true
        );
        return 1;
    }

    private static int setManaTargets(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        float amount = FloatArgumentType.getFloat(context, "amount");

        for (ServerPlayer player : targets) {
            player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                mana.setMana(amount);
                ModNetworking.sendToPlayer(
                        new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                        player
                );
            });
        }

        int count = targets.size();
        context.getSource().sendSuccess(
                () -> Component.literal("§aMana set to §e" + amount + " §afor " + count + " player(s)"),
                true
        );
        return count;
    }

// ============================================
// ADD MANA
// ============================================

    private static int addManaSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        float amount = FloatArgumentType.getFloat(context, "amount");

        player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
            mana.addMana(amount);
            ModNetworking.sendToPlayer(
                    new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                    player
            );
        });

        context.getSource().sendSuccess(
                () -> Component.literal("§aAdded §e" + amount + " §amana"),
                true
        );
        return 1;
    }

    private static int addManaTargets(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        float amount = FloatArgumentType.getFloat(context, "amount");

        for (ServerPlayer player : targets) {
            player.getCapability(ManaProvider.MANA).ifPresent(mana -> {
                mana.addMana(amount);
                ModNetworking.sendToPlayer(
                        new SyncManaPacket(mana.getMana(), mana.getMaxMana(), mana.getTotalRegenRate()),
                        player
                );
            });
        }

        int count = targets.size();
        context.getSource().sendSuccess(
                () -> Component.literal("§aAdded §e" + amount + " §amana for " + count + " player(s)"),
                true
        );
        return count;
    }
}