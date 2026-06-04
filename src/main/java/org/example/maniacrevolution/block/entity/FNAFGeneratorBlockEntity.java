package org.example.maniacrevolution.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncGeneratorPacket;
import org.jetbrains.annotations.Nullable;

public class FNAFGeneratorBlockEntity extends BlockEntity {

    // Статическая ссылка на единственный генератор в мире
    private static FNAFGeneratorBlockEntity instance = null;

    // Максимальный заряд (в тиках, 20 тиков = 1 секунда)
    private static final int MAX_CHARGE = 2000; // 1.5 минут = 100 секунд = 2000 тиков
    private static final int DRAIN_RATE = 1; // Скорость разряда/зарядки: 1 тик за тик
    // ВКЛЮЧЕН → разряжается за 5 минут
    // ВЫКЛЮЧЕН → заряжается за 5 минут
    private static final int DARKNESS_THRESHOLD = (int) (MAX_CHARGE * 0.30); // 30% заряда

    private int charge = MAX_CHARGE; // Текущий заряд
    private boolean powered = true; // Включен ли генератор (по умолчанию включен)
    private int particleTimer = 0;

    public FNAFGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FNAF_GENERATOR.get(), pos, state);
    }

    /**
     * Получить инстанс генератора в мире
     */
    public static FNAFGeneratorBlockEntity getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Сохраняем ссылку на этот генератор
        instance = this;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Удаляем ссылку при удалении блока
        if (instance == this) {
            instance = null;
        }
    }

    private int darknessTimer = 0; // Таймер для применения эффекта тьмы

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean stateChanged = false;

        // Если генератор ВКЛЮЧЕН - РАЗРЯЖАЕТСЯ
        if (powered && charge > 0) {
            charge += DRAIN_RATE;
            if (charge > MAX_CHARGE) charge = MAX_CHARGE;
            stateChanged = true;

            // Частицы лавы когда включен
            particleTimer++;
            if (particleTimer >= 20) { // Каждые 10 тиков
                particleTimer = 0;
                spawnLavaParticles(level, pos);
            }
        }
        // Если генератор ВЫКЛЮЧЕН - ЗАРЯЖАЕТСЯ (восстанавливается)
        else if (!powered && charge <= MAX_CHARGE) {
            charge -= DRAIN_RATE; // Заряжается с той же скоростью, что и разряжается
            if (charge < 0) charge = 0;
            stateChanged = true;

            particleTimer = 0; // Сбрасываем таймер частиц
        }

        // Сохраняем изменения и синхронизируем
        if (stateChanged) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, 3);

            // Синхронизируем с клиентами каждую секунду (20 тиков)
            if (charge % 20 == 0) {
                syncToClients(level, pos);
            }
        }

        // Если заряд меньше 30% - накладываем эффект тьмы (каждые 2 секунды)
        darknessTimer++;
        if (charge < DARKNESS_THRESHOLD && darknessTimer >= 40) { // 40 тиков = 2 секунды
            darknessTimer = 0;
            applyDarknessEffect(level);
        } else if (charge >= DARKNESS_THRESHOLD) {
            darknessTimer = 0; // Сбрасываем таймер если заряд восстановился
        }
    }

    /**
     * Синхронизация состояния генератора со всеми клиентами
     */
    private void syncToClients(ServerLevel level, BlockPos pos) {
        ModNetworking.sendToAllPlayers(
                new SyncGeneratorPacket(pos, charge, powered)
        );
    }

    private void spawnLavaParticles(ServerLevel level, BlockPos pos) {
        // Создаем частицы лавы вокруг генератора
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.8;
        double z = pos.getZ() + 0.5;

        for (int i = 0; i < 3; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetY = level.random.nextDouble() * 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

            level.sendParticles(
                    ParticleTypes.LAVA,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0, 0.05, 0, 0
            );
        }
    }

    private void applyDarknessEffect(ServerLevel level) {
        // Находим всех игроков в режиме приключения и команде survivors
        for (Player player : level.players()) {
            // Проверяем, что игрок в режиме приключения
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                    if (player.getTeam() != null && player.getTeam().getName().equals("survivors")) {
                        // Накладываем эффект тьмы на 5 секунд (100 тиков)
                        // Применяется каждые 2 секунды, так что будет постоянный эффект
                        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false, true));
                    }
                }
            }
        }
    }

    // Геттеры и сеттеры
    public int getCharge() {
        return charge;
    }

    public void setCharge(int charge) {
        setCharge(charge, true);
    }

    /**
     * Установить заряд с опциональной синхронизацией
     * @param charge новое значение заряда
     * @param sync отправлять ли пакет синхронизации
     */
    public void setCharge(int charge, boolean sync) {
        this.charge = Math.max(0, Math.min(MAX_CHARGE, charge));
        setChanged();
        if (level != null && !level.isClientSide && sync) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // Немедленная синхронизация при изменении заряда
            ModNetworking.sendToAllPlayers(
                    new SyncGeneratorPacket(worldPosition, this.charge, powered)
            );
        }
    }

    public int getMaxCharge() {
        return MAX_CHARGE;
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        setPowered(powered, true);
    }

    /**
     * Установить состояние питания с опциональной синхронизацией
     * @param powered новое состояние
     * @param sync отправлять ли пакет синхронизации
     */
    public void setPowered(boolean powered, boolean sync) {
        this.powered = powered;
        setChanged();
        if (level != null && !level.isClientSide && sync) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // Немедленная синхронизация при изменении состояния
            ModNetworking.sendToAllPlayers(
                    new SyncGeneratorPacket(worldPosition, charge, powered)
            );
        }
    }

    public float getChargePercentage() {
        return (float) charge / MAX_CHARGE;
    }

    // NBT сохранение/загрузка
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Charge", charge);
        tag.putBoolean("Powered", powered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.charge = tag.getInt("Charge");
        this.powered = tag.getBoolean("Powered");
    }

    // Синхронизация с клиентом
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}