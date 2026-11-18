package com.sanctionmode.command;

import com.sanctionmode.SanctionService;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CommandSanction extends CommandBase {

    @Override
    public String getName() {
        return "sanction";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "sanctionmode.help.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            if (!(sender instanceof EntityPlayerMP)) {
                throw new WrongUsageException("sanctionmode.help.usage");
            }
            SanctionService.openOperator((EntityPlayerMP) sender);
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!"on".equals(sub) && !"off".equals(sub)) {
            throw new WrongUsageException("sanctionmode.help.usage");
        }
        EntityPlayerMP target = args.length >= 2
            ? getPlayer(server, sender, args[1])
            : getCommandSenderAsPlayer(sender);
        if ("on".equals(sub)) {
            SanctionService.enable(sender, target);
        } else {
            SanctionService.disable(sender, target);
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }
}
