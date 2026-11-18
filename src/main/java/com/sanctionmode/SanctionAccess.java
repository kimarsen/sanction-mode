package com.sanctionmode;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.GameType;

public final class SanctionAccess {

    public static final String TAG = "SanctionMode";

    private SanctionAccess() {
    }

    public static boolean isActive(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            return false;
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getBoolean(TAG);
    }

    public static void setActive(EntityPlayer player, boolean active) {
        NBTTagCompound root = player.getEntityData();
        NBTTagCompound persisted;
        if (root.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            persisted = root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        } else {
            persisted = new NBTTagCompound();
            root.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        }
        persisted.setBoolean(TAG, active);
    }

    public static void maintain(EntityPlayerMP player) {
        if (player == null || player.isDead || player.interactionManager == null || !isActive(player)) {
            return;
        }
        if (player.interactionManager.getGameType() != GameType.SURVIVAL) {
            boolean flying = player.capabilities.isFlying;
            player.setGameType(GameType.SURVIVAL);
            apply(player, flying);
            return;
        }
        apply(player, false);
    }

    public static void apply(EntityPlayerMP player, boolean keepFlying) {
        boolean changed = false;
        if (!player.capabilities.allowFlying) {
            player.capabilities.allowFlying = true;
            changed = true;
        }
        if (!player.capabilities.disableDamage) {
            player.capabilities.disableDamage = true;
            changed = true;
        }
        if (player.capabilities.isCreativeMode) {
            player.capabilities.isCreativeMode = false;
            changed = true;
        }
        if (!player.capabilities.allowEdit) {
            player.capabilities.allowEdit = true;
            changed = true;
        }
        if (keepFlying && !player.capabilities.isFlying) {
            player.capabilities.isFlying = true;
            changed = true;
        }
        if (changed || player.ticksExisted % 100 == 0) {
            player.sendPlayerAbilities();
        }
    }
}
