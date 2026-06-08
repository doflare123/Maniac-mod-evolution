package org.example.maniacrevolution.dodepovich;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenCoinFlipAnimationPacket;
import org.example.maniacrevolution.network.packets.OpenSlotMachinePacket;
import org.example.maniacrevolution.sound.ModSounds;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DodepovichCasinoManager {
    public static final int CLASS_ID = 13;
    public static final int ELUSIVENESS_GOOD_SECONDS = 10;
    public static final int ELUSIVENESS_BAD_SLOW_SECONDS = 5;
    public static final int INSIGHT_GOOD_SECONDS = 20;
    public static final int INSIGHT_BAD_SECONDS = 15;
    public static final int SHACKLES_SECONDS = 7;
    public static final float HEALTH_AMOUNT = 4.0f;
    public static final int EAGLE_BLINDNESS_SECONDS = 10;
    public static final int DEBT_EFFECT_SECONDS = 60;
    public static final float FATE_ALLY_DAMAGE = 5.0f;
    public static final int SLOT_DIAMOND_SECONDS = 20;
    public static final float SLOT_EMERALD_HEAL = 2.0f;
    public static final float SLOT_COAL_DAMAGE = 1.0f;
    public static final int SLOT_COAL_SLOW_SECONDS = 7;
    public static final int SLOT_POISON_SECONDS = 7;
    public static final float SLOT_ROT_DAMAGE = 3.0f;
    public static final int INSURANCE_SECONDS = 60;
    public static final int CREDIT_TOTAL_SECONDS = 60;
    public static final int CREDIT_HEAL_SECONDS = 30;
    public static final int CREDIT_DAMAGE_SECONDS = 30;
    public static final float CREDIT_DAMAGE_MULTIPLIER = 1.15f;
    public static final int JACKPOT_SECONDS = 75;
    public static final double JACKPOT_BASE_CHANCE = 0.005;
    public static final double JACKPOT_MISS_BONUS = 0.0015;
    public static final double JACKPOT_MAX_CHANCE = 0.03;
    public static final double DEATH_BASE_CHANCE = 0.002;
    public static final double DEATH_JACKPOT_MULTIPLIER = 10.0;
    private static final Random RANDOM = new Random();
    private static final Map<UUID, DodepovichCoin> LAST_COIN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> JACKPOT_MISS_STREAK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> DEATH_JACKPOT_STACKS = new ConcurrentHashMap<>();
    private static final List<DelayedAction> DELAYED_ACTIONS = new ArrayList<>();
    private static final EnumSet<DodepovichCoin> RANDOM_REROLL_POOL = EnumSet.complementOf(EnumSet.of(DodepovichCoin.REROLL));

    private DodepovichCasinoManager() {
    }

    public static boolean isDodepovich(ServerPlayer player) {
        if (PlayerDataManager.isSelectedClass(player, CharacterType.SURVIVOR, CLASS_ID)) {
            return true;
        }

        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(CharacterType.SURVIVOR.getScoreboardName());
        if (objective == null) return false;
        if (!scoreboard.hasPlayerScore(player.getScoreboardName(), objective)) return false;
        return scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore() == CLASS_ID;
    }

    public static void clearCoinHistory(ServerPlayer player) {
        LAST_COIN.remove(player.getUUID());
        player.displayClientMessage(Component.literal("§6История монеток Додеповича очищена."), true);
    }

    public static void resetCasinoChances(ServerPlayer player) {
        JACKPOT_MISS_STREAK.remove(player.getUUID());
        DEATH_JACKPOT_STACKS.remove(player.getUUID());
        player.displayClientMessage(Component.literal("§6Шансы джекпота и смерти Додеповича сброшены."), true);
    }

    public static void flipCoin(ServerPlayer player, DodepovichCoin coin) {
        boolean good = RANDOM.nextBoolean();
        ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenCoinFlipAnimationPacket(coin, good));

        player.level().playSound(null, player.blockPosition(),
                ModSounds.COIN_FLIP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        schedule(player, 10, delayedPlayer -> applyCoinEffect(delayedPlayer, coin, good, true));
    }

    public static void playSlotMachine(ServerPlayer player, DodepovichCoin insertedCoin) {
        SlotMachineResult result = rollSlotResult(player);
        ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenSlotMachinePacket(insertedCoin, result));

        player.level().playSound(null, player.blockPosition(),
                ModSounds.SLOT_SPIN.get(), SoundSource.BLOCKS, 0.9f, 1.0f);

        schedule(player, 64, delayedPlayer -> applySlotResult(delayedPlayer, insertedCoin, result));
    }

    public static void tickDelayedActions() {
        if (DELAYED_ACTIONS.isEmpty()) return;
        DELAYED_ACTIONS.removeIf(action -> {
            if (--action.ticksLeft > 0) {
                return false;
            }

            ServerPlayer player = action.player.level().getServer().getPlayerList().getPlayer(action.player.getUUID());
            if (player != null && !player.isRemoved()) {
                action.action.accept(player);
            }
            return true;
        });
    }

    private static void schedule(ServerPlayer player, int delayTicks, java.util.function.Consumer<ServerPlayer> action) {
        DELAYED_ACTIONS.add(new DelayedAction(player, delayTicks, action));
    }

    private static SlotMachineResult rollSlotResult(ServerPlayer player) {
        int misses = JACKPOT_MISS_STREAK.getOrDefault(player.getUUID(), 0);
        double deathChance = DEATH_BASE_CHANCE * Math.pow(DEATH_JACKPOT_MULTIPLIER,
                DEATH_JACKPOT_STACKS.getOrDefault(player.getUUID(), 0));
        if (RANDOM.nextDouble() < deathChance) {
            JACKPOT_MISS_STREAK.put(player.getUUID(), misses + 1);
            return SlotMachineResult.DEATH;
        }

        double jackpotChance = Math.min(JACKPOT_BASE_CHANCE + misses * JACKPOT_MISS_BONUS, JACKPOT_MAX_CHANCE);
        if (RANDOM.nextDouble() < jackpotChance) {
            JACKPOT_MISS_STREAK.put(player.getUUID(), 0);
            DEATH_JACKPOT_STACKS.merge(player.getUUID(), 1, Integer::sum);
            return SlotMachineResult.JACKPOT;
        }

        JACKPOT_MISS_STREAK.put(player.getUUID(), misses + 1);
        double roll = RANDOM.nextDouble();
        if (roll < 0.35) return SlotMachineResult.NONE;
        if (roll < 0.53) return SlotMachineResult.COIN_GOOD;
        if (roll < 0.71) return SlotMachineResult.COIN_BAD;
        if (roll < 0.81) return SlotMachineResult.COAL;
        if (roll < 0.88) return SlotMachineResult.SPIDER_EYE;
        if (roll < 0.94) return SlotMachineResult.ROTTEN_FLESH;
        if (roll < 0.98) return SlotMachineResult.EMERALDS;
        if (roll < 0.992) return SlotMachineResult.INSURANCE;
        if (roll < 0.997) return SlotMachineResult.CREDIT;
        return SlotMachineResult.DIAMONDS;
    }

    private static void applySlotResult(ServerPlayer player, DodepovichCoin coin, SlotMachineResult result) {
        switch (result) {
            case NONE -> player.displayClientMessage(Component.literal("§7Автомат съел монетку. Ничего не выпало."), true);
            case JACKPOT -> applyJackpot(player);
            case COIN_GOOD -> applyCoinEffect(player, coin, true, false);
            case COIN_BAD -> applyCoinEffect(player, coin, false, false);
            case DIAMONDS -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SLOT_DIAMOND_SECONDS * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SLOT_DIAMOND_SECONDS * 20, 0));
                player.displayClientMessage(Component.literal("§bАлмазики! Скорость II и сопротивление на 20 сек."), true);
            }
            case EMERALDS -> {
                player.heal(SLOT_EMERALD_HEAL);
                ServerPlayer ally = findNearestSurvivor(player, false);
                if (ally != null) {
                    ally.heal(SLOT_EMERALD_HEAL);
                    ally.displayClientMessage(Component.literal("§aДодепович поделился изумрудиками: +2 HP."), true);
                }
                player.displayClientMessage(Component.literal("§aИзумрудики! +2 HP тебе и ближайшему союзнику."), true);
            }
            case COAL -> {
                player.hurt(player.damageSources().magic(), SLOT_COAL_DAMAGE);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOT_COAL_SLOW_SECONDS * 20, 0));
                player.displayClientMessage(Component.literal("§8Уголь. -1 HP и замедление."), true);
            }
            case SPIDER_EYE -> {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, SLOT_POISON_SECONDS * 20, 0));
                player.displayClientMessage(Component.literal("§5Неудача. Отравление на 7 сек."), true);
            }
            case ROTTEN_FLESH -> {
                player.hurt(player.damageSources().magic(), SLOT_ROT_DAMAGE);
                player.displayClientMessage(Component.literal("§6Гниение. -3 HP."), true);
            }
            case INSURANCE -> {
                player.addEffect(new MobEffectInstance(ModEffects.DODEPOVICH_INSURANCE.get(), INSURANCE_SECONDS * 20, 0, false, true, true));
                player.displayClientMessage(Component.literal("§aСтраховка активна: следующий плохой эффект монетки будет отменён."), true);
            }
            case CREDIT -> {
                player.addEffect(new MobEffectInstance(ModEffects.DODEPOVICH_CREDIT.get(), 20 * CREDIT_TOTAL_SECONDS, 0, false, true, true));
                player.displayClientMessage(Component.literal("§aКредит: 30 сек восстановления, затем 30 сек выплаты с процентами."), true);
            }
            case DEATH -> {
                player.level().playSound(null, player.blockPosition(),
                        ModSounds.SLOT_DEATH.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                player.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§4§lСМЕРТЬ! §cАвтомат забрал " + player.getName().getString() + "."),
                        false
                );
                player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            }
        }

        if (result != SlotMachineResult.NONE && result != SlotMachineResult.JACKPOT && result != SlotMachineResult.DEATH) {
            player.level().playSound(null, player.blockPosition(),
                    ModSounds.SLOT_WIN.get(), SoundSource.PLAYERS, 0.8f, 1.1f);
        }
    }

    private static void applyCoinEffect(ServerPlayer player, DodepovichCoin coin, boolean good, boolean updateHistory) {
        if (!good && consumeInsurance(player)) {
            return;
        }

        DodepovichCoin effectiveCoin = coin;
        if (coin == DodepovichCoin.REROLL) {
            effectiveCoin = LAST_COIN.get(player.getUUID());
            if (effectiveCoin == null) {
                List<DodepovichCoin> pool = new ArrayList<>(RANDOM_REROLL_POOL);
                effectiveCoin = pool.get(RANDOM.nextInt(pool.size()));
            }
            player.displayClientMessage(Component.literal("§dРеролл: сработала " + effectiveCoin.getDisplayName() + "."), true);
        }

        switch (effectiveCoin) {
            case ELUSIVENESS -> applyElusiveness(player, good);
            case INSIGHT -> applyInsight(player, good);
            case SHACKLES -> applyShackles(player, good);
            case HEALTH -> applyHealth(player, good);
            case EAGLE -> applyEagle(player, good);
            case DEBT -> applyDebt(player, good);
            case FATE -> applyFate(player, good);
            case REROLL -> {
            }
        }

        if (updateHistory && coin != DodepovichCoin.REROLL) {
            LAST_COIN.put(player.getUUID(), coin);
        }
    }

    private static boolean consumeInsurance(ServerPlayer player) {
        if (!player.hasEffect(ModEffects.DODEPOVICH_INSURANCE.get())) {
            return false;
        }
        player.removeEffect(ModEffects.DODEPOVICH_INSURANCE.get());
        player.displayClientMessage(Component.literal("§aСтраховка сработала и отменила плохой эффект монетки."), true);
        return true;
    }

    private static void applyElusiveness(ServerPlayer player, boolean good) {
        if (good) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ELUSIVENESS_GOOD_SECONDS * 20, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ELUSIVENESS_GOOD_SECONDS * 20, 0));
            player.displayClientMessage(Component.literal("§aМонетка Неуловимости: невидимость и скорость."), true);
            return;
        }

        ServerPlayer maniac = findRandomManiac(player);
        if (maniac != null) {
            player.teleportTo((ServerLevel) maniac.level(), maniac.getX(), maniac.getY(), maniac.getZ(), player.getYRot(), player.getXRot());
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ELUSIVENESS_BAD_SLOW_SECONDS * 20, 0));
        player.displayClientMessage(Component.literal("§cМонетка Неуловимости подвела: телепорт к маньяку и замедление."), true);
    }

    private static void applyInsight(ServerPlayer player, boolean good) {
        if (good) {
            ServerPlayer maniac = findRandomManiac(player);
            if (maniac != null) {
                SelectiveGlowingEffect.addGlowing(maniac, player, INSIGHT_GOOD_SECONDS * 20);
                player.displayClientMessage(Component.literal("§aМонетка Прозрения: случайный маньяк подсвечен только тебе."), true);
            } else {
                player.displayClientMessage(Component.literal("§7Монетка Прозрения не нашла маньяка."), true);
            }
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, INSIGHT_BAD_SECONDS * 20, 0));
        player.displayClientMessage(Component.literal("§cМонетка Прозрения подвела: ты подсвечен для всех."), true);
    }

    private static void applyShackles(ServerPlayer player, boolean good) {
        ServerPlayer target = good ? findNearestManiac(player) : player;
        if (target == null) {
            player.displayClientMessage(Component.literal("§7Монетка Оков не нашла цель."), true);
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SHACKLES_SECONDS * 20, 255));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, SHACKLES_SECONDS * 20, 128));
        String msg = good ? "§aМонетка Оков: ближайший маньяк обездвижен." : "§cМонетка Оков подвела: ты обездвижен.";
        player.displayClientMessage(Component.literal(msg), true);
    }

    private static void applyHealth(ServerPlayer player, boolean good) {
        if (good) {
            player.heal(HEALTH_AMOUNT);
            player.displayClientMessage(Component.literal("§aМонетка ЗОЖ: +4 HP."), true);
        } else {
            player.hurt(player.damageSources().magic(), HEALTH_AMOUNT);
            player.displayClientMessage(Component.literal("§cМонетка ЗОЖ подвела: -4 HP."), true);
        }
    }

    private static void applyEagle(ServerPlayer player, boolean good) {
        ServerPlayer target = good ? findNearestManiac(player) : player;
        if (target == null) {
            player.displayClientMessage(Component.literal("§7Монетка Орла не нашла цель."), true);
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EAGLE_BLINDNESS_SECONDS * 20, 0));
        String msg = good ? "§aМонетка Орла: ближайший маньяк ослеплён." : "§cМонетка Орла подвела: ты ослеплён.";
        player.displayClientMessage(Component.literal(msg), true);
    }

    private static void applyDebt(ServerPlayer player, boolean good) {
        if (good) {
            player.addEffect(new MobEffectInstance(ModEffects.DODEPOVICH_DAMAGE_BLOCK.get(), DEBT_EFFECT_SECONDS * 20, 0, false, true, true));
            player.displayClientMessage(Component.literal("§aМонетка Долга: следующий урон будет полностью заблокирован."), true);
        } else {
            player.addEffect(new MobEffectInstance(ModEffects.DODEPOVICH_DOUBLE_DAMAGE.get(), DEBT_EFFECT_SECONDS * 20, 0, false, true, true));
            player.displayClientMessage(Component.literal("§cМонетка Долга подвела: следующий урон будет x2."), true);
        }
    }

    private static void applyFate(ServerPlayer player, boolean good) {
        if (good) {
            ServerPlayer maniac = findRandomManiac(player);
            if (maniac == null) {
                player.displayClientMessage(Component.literal("§7Монеточка Судьбы не нашла активного маньяка."), true);
                return;
            }

            int resurrected = 0;
            for (ServerPlayer target : player.getServer().getPlayerList().getPlayers()) {
                Team team = target.getTeam();
                if (team == null || !team.getName().equalsIgnoreCase("survivors")) continue;
                if (target.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) continue;

                target.setGameMode(GameType.ADVENTURE);
                target.teleportTo((ServerLevel) maniac.level(), maniac.getX(), maniac.getY(), maniac.getZ(),
                        maniac.getYRot(), maniac.getXRot());
                target.setHealth(Math.max(1.0f, target.getMaxHealth() * 0.5f));
                target.displayClientMessage(Component.literal("§6Судьба вернула вас к маньяку."), false);
                resurrected++;
            }

            player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§6Монеточка Судьбы воскресила игроков: §e" + resurrected),
                    false
            );
        } else {
            for (ServerPlayer ally : getActivePlayersByTeam(player, "survivors")) {
                if (ally == player) continue;
                ally.hurt(ally.damageSources().magic(), FATE_ALLY_DAMAGE);
                ally.displayClientMessage(Component.literal("§cСудьба Додеповича ударила по союзникам."), true);
            }
            player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
        }
    }

    private static void applyJackpot(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, JACKPOT_SECONDS * 20, 255, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, JACKPOT_SECONDS * 20, 2, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, JACKPOT_SECONDS * 20, 2, false, true, true));
        player.level().playSound(null, player.blockPosition(),
                ModSounds.SLOT_JACKPOT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§6§lДЖЕКПОТ! §e" + player.getName().getString() + " вошёл в режим удачи Додеповича!"),
                false
        );
    }

    private static ServerPlayer findRandomManiac(ServerPlayer player) {
        List<ServerPlayer> maniacs = getActivePlayersByTeam(player, "maniac");
        return maniacs.isEmpty() ? null : maniacs.get(RANDOM.nextInt(maniacs.size()));
    }

    private static ServerPlayer findNearestManiac(ServerPlayer player) {
        return getActivePlayersByTeam(player, "maniac").stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(player)))
                .orElse(null);
    }

    private static ServerPlayer findNearestSurvivor(ServerPlayer player, boolean includeSelf) {
        return getActivePlayersByTeam(player, "survivors").stream()
                .filter(p -> includeSelf || p != player)
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(player)))
                .orElse(null);
    }

    private static List<ServerPlayer> getActivePlayersByTeam(ServerPlayer player, String teamName) {
        List<ServerPlayer> result = new ArrayList<>();
        if (player.getServer() == null) return result;
        for (ServerPlayer candidate : player.getServer().getPlayerList().getPlayers()) {
            if (candidate.isSpectator()) continue;
            Team team = candidate.getTeam();
            if (team != null && team.getName().equalsIgnoreCase(teamName)) {
                result.add(candidate);
            }
        }
        return result;
    }

    public static float getHalfMaxHealth(ServerPlayer player) {
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        return maxHealth == null ? 10.0f : (float) maxHealth.getValue() * 0.5f;
    }

    private static class DelayedAction {
        private final ServerPlayer player;
        private final java.util.function.Consumer<ServerPlayer> action;
        private int ticksLeft;

        private DelayedAction(ServerPlayer player, int ticksLeft, java.util.function.Consumer<ServerPlayer> action) {
            this.player = player;
            this.ticksLeft = ticksLeft;
            this.action = action;
        }
    }
}
