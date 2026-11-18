package com.sanctionmode;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Collections;
import java.util.List;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
    }

    public void handleMode(boolean active) {
    }

    public void handleOpen(byte kind) {
    }

    public void handleGrants(List<ItemStack> stacks, List<Long> until) {
    }

    public void handleGrant(ItemStack stack, long until) {
    }

    public void handleRequests(List<ItemStack> stacks, List<String> names) {
    }

    public static List<ItemStack> emptyStacks() {
        return Collections.emptyList();
    }
}
