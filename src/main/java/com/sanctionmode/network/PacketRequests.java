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

public class PacketRequests implements IMessage {

    private final List<ItemStack> stacks = new ArrayList<ItemStack>();
    private final List<String> names = new ArrayList<String>();

    public PacketRequests() {
    }

    public PacketRequests(List<ItemStack> stacks, List<String> names) {
        if (stacks != null) {
            this.stacks.addAll(stacks);
        }
        if (names != null) {
            this.names.addAll(names);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        if (count < 0 || count > 5000) {
            return;
        }
        for (int i = 0; i < count; i++) {
            stacks.add(ByteBufUtils.readItemStack(buf));
            names.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int count = Math.min(stacks.size(), names.size());
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            ByteBufUtils.writeItemStack(buf, stacks.get(i));
            ByteBufUtils.writeUTF8String(buf, names.get(i) == null ? "" : names.get(i));
        }
    }

    public static class Handler implements IMessageHandler<PacketRequests, IMessage> {
        @Override
        public IMessage onMessage(PacketRequests message, MessageContext ctx) {
            SanctionModeMod.proxy.handleRequests(message.stacks, message.names);
            return null;
        }
    }
}
