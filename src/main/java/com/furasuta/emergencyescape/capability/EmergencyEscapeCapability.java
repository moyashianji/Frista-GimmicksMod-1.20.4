package com.furasuta.emergencyescape.capability;

import com.furasuta.emergencyescape.EmergencyEscapeMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EmergencyEscapeCapability implements INBTSerializable<CompoundTag> {
    public static final ResourceLocation ID = new ResourceLocation(EmergencyEscapeMod.MODID, "emergency_escape");
    public static Capability<EmergencyEscapeCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private boolean isEscaping = false;
    private int escapeTicksRemaining = 0;
    private double escapeX, escapeY, escapeZ;
    private boolean hasItem = false;

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(EmergencyEscapeCapability.class);
    }

    public boolean isEscaping() {
        return isEscaping;
    }

    public void startEscape(Player player, int durationTicks) {
        this.isEscaping = true;
        this.escapeTicksRemaining = durationTicks;
        this.escapeX = player.getX();
        this.escapeY = player.getY();
        this.escapeZ = player.getZ();
    }

    public void stopEscape() {
        this.isEscaping = false;
        this.escapeTicksRemaining = 0;
    }

    public int getEscapeTicksRemaining() {
        return escapeTicksRemaining;
    }

    public void tick() {
        if (isEscaping && escapeTicksRemaining > 0) {
            escapeTicksRemaining--;
        }
    }

    public boolean shouldDie() {
        return isEscaping && escapeTicksRemaining <= 0;
    }

    public double getEscapeX() {
        return escapeX;
    }

    public double getEscapeY() {
        return escapeY;
    }

    public double getEscapeZ() {
        return escapeZ;
    }

    public void updateEscapeY(double y) {
        this.escapeY = y;
    }

    public boolean hasItem() {
        return hasItem;
    }

    public void setHasItem(boolean hasItem) {
        this.hasItem = hasItem;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isEscaping", isEscaping);
        tag.putInt("escapeTicksRemaining", escapeTicksRemaining);
        tag.putDouble("escapeX", escapeX);
        tag.putDouble("escapeY", escapeY);
        tag.putDouble("escapeZ", escapeZ);
        tag.putBoolean("hasItem", hasItem);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.isEscaping = tag.getBoolean("isEscaping");
        this.escapeTicksRemaining = tag.getInt("escapeTicksRemaining");
        this.escapeX = tag.getDouble("escapeX");
        this.escapeY = tag.getDouble("escapeY");
        this.escapeZ = tag.getDouble("escapeZ");
        this.hasItem = tag.getBoolean("hasItem");
    }

    public static class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final EmergencyEscapeCapability capability = new EmergencyEscapeCapability();
        private final LazyOptional<EmergencyEscapeCapability> optional = LazyOptional.of(() -> capability);

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
