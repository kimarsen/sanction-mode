package com.sanctionmode.data;

import com.sanctionmode.SanctionKeys;
import com.sanctionmode.SanctionTime;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SanctionWorldData extends WorldSavedData {

    public static final String NAME = "sanctionmode";

    private final Map<String, Grant> grants = new LinkedHashMap<String, Grant>();

    public SanctionWorldData(String name) {
        super(name);
    }

    public static SanctionWorldData get(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        WorldServer world = server.getWorld(0);
        if (world == null) {
            return null;
        }
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            return null;
        }
        SanctionWorldData data = (SanctionWorldData) storage.getOrLoadData(SanctionWorldData.class, NAME);
        if (data == null) {
            data = new SanctionWorldData(NAME);
            storage.setData(NAME, data);
        }
        return data;
    }

    public long until(String key) {
        Grant grant = grants.get(key);
        if (grant == null) {
            return 0L;
        }
        if (grant.until <= System.currentTimeMillis()) {
            grants.remove(key);
            markDirty();
            return 0L;
        }
        return grant.until;
    }

    public long extend(ItemStack stack) {
        String key = SanctionKeys.of(stack);
        long now = System.currentTimeMillis();
        Grant current = grants.get(key);
        long base = current == null ? now : Math.max(now, current.until);
        long next = base + SanctionTime.HOUR;
        grants.put(key, new Grant(stack.copy(), next));
        markDirty();
        return next;
    }

    public void revoke(String key) {
        if (grants.remove(key) != null) {
            markDirty();
        }
    }

    public List<ItemStack> stacks() {
        long now = System.currentTimeMillis();
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        List<String> expired = new ArrayList<String>();
        for (Map.Entry<String, Grant> entry : grants.entrySet()) {
            if (entry.getValue().until <= now || entry.getValue().stack.isEmpty()) {
                expired.add(entry.getKey());
                continue;
            }
            stacks.add(entry.getValue().stack.copy());
        }
        for (String key : expired) {
            grants.remove(key);
        }
        if (!expired.isEmpty()) {
            markDirty();
        }
        return stacks;
    }

    public List<Long> deadlines() {
        long now = System.currentTimeMillis();
        List<Long> values = new ArrayList<Long>();
        for (Grant grant : grants.values()) {
            if (grant.until > now && !grant.stack.isEmpty()) {
                values.add(Long.valueOf(grant.until));
            }
        }
        return values;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        grants.clear();
        NBTTagList list = compound.getTagList("grants", 10);
        long now = System.currentTimeMillis();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            long deadline = tag.getLong("until");
            if (deadline <= now) {
                continue;
            }
            ItemStack stack = new ItemStack(tag.getCompoundTag("stack"));
            if (stack.isEmpty()) {
                continue;
            }
            stack.setCount(1);
            String key = SanctionKeys.of(stack);
            if (key.isEmpty()) {
                continue;
            }
            grants.put(key, new Grant(stack, deadline));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        long now = System.currentTimeMillis();
        for (Grant grant : grants.values()) {
            if (grant.until <= now || grant.stack.isEmpty()) {
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("stack", grant.stack.writeToNBT(new NBTTagCompound()));
            tag.setLong("until", grant.until);
            list.appendTag(tag);
        }
        compound.setTag("grants", list);
        return compound;
    }

    public static final class Grant {
        public final ItemStack stack;
        public final long until;

        public Grant(ItemStack stack, long until) {
            this.stack = stack;
            this.until = until;
        }
    }
}
