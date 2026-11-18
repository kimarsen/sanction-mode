package com.sanctionmode.network;

import com.sanctionmode.SanctionService;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketGrantAction implements IMessage {

    private ItemStack stack = ItemStack.EMPTY;
    private boolean extend;

    public PacketGrantAction() {
    }

    public PacketGrantAction(ItemStack stack, boolean extend) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.extend = extend;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readableBytes() > 32768) {
            stack = ItemStack.EMPTY;
            extend = false;
            return;
        }
        stack = ByteBufUtils.readItemStack(buf);
        extend = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, stack);
        buf.writeBoolean(extend);
    }

    public static class Handler implements IMessageHandler<PacketGrantAction, IMessage> {
        @Override
        public IMessage onMessage(final PacketGrantAction message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    SanctionService.grant(player, message.stack, message.extend);
                }
            });
            return null;
        }
    }
}
