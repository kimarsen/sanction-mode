package com.sanctionmode;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemCatalog {

    private static final Map<String, ItemStack> byKey = new LinkedHashMap<String, ItemStack>();
    private static final Map<CreativeTabs, List<ItemStack>> byTab = new LinkedHashMap<CreativeTabs, List<ItemStack>>();
    private static final List<ItemStack> all = new ArrayList<ItemStack>();
    private static final Set<Item> seen = new HashSet<Item>();
    private static boolean ready;

    private ItemCatalog() {
    }

    public static void ensure() {
        if (ready) {
            return;
        }
        synchronized (ItemCatalog.class) {
            if (ready) {
                return;
            }
            build();
            ready = true;
        }
    }

    public static List<ItemStack> all() {
        ensure();
        return all;
    }

    public static List<CreativeTabs> tabs() {
        ensure();
        return new ArrayList<CreativeTabs>(byTab.keySet());
    }

    public static List<ItemStack> forTab(CreativeTabs tab) {
        ensure();
        List<ItemStack> list = byTab.get(tab);
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    public static boolean contains(ItemStack stack) {
        return !canonical(stack).isEmpty();
    }

    public static ItemStack canonical(ItemStack stack) {
        ensure();
        ItemStack clean = sanitize(stack);
        if (clean.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack found = byKey.get(SanctionKeys.of(clean));
        return found == null ? ItemStack.EMPTY : found;
    }

    private static void build() {
        long started = System.currentTimeMillis();
        byKey.clear();
        byTab.clear();
        all.clear();
        seen.clear();
        List<CreativeTabs> tabs = new ArrayList<CreativeTabs>();
        for (CreativeTabs tab : CreativeTabs.CREATIVE_TAB_ARRAY) {
            if (tab != null && tab != CreativeTabs.SEARCH && tab != CreativeTabs.INVENTORY) {
                tabs.add(tab);
            }
        }
        for (Item item : Item.REGISTRY) {
            if (item == null || item == Items.AIR) {
                continue;
            }
            for (CreativeTabs tab : tabs) {
                if (!owns(item, tab)) {
                    continue;
                }
                NonNullList<ItemStack> found = NonNullList.create();
                try {
                    item.getSubItems(tab, found);
                } catch (Throwable ignored) {
                    continue;
                }
                List<ItemStack> stored = byTab.get(tab);
                if (stored == null) {
                    stored = new ArrayList<ItemStack>();
                    byTab.put(tab, stored);
                }
                for (ItemStack stack : found) {
                    ItemStack interned = intern(stack);
                    if (!interned.isEmpty() && !stored.contains(interned)) {
                        stored.add(interned);
                    }
                }
            }
            if (!seen.contains(item)) {
                try {
                    intern(new ItemStack(item));
                } catch (Throwable ignored) {
                }
            }
        }
        if (SanctionModeMod.logger != null) {
            SanctionModeMod.logger.info("Indexed {} catalog items in {} ms", Integer.valueOf(all.size()), Long.valueOf(System.currentTimeMillis() - started));
        }
    }

    private static boolean owns(Item item, CreativeTabs tab) {
        try {
            CreativeTabs[] own = item.getCreativeTabs();
            if (own != null) {
                for (int i = 0; i < own.length; i++) {
                    if (own[i] == tab) {
                        return true;
                    }
                }
            }
            return item.getCreativeTab() == tab;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ItemStack intern(ItemStack stack) {
        ItemStack clean = sanitize(stack);
        if (clean.isEmpty()) {
            return ItemStack.EMPTY;
        }
        String key = SanctionKeys.of(clean);
        if (key.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = byKey.get(key);
        if (existing != null) {
            return existing;
        }
        byKey.put(key, clean);
        all.add(clean);
        seen.add(clean.getItem());
        return clean;
    }

    public static ItemStack sanitize(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null || stack.getItem() == Items.AIR) {
            return ItemStack.EMPTY;
        }
        if (stack.getItem().getRegistryName() == null) {
            return ItemStack.EMPTY;
        }
        int meta = stack.getMetadata();
        if (meta < 0 || meta > 32766) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = new ItemStack(stack.getItem(), 1, meta);
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound().copy();
            if (tag.isEmpty()) {
                return copy;
            }
            if (tag.toString().length() > 12000) {
                return ItemStack.EMPTY;
            }
            copy.setTagCompound(tag);
        }
        return copy;
    }
}
