package com.sanctionmode.network;

import com.sanctionmode.SanctionService;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTake implements IMessage {

    private ItemStack stack = ItemStack.EMPTY;

    public PacketTake() {
    }

    public PacketTake(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readableBytes() > 32768) {
            stack = ItemStack.EMPTY;
            return;
        }
        stack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, stack);
    }

    public static class Handler implements IMessageHandler<PacketTake, IMessage> {
        @Override
        public IMessage onMessage(final PacketTake message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    SanctionService.take(player, message.stack);
                }
            });
            return null;
        }
    }
}
