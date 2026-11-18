package com.sanctionmode.network;

import com.sanctionmode.SanctionService;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketAskOpen implements IMessage {

    private byte kind;

    public PacketAskOpen() {
    }

    public PacketAskOpen(byte kind) {
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

    public static class Handler implements IMessageHandler<PacketAskOpen, IMessage> {
        @Override
        public IMessage onMessage(final PacketAskOpen message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (message.kind == 1) {
                        SanctionService.openOperator(player);
                    } else {
                        SanctionService.syncGrants(player);
                    }
                }
            });
            return null;
        }
    }
}
