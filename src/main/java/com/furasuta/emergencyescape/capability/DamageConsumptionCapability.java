package com.furasuta.emergencyescape.capability;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.config.ModConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DamageConsumptionCapability implements INBTSerializable<CompoundTag> {
    public static final ResourceLocation ID = new ResourceLocation(EmergencyEscapeMod.MODID, "damage_consumption");
    public static Capability<DamageConsumptionCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private static final Logger LOGGER = LoggerFactory.getLogger(DamageConsumptionCapability.class);

    private final List<ConsumptionTimer> activeTimers = new ArrayList<>();
    private boolean isActive = false;

    // 足ダメージ加算デバフ
    private float legDamageAccum = 0f;
    // 速度ランク状態（実行時のみ、NBTには保存しない）
    private transient int heldRank = 0;
    private transient int rankHoldTimer = 0;
    private transient int lastMeasuredRank = 0;
    private transient double lastX, lastY, lastZ;
    private transient boolean posInitialized = false;
    private transient int sampleCounter = 0;
    private transient int bonusConsumeCounter = 0;

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(DamageConsumptionCapability.class);
    }

    // タイマーを追加（スタック式）
    public void addConsumption(boolean isLargeDamage, boolean isInstant, int durationTicks, int intervalTicks, int amount) {
        String timerType = (isLargeDamage ? "Large" : "Small") + (isInstant ? "/Instant" : "/Sustained");

        ConsumptionTimer timer = new ConsumptionTimer(isLargeDamage, isInstant, durationTicks, intervalTicks, amount);
        activeTimers.add(timer);

        LOGGER.info("[EmergencyEscape] Timer stacked: [{}] duration={}ticks({}s), interval={}ticks({}s), amount={}, totalTimers={}",
            timerType, durationTicks, durationTicks/20.0, intervalTicks, intervalTicks/20.0, amount, activeTimers.size());
    }

    public List<ConsumptionTimer> getActiveTimers() {
        return activeTimers;
    }

    public void tick(Player player) {
        if (!isActive || player.isDeadOrDying()) {
            return;
        }

        Iterator<ConsumptionTimer> iterator = activeTimers.iterator();
        while (iterator.hasNext()) {
            ConsumptionTimer timer = iterator.next();
            timer.tick();

            if (timer.getRemainingDuration() % 20 == 0 && timer.getRemainingDuration() > 0) {
                String timerType = (timer.isLargeDamage() ? "Large" : "Small") +
                                   (timer.isInstant() ? "/Instant" : "/Sustained");
                LOGGER.debug("[EmergencyEscape] Timer tick: [{}] remaining={}s, interval={}/{} ticks",
                    timerType, timer.getRemainingDuration()/20, timer.getCurrentIntervalTicks(), timer.getIntervalTicks());
            }

            if (timer.shouldConsume()) {
                int currentLevel = player.experienceLevel;
                int levelsToConsume = Math.min(timer.getAmount(), currentLevel);

                if (currentLevel > 0 && levelsToConsume > 0) {
                    int newLevel = currentLevel - levelsToConsume;

                    // 負の値でクライアントに同期
                    player.giveExperienceLevels(-levelsToConsume);

                    String timerType = (timer.isLargeDamage() ? "Large" : "Small") +
                                       (timer.isInstant() ? "/Instant" : "/Sustained");
                    LOGGER.info("[EmergencyEscape] Level consumed: {} -> {} (-{}) [{}] remaining={}ticks({}s) interval={}/{} Player: {}",
                        currentLevel, newLevel, levelsToConsume, timerType,
                        timer.getRemainingDuration(), timer.getRemainingDuration()/20.0,
                        timer.getCurrentIntervalTicks(), timer.getIntervalTicks(),
                        player.getName().getString());
                }
                timer.resetInterval();
            }

            if (timer.isExpired()) {
                String timerType = (timer.isLargeDamage() ? "Large" : "Small") +
                                   (timer.isInstant() ? "/Instant" : "/Sustained");
                LOGGER.info("[EmergencyEscape] Timer expired: [{}] totalTimers={}", timerType, activeTimers.size() - 1);
                iterator.remove();
            }
        }
    }

    public void clearAllTimers() {
        activeTimers.clear();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (!active) {
            clearAllTimers();
            resetLegBonus();
        }
    }

    public static int getPlayerLevel(Player player) {
        return player.experienceLevel;
    }

    // ===== 足ダメージ加算デバフ =====

    /** 足への被弾ダメージを累積する（加算2の閾値で頭打ち）。 */
    public void addLegDamage(float amount) {
        if (amount <= 0) return;
        int cap = ModConfig.LEG_BONUS2_THRESHOLD.get();
        this.legDamageAccum = Math.min(this.legDamageAccum + amount, cap);
    }

    public float getLegDamageAccum() {
        return legDamageAccum;
    }

    public void setLegDamageAccum(float value) {
        this.legDamageAccum = value;
    }

    public void resetLegBonus() {
        this.legDamageAccum = 0f;
        this.heldRank = 0;
        this.rankHoldTimer = 0;
        this.lastMeasuredRank = 0;
        this.posInitialized = false;
        this.sampleCounter = 0;
        this.bonusConsumeCounter = 0;
    }

    /** 現在の加算レベル（0=なし / 1 / 2）。configの閾値で判定。 */
    public int getBonusLevel() {
        if (!ModConfig.LEG_BONUS_ENABLED.get()) return 0;
        if (legDamageAccum >= ModConfig.LEG_BONUS2_THRESHOLD.get()) return 2;
        if (legDamageAccum >= ModConfig.LEG_BONUS1_THRESHOLD.get()) return 1;
        return 0;
    }

    /** 現在維持中の速度ランク（1〜5、0=なし）。 */
    public int getHeldRank() {
        return heldRank;
    }

    private static int rankForSpeed(double speed) {
        if (speed < ModConfig.LEG_SPEED_T1.get()) return 1;
        if (speed < ModConfig.LEG_SPEED_T2.get()) return 2;
        if (speed < ModConfig.LEG_SPEED_T3.get()) return 3;
        if (speed < ModConfig.LEG_SPEED_T4.get()) return 4;
        return 5;
    }

    private static int consumptionFor(int bonusLevel, int rank) {
        if (bonusLevel == 1) {
            switch (rank) {
                case 1: return ModConfig.LEG_R1_AMOUNT1.get();
                case 2: return ModConfig.LEG_R2_AMOUNT1.get();
                case 3: return ModConfig.LEG_R3_AMOUNT1.get();
                case 4: return ModConfig.LEG_R4_AMOUNT1.get();
                case 5: return ModConfig.LEG_R5_AMOUNT1.get();
            }
        } else if (bonusLevel == 2) {
            switch (rank) {
                case 1: return ModConfig.LEG_R1_AMOUNT2.get();
                case 2: return ModConfig.LEG_R2_AMOUNT2.get();
                case 3: return ModConfig.LEG_R3_AMOUNT2.get();
                case 4: return ModConfig.LEG_R4_AMOUNT2.get();
                case 5: return ModConfig.LEG_R5_AMOUNT2.get();
            }
        }
        return 0;
    }

    /**
     * 足デバフの毎tick処理（サーバー側）。
     * 速度サンプリング → 速度ランクの維持(ガタつき防止) → 通常消費中のみ加算消費。
     */
    public void tickLegBonus(Player player) {
        if (!ModConfig.LEG_BONUS_ENABLED.get() || player.isDeadOrDying()) return;

        int bonusLevel = getBonusLevel();
        if (bonusLevel <= 0) {
            heldRank = 0;
            rankHoldTimer = 0;
            bonusConsumeCounter = 0;
            return;
        }

        // --- 速度サンプリング（座標差分から実測。サーバーではdeltaMovementが不正確なため）---
        if (!posInitialized) {
            lastX = player.getX();
            lastY = player.getY();
            lastZ = player.getZ();
            posInitialized = true;
        }
        sampleCounter++;
        int sampleInterval = ModConfig.LEG_SPEED_SAMPLE_INTERVAL.get();
        if (sampleCounter >= sampleInterval) {
            double dx = player.getX() - lastX;
            double dy = player.getY() - lastY;
            double dz = player.getZ() - lastZ;
            lastX = player.getX();
            lastY = player.getY();
            lastZ = player.getZ();

            double horizontal = Math.sqrt(dx * dx + dz * dz) / sampleCounter;
            double effective = horizontal;
            // 大きな落下のみ鉛直速度を加味（階段・段差の一瞬の落下は無視）
            if (dy < 0 && player.fallDistance > ModConfig.LEG_LARGE_FALL_DISTANCE.get().floatValue()) {
                double fallSpeed = Math.abs(dy) / sampleCounter;
                effective = Math.max(effective, fallSpeed);
            }
            sampleCounter = 0;

            int measured = rankForSpeed(effective);
            lastMeasuredRank = measured;
            // より高いランクは即時反映＋タイマーリセット
            if (measured > heldRank) {
                heldRank = measured;
                rankHoldTimer = ModConfig.LEG_RANK_HOLD_TICKS.get();
            }
        }

        // --- ランク維持タイマー（終了時に最新速度で再判定）---
        if (rankHoldTimer > 0) {
            rankHoldTimer--;
            if (rankHoldTimer <= 0) {
                heldRank = lastMeasuredRank;
                if (heldRank > 0) {
                    rankHoldTimer = ModConfig.LEG_RANK_HOLD_TICKS.get();
                }
            }
        }

        // --- 加算消費（通常消費が動いている時のみ発生）---
        if (activeTimers.isEmpty()) {
            bonusConsumeCounter = 0;
            return;
        }
        bonusConsumeCounter++;
        if (bonusConsumeCounter >= ModConfig.LEG_BONUS_CONSUME_INTERVAL.get()) {
            bonusConsumeCounter = 0;
            int amount = consumptionFor(bonusLevel, heldRank);
            if (amount > 0) {
                int currentLevel = player.experienceLevel;
                int toConsume = Math.min(amount, currentLevel);
                if (toConsume > 0) {
                    player.giveExperienceLevels(-toConsume);
                    LOGGER.info("[EmergencyEscape] Leg-bonus consumed: -{} (加算{} rank{}) {} -> {} Player: {}",
                            toConsume, bonusLevel, heldRank, currentLevel, currentLevel - toConsume,
                            player.getName().getString());
                }
            }
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isActive", isActive);
        tag.putFloat("legDamageAccum", legDamageAccum);

        ListTag timersList = new ListTag();
        for (ConsumptionTimer timer : activeTimers) {
            timersList.add(timer.serializeNBT());
        }
        tag.put("timers", timersList);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.isActive = tag.getBoolean("isActive");
        this.legDamageAccum = tag.getFloat("legDamageAccum");
        this.activeTimers.clear();

        ListTag timersList = tag.getList("timers", Tag.TAG_COMPOUND);
        for (int i = 0; i < timersList.size(); i++) {
            ConsumptionTimer timer = new ConsumptionTimer();
            timer.deserializeNBT(timersList.getCompound(i));
            activeTimers.add(timer);
        }
    }

    public static class ConsumptionTimer implements INBTSerializable<CompoundTag> {
        private boolean isLargeDamage;
        private boolean isInstant;
        private int remainingDuration;
        private int intervalTicks;
        private int currentIntervalTicks;
        private int amount;

        public ConsumptionTimer() {}

        public ConsumptionTimer(boolean isLargeDamage, boolean isInstant, int durationTicks, int intervalTicks, int amount) {
            this.isLargeDamage = isLargeDamage;
            this.isInstant = isInstant;
            this.remainingDuration = durationTicks;
            this.intervalTicks = intervalTicks;
            this.currentIntervalTicks = 0;
            this.amount = amount;
        }

        public void tick() {
            remainingDuration--;
            currentIntervalTicks++;
        }

        public boolean shouldConsume() {
            return currentIntervalTicks >= intervalTicks;
        }

        public void resetInterval() {
            currentIntervalTicks = 0;
        }

        public void refresh(int newDurationTicks) {
            this.remainingDuration = newDurationTicks;
            // currentIntervalTicksは継続させる
        }

        public boolean isExpired() {
            return remainingDuration <= 0;
        }

        public int getAmount() {
            return amount;
        }

        public boolean isLargeDamage() {
            return isLargeDamage;
        }

        public boolean isInstant() {
            return isInstant;
        }

        public int getRemainingDuration() {
            return remainingDuration;
        }

        public int getIntervalTicks() {
            return intervalTicks;
        }

        public int getCurrentIntervalTicks() {
            return currentIntervalTicks;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("isLargeDamage", isLargeDamage);
            tag.putBoolean("isInstant", isInstant);
            tag.putInt("remainingDuration", remainingDuration);
            tag.putInt("intervalTicks", intervalTicks);
            tag.putInt("currentIntervalTicks", currentIntervalTicks);
            tag.putInt("amount", amount);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            this.isLargeDamage = tag.getBoolean("isLargeDamage");
            this.isInstant = tag.getBoolean("isInstant");
            this.remainingDuration = tag.getInt("remainingDuration");
            this.intervalTicks = tag.getInt("intervalTicks");
            this.currentIntervalTicks = tag.getInt("currentIntervalTicks");
            this.amount = tag.getInt("amount");
        }
    }

    public static class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final DamageConsumptionCapability capability = new DamageConsumptionCapability();
        private final LazyOptional<DamageConsumptionCapability> optional = LazyOptional.of(() -> capability);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == CAPABILITY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return capability.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            capability.deserializeNBT(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
