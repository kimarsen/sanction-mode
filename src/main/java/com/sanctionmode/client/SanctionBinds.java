package com.sanctionmode.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public final class SanctionBinds {

    public static final KeyBinding CATALOG = new KeyBinding("key.sanctionmode.catalog", Keyboard.KEY_G, "key.categories.sanctionmode");
    public static final KeyBinding OPERATOR = new KeyBinding("key.sanctionmode.operator", Keyboard.KEY_K, "key.categories.sanctionmode");
    public static final KeyBinding HUD = new KeyBinding("key.sanctionmode.hud", Keyboard.KEY_H, "key.categories.sanctionmode");

    private SanctionBinds() {
    }
}
