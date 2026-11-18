package com.sanctionmode.network;

import com.sanctionmode.SanctionModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketGrantOne implements IMessage {

    private ItemStack stack = ItemStack.EMPTY;
    private long until;

    public PacketGrantOne() {
    }

    public PacketGrantOne(ItemStack stack, long until) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.until = until;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stack = ByteBufUtils.readItemStack(buf);
        until = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, stack);
        buf.writeLong(until);
    }

    public static class Handler implements IMessageHandler<PacketGrantOne, IMessage> {
        @Override
        public IMessage onMessage(PacketGrantOne message, MessageContext ctx) {
            SanctionModeMod.proxy.handleGrant(message.stack, message.until);
            return null;
        }
    }
}
