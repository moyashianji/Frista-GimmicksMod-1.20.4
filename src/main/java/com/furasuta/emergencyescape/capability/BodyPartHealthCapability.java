package com.furasuta.emergencyescape.capability;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import com.furasuta.emergencyescape.config.ModConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BodyPartHealthCapability implements INBTSerializable<CompoundTag> {
    public static final ResourceLocation ID = new ResourceLocation(EmergencyEscapeMod.MODID, "body_part_health");
    public static Capability<BodyPartHealthCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private float headHealth;
    private float bodyHealth;
    private int maxHeadHealth;
    private int maxBodyHealth;
    private boolean isActive = false;

    public BodyPartHealthCapability() {
        this.maxHeadHealth = ModConfig.HEAD_MAX_HEALTH.get();
        this.maxBodyHealth = ModConfig.BODY_MAX_HEALTH.get();
        this.headHealth = maxHeadHealth;
        this.bodyHealth = maxBodyHealth;
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(BodyPartHealthCapability.class);
    }

    public float getHeadHealth() {
        return headHealth;
    }

    public void setHeadHealth(float health) {
        this.headHealth = Math.max(0, Math.min(health, maxHeadHealth));
    }

    public float getBodyHealth() {
        return bodyHealth;
    }

    public void setBodyHealth(float health) {
        this.bodyHealth = Math.max(0, Math.min(health, maxBodyHealth));
    }

    public int getMaxHeadHealth() {
        return maxHeadHealth;
    }

    public int getMaxBodyHealth() {
        return maxBodyHealth;
    }

    public void damageHead(float amount) {
        this.headHealth = Math.max(0, this.headHealth - amount);
    }

    public void damageBody(float amount) {
        this.bodyHealth = Math.max(0, this.bodyHealth - amount);
    }

    public float getHeadHealthPercent() {
        return maxHeadHealth > 0 ? (headHealth / maxHeadHealth) * 100f : 0;
    }

    public float getBodyHealthPercent() {
        return maxBodyHealth > 0 ? (bodyHealth / maxBodyHealth) * 100f : 0;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (active) {
            // Reset health when activated
            this.maxHeadHealth = ModConfig.HEAD_MAX_HEALTH.get();
            this.maxBodyHealth = ModConfig.BODY_MAX_HEALTH.get();
            this.headHealth = maxHeadHealth;
            this.bodyHealth = maxBodyHealth;
        }
    }

    public void reset() {
        this.maxHeadHealth = ModConfig.HEAD_MAX_HEALTH.get();
        this.maxBodyHealth = ModConfig.BODY_MAX_HEALTH.get();
        this.headHealth = maxHeadHealth;
        this.bodyHealth = maxBodyHealth;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("headHealth", headHealth);
        tag.putFloat("bodyHealth", bodyHealth);
        tag.putInt("maxHeadHealth", maxHeadHealth);
        tag.putInt("maxBodyHealth", maxBodyHealth);
        tag.putBoolean("isActive", isActive);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.headHealth = tag.getFloat("headHealth");
        this.bodyHealth = tag.getFloat("bodyHealth");
        this.maxHeadHealth = tag.getInt("maxHeadHealth");
        this.maxBodyHealth = tag.getInt("maxBodyHealth");
        this.isActive = tag.getBoolean("isActive");
    }

    public static class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final BodyPartHealthCapability capability = new BodyPartHealthCapability();
        private final LazyOptional<BodyPartHealthCapability> optional = LazyOptional.of(() -> capability);

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
