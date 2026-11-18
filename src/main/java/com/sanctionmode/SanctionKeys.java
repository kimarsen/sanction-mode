package com.sanctionmode;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SanctionKeys {

    private SanctionKeys() {
    }

    public static String of(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return "";
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        StringBuilder builder = new StringBuilder(name.toString());
        builder.append('|').append(stack.getMetadata()).append('|');
        if (stack.hasTagCompound() && !stack.getTagCompound().isEmpty()) {
            append(builder, stack.getTagCompound());
        }
        return builder.toString();
    }

    private static void append(StringBuilder builder, NBTBase base) {
        if (base instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) base;
            List<String> keys = new ArrayList<String>(compound.getKeySet());
            Collections.sort(keys);
            builder.append('{');
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                String key = keys.get(i);
                builder.append(key).append(':');
                append(builder, compound.getTag(key));
            }
            builder.append('}');
            return;
        }
        if (base instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) base;
            builder.append('[');
            for (int i = 0; i < list.tagCount(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                append(builder, list.get(i));
            }
            builder.append(']');
            return;
        }
        builder.append(base.toString());
    }
}
