package com.sanctionmode;

import com.sanctionmode.data.SanctionRequests;
import com.sanctionmode.data.SanctionWorldData;
import com.sanctionmode.network.PacketGrantOne;
import com.sanctionmode.network.PacketGrants;
import com.sanctionmode.network.PacketHandler;
import com.sanctionmode.network.PacketMode;
import com.sanctionmode.network.PacketOpen;
import com.sanctionmode.network.PacketRequests;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SanctionService {

    private static final Map<UUID, Window> takes = new HashMap<UUID, Window>();
    private static final Map<UUID, Long> pendingNotice = new HashMap<UUID, Long>();

    private SanctionService() {
    }

    public static boolean isOperator(EntityPlayerMP player) {
        return player != null && player.canUseCommand(2, "sanction");
    }

    public static void enable(ICommandSender sender, EntityPlayerMP target) {
        SanctionAccess.setActive(target, true);
        boolean flying = target.capabilities.isFlying;
        if (target.interactionManager.getGameType() != GameType.SURVIVAL) {
            target.setGameType(GameType.SURVIVAL);
        }
        SanctionAccess.apply(target, flying);
        PacketHandler.sendTo(new PacketMode(true), target);
        syncGrants(target);
        ITextComponent on = new TextComponentTranslation("sanctionmode.mode.on", target.getName());
        on.getStyle().setColor(TextFormatting.GREEN);
        ITextComponent hint = new TextComponentTranslation("sanctionmode.mode.hint");
        hint.getStyle().setColor(TextFormatting.GRAY);
        sender.sendMessage(on);
        sender.sendMessage(hint);
        if (sender != target) {
            target.sendMessage(on);
            target.sendMessage(hint);
        }
        SanctionModeMod.logger.info("Sanction mode enabled for {}", target.getName());
    }

    public static void disable(ICommandSender sender, EntityPlayerMP target) {
        SanctionAccess.setActive(target, false);
        target.setGameType(target.interactionManager.getGameType());
        PacketHandler.sendTo(new PacketMode(false), target);
        ITextComponent off = new TextComponentTranslation("sanctionmode.mode.off", target.getName());
        off.getStyle().setColor(TextFormatting.RED);
        sender.sendMessage(off);
        if (sender != target) {
            target.sendMessage(off);
        }
        SanctionModeMod.logger.info("Sanction mode disabled for {}", target.getName());
    }

    public static void login(EntityPlayerMP player) {
        PacketHandler.sendTo(new PacketMode(SanctionAccess.isActive(player)), player);
        syncGrants(player);
        if (isOperator(player)) {
            syncRequests(player);
        }
        if (SanctionAccess.isActive(player)) {
            SanctionAccess.maintain(player);
        }
    }

    public static void forget(EntityPlayerMP player) {
        takes.remove(player.getUniqueID());
        pendingNotice.remove(player.getUniqueID());
        SanctionRequests.forget(player.getUniqueID());
        if (player.getServer() != null) {
            syncRequestsToOps(player.getServer());
        }
    }

    public static void openOperator(EntityPlayerMP player) {
        if (!isOperator(player)) {
            player.sendStatusMessage(colored("sanctionmode.no_permission", TextFormatting.RED), true);
            return;
        }
        syncGrants(player);
        syncRequests(player);
        PacketHandler.sendTo(new PacketOpen((byte) 1), player);
    }

    public static void syncGrants(EntityPlayerMP player) {
        SanctionWorldData data = SanctionWorldData.get(player.getServer());
        if (data == null) {
            PacketHandler.sendTo(new PacketGrants(new ArrayList<ItemStack>(), new ArrayList<Long>()), player);
            return;
        }
        PacketHandler.sendTo(new PacketGrants(data.stacks(), data.deadlines()), player);
    }

    public static void take(EntityPlayerMP player, ItemStack requested) {
        if (player == null) {
            return;
        }
        if (!SanctionAccess.isActive(player)) {
            player.sendStatusMessage(colored("sanctionmode.not_active", TextFormatting.RED), true);
            return;
        }
        if (!allowTake(player)) {
            return;
        }
        ItemStack canonical = ItemCatalog.canonical(requested);
        if (canonical.isEmpty()) {
            player.sendStatusMessage(colored("sanctionmode.unknown", TextFormatting.RED), true);
            return;
        }
        String key = SanctionKeys.of(canonical);
        SanctionWorldData data = SanctionWorldData.get(player.getServer());
        long until = data == null ? 0L : data.until(key);
        if (until <= System.currentTimeMillis()) {
            boolean created = SanctionRequests.add(player, canonical);
            if (player.getServer() != null) {
                syncRequestsToOps(player.getServer());
            }
            if (created) {
                notifyOperators(player, canonical);
                player.sendStatusMessage(colored("sanctionmode.request.sent", TextFormatting.GOLD), true);
            } else if (noticePending(player)) {
                player.sendStatusMessage(colored("sanctionmode.request.pending", TextFormatting.YELLOW), true);
            }
            return;
        }
        ItemStack give = canonical.copy();
        give.setCount(give.getMaxStackSize());
        int wanted = give.getCount();
        boolean fit = player.inventory.addItemStackToInventory(give);
        player.inventoryContainer.detectAndSendChanges();
        int leftover = give.isEmpty() ? 0 : give.getCount();
        if (leftover < wanted) {
            float pitch = ((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7F + 1.0F) * 2.0F;
            player.connection.sendPacket(new SPacketSoundEffect(
                SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS,
                player.posX,
                player.posY,
                player.posZ,
                0.2F,
                pitch
            ));
        }
        if (!fit) {
            player.sendStatusMessage(colored("sanctionmode.inventory.full", TextFormatting.RED), true);
        }
    }

    public static void grant(EntityPlayerMP operator, ItemStack requested, boolean extend) {
        if (!isOperator(operator)) {
            operator.sendStatusMessage(colored("sanctionmode.no_permission", TextFormatting.RED), true);
            return;
        }
        ItemStack canonical = ItemCatalog.canonical(requested);
        if (canonical.isEmpty()) {
            operator.sendStatusMessage(colored("sanctionmode.unknown", TextFormatting.RED), true);
            return;
        }
        SanctionWorldData data = SanctionWorldData.get(operator.getServer());
        if (data == null) {
            return;
        }
        String key = SanctionKeys.of(canonical);
        if (extend) {
            long until = data.extend(canonical);
            SanctionRequests.removeItem(key);
            broadcastGrant(operator.getServer(), canonical, until);
            syncRequestsToOps(operator.getServer());
            ITextComponent message = SanctionTime.granted(canonical, until - System.currentTimeMillis());
            message.getStyle().setColor(TextFormatting.GREEN);
            operator.sendStatusMessage(message, true);
            return;
        }
        data.revoke(key);
        broadcastGrant(operator.getServer(), canonical, 0L);
        ITextComponent message = new TextComponentTranslation("sanctionmode.revoked", canonical.getDisplayName());
        message.getStyle().setColor(TextFormatting.RED);
        operator.sendStatusMessage(message, true);
    }

    private static void broadcastGrant(MinecraftServer server, ItemStack stack, long until) {
        if (server == null) {
            return;
        }
        PacketGrantOne packet = new PacketGrantOne(stack.copy(), until);
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            PacketHandler.sendTo(packet, player);
        }
    }

    private static void syncRequests(EntityPlayerMP player) {
        List<SanctionRequests.Entry> entries = SanctionRequests.all();
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        List<String> names = new ArrayList<String>();
        for (SanctionRequests.Entry entry : entries) {
            stacks.add(entry.stack.copy());
            names.add(entry.playerName);
        }
        PacketHandler.sendTo(new PacketRequests(stacks, names), player);
    }

    private static void syncRequestsToOps(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (isOperator(player)) {
                syncRequests(player);
            }
        }
    }

    private static void notifyOperators(EntityPlayerMP requester, ItemStack stack) {
        MinecraftServer server = requester.getServer();
        if (server == null) {
            return;
        }
        ITextComponent message = new TextComponentTranslation("sanctionmode.request.notify", requester.getName(), stack.getDisplayName());
        message.getStyle().setColor(TextFormatting.GOLD);
        message.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sanction"));
        message.getStyle().setHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new TextComponentTranslation("sanctionmode.request.open")
        ));
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (isOperator(player)) {
                player.sendMessage(message);
            }
        }
    }

    private static boolean allowTake(EntityPlayerMP player) {
        long now = System.currentTimeMillis();
        Window window = takes.get(player.getUniqueID());
        if (window == null || now - window.start >= 1000L) {
            takes.put(player.getUniqueID(), new Window(now, 1));
            return true;
        }
        if (window.count >= 60) {
            return false;
        }
        window.count++;
        return true;
    }

    private static boolean noticePending(EntityPlayerMP player) {
        long now = System.currentTimeMillis();
        Long last = pendingNotice.get(player.getUniqueID());
        if (last != null && now - last.longValue() < 800L) {
            return false;
        }
        pendingNotice.put(player.getUniqueID(), Long.valueOf(now));
        return true;
    }

    private static ITextComponent colored(String key, TextFormatting color) {
        ITextComponent message = new TextComponentTranslation(key);
        message.getStyle().setColor(color);
        return message;
    }

    private static final class Window {
        private final long start;
        private int count;

        private Window(long start, int count) {
            this.start = start;
            this.count = count;
        }
    }
}
