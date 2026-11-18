package com.sanctionmode.network;

import com.sanctionmode.SanctionModeMod;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class PacketHandler {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(SanctionModeMod.MODID);
    private static int nextId;

    private PacketHandler() {
    }

    public static void register() {
        CHANNEL.registerMessage(PacketAskOpen.Handler.class, PacketAskOpen.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketTake.Handler.class, PacketTake.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketGrantAction.Handler.class, PacketGrantAction.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketMode.Handler.class, PacketMode.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketOpen.Handler.class, PacketOpen.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketGrants.Handler.class, PacketGrants.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketGrantOne.Handler.class, PacketGrantOne.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketRequests.Handler.class, PacketRequests.class, nextId++, Side.CLIENT);
    }

    public static void sendTo(IMessage message, EntityPlayerMP player) {
        CHANNEL.sendTo(message, player);
    }

    public static void sendToServer(IMessage message) {
        CHANNEL.sendToServer(message);
    }
}
