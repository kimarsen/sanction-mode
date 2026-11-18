package com.sanctionmode.network;

import com.sanctionmode.SanctionModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketOpen implements IMessage {

    private byte kind;

    public PacketOpen() {
    }

    public PacketOpen(byte kind) {
        this.kind = kind;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind);
    }

    public static class Handler implements IMessageHandler<PacketOpen, IMessage> {
        @Override
        public IMessage onMessage(PacketOpen message, MessageContext ctx) {
            SanctionModeMod.proxy.handleOpen(message.kind);
            return null;
        }
    }
}
