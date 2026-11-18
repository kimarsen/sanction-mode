package com.sanctionmode.network;

import com.sanctionmode.SanctionModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketGrants implements IMessage {

    private final List<ItemStack> stacks = new ArrayList<ItemStack>();
    private final List<Long> until = new ArrayList<Long>();

    public PacketGrants() {
    }

    public PacketGrants(List<ItemStack> stacks, List<Long> until) {
        if (stacks != null) {
            this.stacks.addAll(stacks);
        }
        if (until != null) {
            this.until.addAll(until);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        if (count < 0 || count > 50000) {
            return;
        }
        for (int i = 0; i < count; i++) {
            stacks.add(ByteBufUtils.readItemStack(buf));
            until.add(Long.valueOf(buf.readLong()));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int count = Math.min(stacks.size(), until.size());
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            ByteBufUtils.writeItemStack(buf, stacks.get(i));
            buf.writeLong(until.get(i).longValue());
        }
    }

    public static class Handler implements IMessageHandler<PacketGrants, IMessage> {
        @Override
        public IMessage onMessage(PacketGrants message, MessageContext ctx) {
            SanctionModeMod.proxy.handleGrants(message.stacks, message.until);
            return null;
        }
    }
}
