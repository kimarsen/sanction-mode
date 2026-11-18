package com.sanctionmode;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public final class SanctionTime {

    public static final long HOUR = 3600000L;

    private SanctionTime() {
    }

    public static ITextComponent granted(ItemStack stack, long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        String name = stack.getDisplayName();
        if (hours > 0L) {
            return new TextComponentTranslation("sanctionmode.granted.hm", name, Long.valueOf(hours), Long.valueOf(minutes));
        }
        if (minutes > 0L) {
            return new TextComponentTranslation("sanctionmode.granted.ms", name, Long.valueOf(minutes), Long.valueOf(secs));
        }
        return new TextComponentTranslation("sanctionmode.granted.s", name, Long.valueOf(secs));
    }
}
