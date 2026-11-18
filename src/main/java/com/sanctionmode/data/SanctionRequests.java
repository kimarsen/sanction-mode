package com.sanctionmode.data;

import com.sanctionmode.SanctionKeys;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SanctionRequests {

    private static final long KEEP = 6L * 60L * 60L * 1000L;
    private static final int LIMIT = 2000;
    private static final Map<String, Entry> pending = new LinkedHashMap<String, Entry>();

    private SanctionRequests() {
    }

    public static boolean add(EntityPlayerMP player, ItemStack stack) {
        purge();
        String itemKey = SanctionKeys.of(stack);
        if (itemKey.isEmpty()) {
            return false;
        }
        String id = player.getUniqueID().toString() + "|" + itemKey;
        if (pending.containsKey(id)) {
            return false;
        }
        if (pending.size() >= LIMIT) {
            return false;
        }
        pending.put(id, new Entry(player.getUniqueID(), player.getName(), stack.copy(), itemKey, System.currentTimeMillis()));
        return true;
    }

    public static void removeItem(String itemKey) {
        Iterator<Entry> iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            if (itemKey.equals(iterator.next().key)) {
                iterator.remove();
            }
        }
    }

    public static void forget(UUID playerId) {
        Iterator<Entry> iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            if (playerId.equals(iterator.next().playerId)) {
                iterator.remove();
            }
        }
    }

    public static List<Entry> all() {
        purge();
        return new ArrayList<Entry>(pending.values());
    }

    private static void purge() {
        long now = System.currentTimeMillis();
        Iterator<Entry> iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().time > KEEP) {
                iterator.remove();
            }
        }
    }

    public static final class Entry {
        public final UUID playerId;
        public final String playerName;
        public final ItemStack stack;
        public final String key;
        public final long time;

        public Entry(UUID playerId, String playerName, ItemStack stack, String key, long time) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.stack = stack;
            this.key = key;
            this.time = time;
        }
    }
}
