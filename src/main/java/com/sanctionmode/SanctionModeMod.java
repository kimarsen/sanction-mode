package com.sanctionmode;

import com.sanctionmode.command.CommandSanction;
import com.sanctionmode.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = SanctionModeMod.MODID,
    name = SanctionModeMod.NAME,
    version = SanctionModeMod.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    acceptableRemoteVersions = "[1.0.0]",
    dependencies = "required-after:forge@[14.23.5.2847,)"
)
public class SanctionModeMod {

    public static final String MODID = "sanctionmode";
    public static final String NAME = "Sanction Mode";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MODID)
    public static SanctionModeMod instance;

    @SidedProxy(
        clientSide = "com.sanctionmode.client.ClientProxy",
        serverSide = "com.sanctionmode.CommonProxy"
    )
    public static CommonProxy proxy;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        PacketHandler.register();
        MinecraftForge.EVENT_BUS.register(new SanctionEvents());
        proxy.preInit(event);
        logger.info("Sanction Mode {} ready for Minecraft 1.12.2, Forge {}", VERSION, net.minecraftforge.common.ForgeVersion.getVersion());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandSanction());
    }
}
