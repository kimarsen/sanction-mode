package com.sanctionmode;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SanctionEvents {

    private static final Set<String> VANILLA_MODES = new HashSet<String>(Arrays.asList(
        "0", "1", "2", "3", "s", "c", "a", "sp", "survival", "creative", "adventure", "spectator"
    ));

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        if (event.player instanceof EntityPlayerMP) {
            SanctionAccess.maintain((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            SanctionService.login((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            SanctionService.forget((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            SanctionAccess.maintain(player);
            SanctionService.login(player);
        }
    }

    @SubscribeEvent
    public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            SanctionAccess.maintain(player);
            PacketModeSafe.sync(player);
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        ICommand command = event.getCommand();
        if (!isGameMode(command)) {
            return;
        }
        String[] args = event.getParameters();
        if (args == null || args.length == 0 || args[0] == null) {
            return;
        }
        String mode = args[0].toLowerCase(Locale.ROOT);
        ICommandSender sender = event.getSender();
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return;
        }
        if (isSanction(mode)) {
            event.setCanceled(true);
            try {
                EntityPlayerMP target = resolve(server, sender, args);
                SanctionService.enable(sender, target);
            } catch (CommandException exception) {
                TextComponentTranslation message = new TextComponentTranslation("commands.generic.player.notFound", args.length >= 2 ? args[1] : "");
                message.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(message);
            }
            return;
        }
        if (!VANILLA_MODES.contains(mode)) {
            return;
        }
        try {
            EntityPlayerMP target = resolve(server, sender, args);
            if (SanctionAccess.isActive(target)) {
                SanctionAccess.setActive(target, false);
                PacketModeSafe.sync(target);
            }
        } catch (CommandException ignored) {
        }
    }

    private static boolean isGameMode(ICommand command) {
        if (command == null) {
            return false;
        }
        if ("gamemode".equalsIgnoreCase(command.getName())) {
            return true;
        }
        List<String> aliases = command.getAliases();
        if (aliases == null) {
            return false;
        }
        for (String alias : aliases) {
            if ("gamemode".equalsIgnoreCase(alias)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSanction(String mode) {
        return "4".equals(mode) || "sanction".equals(mode) || "sanctioned".equals(mode) || "допуск".equals(mode);
    }

    private static EntityPlayerMP resolve(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 2) {
            return CommandBase.getPlayer(server, sender, args[1]);
        }
        return CommandBase.getCommandSenderAsPlayer(sender);
    }

    private static final class PacketModeSafe {
        private static void sync(EntityPlayerMP player) {
            com.sanctionmode.network.PacketHandler.sendTo(
                new com.sanctionmode.network.PacketMode(SanctionAccess.isActive(player)),
                player
            );
        }
    }
}
