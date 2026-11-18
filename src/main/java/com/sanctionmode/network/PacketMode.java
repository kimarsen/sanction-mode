package com.sanctionmode.network;

import com.sanctionmode.SanctionModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMode implements IMessage {

    private boolean active;

    public PacketMode() {
    }

    public PacketMode(boolean active) {
        this.active = active;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        active = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(active);
    }

    public static class Handler implements IMessageHandler<PacketMode, IMessage> {
        @Override
        public IMessage onMessage(PacketMode message, MessageContext ctx) {
            SanctionModeMod.proxy.handleMode(message.active);
            return null;
        }
    }
}
