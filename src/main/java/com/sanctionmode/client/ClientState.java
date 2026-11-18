package com.sanctionmode.client;

import com.sanctionmode.SanctionKeys;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClientState {

    private static boolean mode;
    private static boolean hud;
    private static final Map<String, Long> expiry = new LinkedHashMap<String, Long>();
    private static final Map<String, ItemStack> grantStacks = new LinkedHashMap<String, ItemStack>();
    private static final List<String> requestKeys = new ArrayList<String>();
    private static final List<ItemStack> requestStacks = new ArrayList<ItemStack>();
    private static final List<String> requestNames = new ArrayList<String>();
    private static int requestSignature;

    private ClientState() {
    }

    public static boolean mode() {
        return mode;
    }

    public static void setMode(boolean active) {
        mode = active;
        if (!active) {
            hud = false;
        }
    }

    public static boolean hud() {
        return hud;
    }

    public static void toggleHud() {
        if (mode) {
            hud = !hud;
        }
    }

    public static void setGrants(List<ItemStack> stacks, List<Long> until) {
        expiry.clear();
        grantStacks.clear();
        int count = Math.min(stacks.size(), until.size());
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            ItemStack stack = stacks.get(i);
            long deadline = until.get(i).longValue();
            if (stack == null || stack.isEmpty() || deadline <= now) {
                continue;
            }
            String key = SanctionKeys.of(stack);
            if (key.isEmpty()) {
                continue;
            }
            expiry.put(key, Long.valueOf(deadline));
            grantStacks.put(key, stack.copy());
        }
    }

    public static void putExpiry(ItemStack stack, long until) {
        String key = SanctionKeys.of(stack);
        if (key.isEmpty()) {
            return;
        }
        if (until <= System.currentTimeMillis()) {
            expiry.remove(key);
            grantStacks.remove(key);
            return;
        }
        expiry.put(key, Long.valueOf(until));
        grantStacks.put(key, stack.copy());
    }

    public static long expiry(ItemStack stack) {
        Long value = expiry.get(SanctionKeys.of(stack));
        if (value == null) {
            return 0L;
        }
        if (value.longValue() <= System.currentTimeMillis()) {
            expiry.remove(SanctionKeys.of(stack));
            return 0L;
        }
        return value.longValue();
    }

    public static void setRequests(List<ItemStack> stacks, List<String> names) {
        requestKeys.clear();
        requestStacks.clear();
        requestNames.clear();
        int count = Math.min(stacks.size(), names.size());
        for (int i = 0; i < count; i++) {
            ItemStack stack = stacks.get(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String key = SanctionKeys.of(stack);
            if (key.isEmpty()) {
                continue;
            }
            requestKeys.add(key);
            requestStacks.add(stack.copy());
            requestNames.add(names.get(i) == null ? "" : names.get(i));
        }
        requestSignature++;
    }

    public static int requestSignature() {
        return requestSignature;
    }

    public static List<ItemStack> requestStacks() {
        Map<String, ItemStack> unique = new LinkedHashMap<String, ItemStack>();
        for (int i = 0; i < requestKeys.size(); i++) {
            if (!unique.containsKey(requestKeys.get(i))) {
                unique.put(requestKeys.get(i), requestStacks.get(i));
            }
        }
        return new ArrayList<ItemStack>(unique.values());
    }

    public static List<String> requesters(ItemStack stack) {
        String key = SanctionKeys.of(stack);
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < requestKeys.size(); i++) {
            if (!key.equals(requestKeys.get(i))) {
                continue;
            }
            String name = requestNames.get(i);
            if (!name.isEmpty() && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }
}
