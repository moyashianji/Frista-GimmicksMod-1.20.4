package com.furasuta.emergencyescape.capability;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DamageConsumptionCapability implements INBTSerializable<CompoundTag> {
    public static final ResourceLocation ID = new ResourceLocation(EmergencyEscapeMod.MODID, "damage_consumption");
    public static Capability<DamageConsumptionCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private final List<ConsumptionTimer> activeTimers = new ArrayList<>();
    private boolean isActive = false;

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(DamageConsumptionCapability.class);
    }

    public void addConsumption(boolean isLargeDamage, boolean isInstant, int durationTicks, int intervalTicks, int amount) {
        activeTimers.add(new ConsumptionTimer(isLargeDamage, isInstant, durationTicks, intervalTicks, amount));
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

            if (timer.shouldConsume()) {
                int currentExp = getPlayerTotalExperience(player);
                if (currentExp > 0) {
                    int newExp = Math.max(0, currentExp - timer.getAmount());
                    setPlayerTotalExperience(player, newExp);
                }
                timer.resetInterval();
            }

            if (timer.isExpired()) {
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

    public static int getPlayerTotalExperience(Player player) {
        int level = player.experienceLevel;
        int exp = (int) (player.experienceProgress * player.getXpNeededForNextLevel());

        int totalExp = 0;
        for (int i = 0; i < level; i++) {
            totalExp += getExperienceForLevel(i);
        }
        return totalExp + exp;
    }

    public static void setPlayerTotalExperience(Player player, int exp) {
        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.totalExperience = 0;

        if (exp > 0) {
            player.giveExperiencePoints(exp);
        }
    }

    private static int getExperienceForLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else if (level >= 15) {
            return 37 + (level - 15) * 5;
        } else {
            return 7 + level * 2;
        }
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
