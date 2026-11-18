package com.sanctionmode.client;

import com.sanctionmode.ItemCatalog;
import com.sanctionmode.client.gui.GuiCatalog;
import com.sanctionmode.network.PacketAskOpen;
import com.sanctionmode.network.PacketHandler;
import com.sanctionmode.network.PacketTake;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class ClientEvents {

    private static final int CATALOG_BUTTON = 428191;
    private boolean pickWasDown;
    private boolean catalogReady;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (!catalogReady) {
            ItemCatalog.ensure();
            catalogReady = true;
        }
        boolean openCatalog = SanctionBinds.CATALOG.isPressed();
        boolean openOperator = SanctionBinds.OPERATOR.isPressed();
        boolean toggleHud = SanctionBinds.HUD.isPressed();
        if (mc.currentScreen == null && toggleHud) {
            ClientState.toggleHud();
        }
        if (mc.currentScreen == null && openCatalog && ClientState.mode()) {
            mc.displayGuiScreen(new GuiCatalog(GuiCatalog.Kind.CATALOG));
            PacketHandler.sendToServer(new PacketAskOpen((byte) 0));
        }
        if (mc.currentScreen == null && openOperator) {
            PacketHandler.sendToServer(new PacketAskOpen((byte) 1));
        }
        boolean down = pickDown(mc);
        if (down && !pickWasDown && mc.currentScreen == null && ClientState.mode()) {
            requestLookedBlock(mc);
        }
        pickWasDown = down;
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiInventory) || !ClientState.mode()) {
            return;
        }
        int x = event.getGui().width / 2 - 40;
        event.getButtonList().add(new GuiButton(CATALOG_BUTTON, x, 2, 80, 20, net.minecraft.client.resources.I18n.format("sanctionmode.button.catalog")));
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getButton().id != CATALOG_BUTTON || !(event.getGui() instanceof GuiInventory)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new GuiCatalog(GuiCatalog.Kind.CATALOG));
        PacketHandler.sendToServer(new PacketAskOpen((byte) 0));
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || !ClientState.mode() || !ClientState.hud()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.gameSettings.showDebugInfo) {
            return;
        }
        String text = net.minecraft.client.resources.I18n.format("sanctionmode.hud");
        int x = (event.getResolution().getScaledWidth() - mc.fontRenderer.getStringWidth(text)) / 2;
        mc.fontRenderer.drawStringWithShadow(text, x, 2, 0x55FF55);
    }

    private static boolean pickDown(Minecraft mc) {
        int code = mc.gameSettings.keyBindPickBlock.getKeyCode();
        if (code == 0) {
            return false;
        }
        if (code < 0) {
            return Mouse.isButtonDown(code + 100);
        }
        return Keyboard.isKeyDown(code);
    }

    private static void requestLookedBlock(Minecraft mc) {
        RayTraceResult hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || mc.world == null || mc.player == null) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        IBlockState state = mc.world.getBlockState(pos);
        Block block = state.getBlock();
        ItemStack picked;
        try {
            picked = block.getPickBlock(state, hit, mc.world, pos, mc.player);
        } catch (Throwable error) {
            picked = ItemStack.EMPTY;
        }
        if (picked.isEmpty()) {
            Item item = Item.getItemFromBlock(block);
            if (item != null && item != Items.AIR) {
                picked = new ItemStack(item, 1, block.damageDropped(state));
            }
        }
        if (picked.isEmpty()) {
            return;
        }
        picked = picked.copy();
        picked.setCount(1);
        if (!ItemCatalog.contains(picked)) {
            ItemStack plain = new ItemStack(picked.getItem(), 1, picked.getMetadata());
            if (ItemCatalog.contains(plain)) {
                picked = plain;
            }
        }
        PacketHandler.sendToServer(new PacketTake(picked));
    }
}
