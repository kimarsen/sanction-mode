package com.sanctionmode.client;

import com.sanctionmode.CommonProxy;
import com.sanctionmode.client.gui.GuiCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        ClientRegistry.registerKeyBinding(SanctionBinds.CATALOG);
        ClientRegistry.registerKeyBinding(SanctionBinds.OPERATOR);
        ClientRegistry.registerKeyBinding(SanctionBinds.HUD);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }

    @Override
    public void handleMode(final boolean active) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientState.setMode(active);
            }
        });
    }

    @Override
    public void handleOpen(final byte kind) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                Minecraft mc = Minecraft.getMinecraft();
                GuiCatalog.Kind guiKind = kind == 1 ? GuiCatalog.Kind.OPERATOR : GuiCatalog.Kind.CATALOG;
                if (mc.currentScreen instanceof GuiCatalog && ((GuiCatalog) mc.currentScreen).kind() == guiKind) {
                    return;
                }
                mc.displayGuiScreen(new GuiCatalog(guiKind));
            }
        });
    }

    @Override
    public void handleGrants(List<ItemStack> stacks, List<Long> until) {
        final List<ItemStack> copyStacks = new ArrayList<ItemStack>(stacks);
        final List<Long> copyUntil = new ArrayList<Long>(until);
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientState.setGrants(copyStacks, copyUntil);
            }
        });
    }

    @Override
    public void handleGrant(ItemStack stack, final long until) {
        final ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientState.putExpiry(copy, until);
            }
        });
    }

    @Override
    public void handleRequests(List<ItemStack> stacks, List<String> names) {
        final List<ItemStack> copyStacks = new ArrayList<ItemStack>(stacks);
        final List<String> copyNames = new ArrayList<String>(names);
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientState.setRequests(copyStacks, copyNames);
            }
        });
    }
}
