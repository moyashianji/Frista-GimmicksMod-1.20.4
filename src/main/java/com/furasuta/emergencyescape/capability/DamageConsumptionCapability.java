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

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(DamageConsumptionCapability.class);
    }

    /**
     * Add a new consumption timer. All timers stack (no refresh).
     * Multiple attacks = more level consumption.
     */
    public void addConsumption(boolean isLargeDamage, boolean isInstant, int durationTicks, int intervalTicks, int amount) {
        String timerType = (isLargeDamage ? "Large" : "Small") + (isInstant ? "/Instant" : "/Sustained");

        // Always add a new timer - stacking allows more consumption with more attacks
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

            // Debug: Log timer state every 20 ticks (1 second)
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

                    // Use giveExperienceLevels with negative value to properly sync to client
                    player.giveExperienceLevels(-levelsToConsume);

                    // Log level change
                    String timerType = (timer.isLargeDamage() ? "Large" : "Small") +
                                       (timer.isInstant() ? "/Instant" : "/Sustained");
                    LOGGER.info("[EmergencyEscape] Level consumed: {} -> {} (-{}) [{}] remaining={}ticks({}s) interval={}/{} Player: {}",
                        currentLevel, newLevel, levelsToConsume, timerType,
                        timer.getRemainingDuration(), timer.getRemainingDuration()/20.0,
                        timer.getCurrentIntervalTicks(), timer.getIntervalTicks(),
                        player.getName().getString());
                }
                // Always reset interval after consumption check
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
        }
    }

    /**
     * Get player's current experience level.
     * Used for emergency escape trigger check (level 0 = escape).
     */
    public static int getPlayerLevel(Player player) {
        return player.experienceLevel;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isActive", isActive);

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

        /**
         * Refresh this timer by resetting its duration.
         * This is called when the same type of damage is received again.
         */
        public void refresh(int newDurationTicks) {
            this.remainingDuration = newDurationTicks;
            // Don't reset currentIntervalTicks - let the current interval continue
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
