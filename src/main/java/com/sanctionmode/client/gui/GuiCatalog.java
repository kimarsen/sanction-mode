package com.sanctionmode.client.gui;

import com.sanctionmode.ItemCatalog;
import com.sanctionmode.SanctionTime;
import com.sanctionmode.client.ClientState;
import com.sanctionmode.network.PacketGrantAction;
import com.sanctionmode.network.PacketHandler;
import com.sanctionmode.network.PacketTake;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SideOnly(Side.CLIENT)
public class GuiCatalog extends GuiScreen {

    public enum Kind {
        CATALOG,
        OPERATOR
    }

    private static final int COLS = 9;
    private static final int ROWS = 5;
    private static final int SLOT = 18;
    private static final int TAB_W = 28;
    private static final int TAB_H = 26;
    private static final int PANEL_W = 220;
    private static final int PANEL_H = 168;

    private final Kind kind;
    private final List<CreativeTabs> tabs = new ArrayList<CreativeTabs>();
    private final List<ItemStack> view = new ArrayList<ItemStack>();
    private GuiTextField search;
    private int panelX;
    private int panelY;
    private int selected;
    private int scroll;
    private int tabScroll;
    private boolean dragging;
    private String lastQuery = "";
    private int lastRequestSignature = -1;
    private ItemStack hovered = ItemStack.EMPTY;
    private String hoveredTab = "";

    public GuiCatalog(Kind kind) {
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void initGui() {
        super.initGui();
        ItemCatalog.ensure();
        tabs.clear();
        tabs.addAll(ItemCatalog.tabs());
        layout();
        String kept = search == null ? "" : search.getText();
        search = new GuiTextField(0, fontRenderer, panelX + 8, panelY + 18, PANEL_W - 16, 14);
        search.setMaxStringLength(64);
        search.setFocused(true);
        search.setText(kept);
        Keyboard.enableRepeatEvents(true);
        lastQuery = "";
        rebuild();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        if (search == null) {
            return;
        }
        search.updateCursorCounter();
        String query = search.getText();
        int signature = ClientState.requestSignature();
        boolean requests = isRequestsTab();
        if (!query.equals(lastQuery) || (requests && signature != lastRequestSignature)) {
            if (!query.equals(lastQuery)) {
                scroll = 0;
            }
            lastQuery = query;
            lastRequestSignature = signature;
            rebuild();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        hovered = ItemStack.EMPTY;
        hoveredTab = "";
        drawDefaultBackground();
        drawRect(panelX - 2, panelY - 2, panelX + PANEL_W + 2, panelY + PANEL_H + 2, 0xFF000000);
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xF0101010);
        String title = I18n.format(kind == Kind.OPERATOR ? "sanctionmode.operator.title" : "sanctionmode.catalog.title");
        drawCenteredString(fontRenderer, title, panelX + PANEL_W / 2, panelY + 6, 0xFFFFFF);
        drawTabs(mouseX, mouseY);
        int gridX = gridX();
        int gridY = gridY();
        int startRow = scroll;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = (startRow + row) * COLS + col;
                int x = gridX + col * SLOT;
                int y = gridY + row * SLOT;
                drawSlotBackground(x, y);
                if (index >= view.size()) {
                    continue;
                }
                ItemStack stack = view.get(index);
                long until = ClientState.expiry(stack);
                boolean pending = !ClientState.requesters(stack).isEmpty();
                if (until > System.currentTimeMillis()) {
                    drawBorder(x, y, 0xFF3DDC84);
                } else if (pending && kind == Kind.OPERATOR) {
                    drawBorder(x, y, 0xFFE1B341);
                }
            }
        }
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        mc.getRenderItem().zLevel = 100.0F;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = (startRow + row) * COLS + col;
                if (index >= view.size()) {
                    continue;
                }
                ItemStack stack = view.get(index);
                int x = gridX + col * SLOT;
                int y = gridY + row * SLOT;
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
                mc.getRenderItem().renderItemOverlayIntoGUI(fontRenderer, stack, x, y, null);
            }
        }
        mc.getRenderItem().zLevel = 0.0F;
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        drawScrollbar();
        if (search != null) {
            search.drawTextBox();
            if (search.getText().isEmpty() && !search.isFocused()) {
                fontRenderer.drawString(I18n.format("sanctionmode.tab.all"), search.x + 4, search.y + 3, 0x808080);
            }
        }
        int index = slotIndex(mouseX, mouseY);
        if (index >= 0) {
            hovered = view.get(index);
        }
        String hint = hovered.isEmpty()
            ? I18n.format(kind == Kind.OPERATOR ? "sanctionmode.grant.hint" : "sanctionmode.catalog.hint")
            : statusLine(hovered);
        fontRenderer.drawSplitString(hint, panelX + 8, panelY + PANEL_H - 24, PANEL_W - 16, 0xA0A0A0);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!hovered.isEmpty()) {
            drawStackTooltip(hovered, mouseX, mouseY);
        } else if (!hoveredTab.isEmpty()) {
            List<String> lines = new ArrayList<String>();
            lines.add(hoveredTab);
            drawHoveringText(lines, mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (search != null) {
            search.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton == 0 && overTab(mouseX, mouseY)) {
            int index = tabScroll + (mouseY - panelY) / TAB_H;
            if (index >= 0 && index < tabCount()) {
                selected = index;
                scroll = 0;
                rebuild();
                playClick();
            }
            return;
        }
        if (mouseButton == 0 && overScrollbar(mouseX, mouseY) && maxScroll() > 0) {
            dragging = true;
            updateScrollFromMouse(mouseY);
            return;
        }
        int index = slotIndex(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        ItemStack stack = view.get(index);
        if (kind == Kind.OPERATOR) {
            if (mouseButton == 0) {
                long now = System.currentTimeMillis();
                long current = ClientState.expiry(stack);
                ClientState.putExpiry(stack, Math.max(now, current) + SanctionTime.HOUR);
                PacketHandler.sendToServer(new PacketGrantAction(stack, true));
                playClick();
            } else if (mouseButton == 1) {
                ClientState.putExpiry(stack, 0L);
                PacketHandler.sendToServer(new PacketGrantAction(stack, false));
                playClick();
            }
        } else if (mouseButton == 0) {
            PacketHandler.sendToServer(new PacketTake(stack));
            playClick();
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragging) {
            updateScrollFromMouse(mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        dragging = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (mouseX < panelX) {
            if (wheel > 0) {
                tabScroll = Math.max(0, tabScroll - 1);
            } else {
                tabScroll = Math.min(maxTabScroll(), tabScroll + 1);
            }
            return;
        }
        if (wheel > 0) {
            scroll = Math.max(0, scroll - 1);
        } else {
            scroll = Math.min(maxScroll(), scroll + 1);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (search != null && search.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void layout() {
        panelX = (width - (PANEL_W + TAB_W)) / 2 + TAB_W;
        panelY = Math.max(8, (height - PANEL_H) / 2);
    }

    private void rebuild() {
        String query = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<ItemStack> source;
        if (isRequestsTab()) {
            source = ClientState.requestStacks();
        } else if (isAllTab()) {
            source = ItemCatalog.all();
        } else {
            CreativeTabs tab = currentTab();
            source = tab == null ? ItemCatalog.all() : ItemCatalog.forTab(tab);
        }
        view.clear();
        for (ItemStack stack : source) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (query.isEmpty() || matches(stack, query)) {
                view.add(stack);
            }
        }
        if (scroll > maxScroll()) {
            scroll = maxScroll();
        }
    }

    private boolean matches(ItemStack stack, String query) {
        if (stack.getDisplayName().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        ResourceLocation id = stack.getItem().getRegistryName();
        return id != null && id.toString().toLowerCase(Locale.ROOT).contains(query);
    }

    private void drawTabs(int mouseX, int mouseY) {
        int visible = Math.max(1, PANEL_H / TAB_H);
        int shown = Math.min(visible, tabCount() - tabScroll);
        for (int i = 0; i < shown; i++) {
            int index = tabScroll + i;
            int x = panelX - TAB_W;
            int y = panelY + i * TAB_H;
            boolean active = index == selected;
            drawRect(x, y, x + TAB_W - 2, y + TAB_H - 2, active ? 0xFF245C32 : 0xFF2A2A2A);
            ItemStack icon = tabIcon(index);
            if (!icon.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(icon, x + 5, y + 4);
                RenderHelper.disableStandardItemLighting();
            }
            if (mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < y + TAB_H) {
                hoveredTab = tabLabel(index);
            }
        }
    }

    private void drawSlotBackground(int x, int y) {
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        drawRect(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    private void drawBorder(int x, int y, int color) {
        drawRect(x - 1, y - 1, x + 17, y, color);
        drawRect(x - 1, y + 16, x + 17, y + 17, color);
        drawRect(x - 1, y, x, y + 16, color);
        drawRect(x + 16, y, x + 17, y + 16, color);
    }

    private void drawScrollbar() {
        int x = gridX() + COLS * SLOT + 4;
        int y = gridY();
        int height = ROWS * SLOT;
        drawRect(x, y, x + 8, y + height, 0xFF202020);
        int max = maxScroll();
        if (max <= 0) {
            drawRect(x, y, x + 8, y + height, 0xFF606060);
            return;
        }
        int thumb = Math.max(12, height * ROWS / (ROWS + max));
        int travel = height - thumb;
        int thumbY = y + (max == 0 ? 0 : travel * scroll / max);
        drawRect(x, thumbY, x + 8, thumbY + thumb, 0xFFB0B0B0);
    }

    private void drawStackTooltip(ItemStack stack, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<String>(stack.getTooltip(
            mc.player,
            mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL
        ));
        long until = ClientState.expiry(stack);
        long now = System.currentTimeMillis();
        if (until > now) {
            lines.add(TextFormatting.GREEN + I18n.format("sanctionmode.allowed", timeText(until - now)));
        } else if (kind == Kind.OPERATOR) {
            lines.add(TextFormatting.YELLOW + I18n.format("sanctionmode.grant.hint"));
        } else {
            lines.add(TextFormatting.RED + I18n.format("sanctionmode.denied"));
        }
        if (kind == Kind.OPERATOR) {
            List<String> names = ClientState.requesters(stack);
            if (!names.isEmpty()) {
                lines.add(TextFormatting.GOLD + I18n.format("sanctionmode.requesters", join(names)));
            }
        }
        drawHoveringText(lines, mouseX, mouseY);
    }

    private String statusLine(ItemStack stack) {
        long until = ClientState.expiry(stack);
        long now = System.currentTimeMillis();
        String name = stack.getDisplayName();
        if (fontRenderer.getStringWidth(name) > PANEL_W - 16) {
            name = fontRenderer.trimStringToWidth(name, PANEL_W - 24) + "...";
        }
        if (until > now) {
            return TextFormatting.GREEN + name + " " + timeText(until - now);
        }
        if (kind == Kind.OPERATOR) {
            return TextFormatting.YELLOW + name;
        }
        return TextFormatting.RED + name;
    }

    private String timeText(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (hours > 0L) {
            return I18n.format("sanctionmode.time.hm", Long.valueOf(hours), Long.valueOf(minutes));
        }
        if (minutes > 0L) {
            return I18n.format("sanctionmode.time.ms", Long.valueOf(minutes), Long.valueOf(secs));
        }
        return I18n.format("sanctionmode.time.s", Long.valueOf(secs));
    }

    private String join(List<String> names) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size() && i < 6; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(names.get(i));
        }
        if (names.size() > 6) {
            builder.append(" +").append(names.size() - 6);
        }
        return builder.toString();
    }

    private ItemStack tabIcon(int index) {
        if (isAllIndex(index)) {
            return new ItemStack(Items.COMPASS);
        }
        if (isRequestsIndex(index)) {
            return new ItemStack(Items.PAPER);
        }
        CreativeTabs tab = tabAt(index);
        if (tab == null) {
            return new ItemStack(Items.PAPER);
        }
        try {
            ItemStack icon = tab.getIcon();
            if (icon != null && !icon.isEmpty()) {
                return icon;
            }
        } catch (Throwable ignored) {
        }
        return new ItemStack(Items.PAPER);
    }

    private String tabLabel(int index) {
        if (isAllIndex(index)) {
            return I18n.format("sanctionmode.tab.all");
        }
        if (isRequestsIndex(index)) {
            return I18n.format("sanctionmode.tab.requests") + " (" + ClientState.requestStacks().size() + ")";
        }
        CreativeTabs tab = tabAt(index);
        if (tab == null) {
            return "";
        }
        return I18n.format("itemGroup." + tab.getTabLabel());
    }

    private boolean isAllTab() {
        return isAllIndex(selected);
    }

    private boolean isRequestsTab() {
        return isRequestsIndex(selected);
    }

    private boolean isAllIndex(int index) {
        return index == 0;
    }

    private boolean isRequestsIndex(int index) {
        return kind == Kind.OPERATOR && index == tabCount() - 1;
    }

    private CreativeTabs currentTab() {
        return tabAt(selected);
    }

    private CreativeTabs tabAt(int index) {
        int tabIndex = index - 1;
        if (tabIndex < 0 || tabIndex >= tabs.size()) {
            return null;
        }
        return tabs.get(tabIndex);
    }

    private int tabCount() {
        return 1 + tabs.size() + (kind == Kind.OPERATOR ? 1 : 0);
    }

    private int maxScroll() {
        int rows = (view.size() + COLS - 1) / COLS;
        return Math.max(0, rows - ROWS);
    }

    private int maxTabScroll() {
        int visible = Math.max(1, PANEL_H / TAB_H);
        return Math.max(0, tabCount() - visible);
    }

    private int gridX() {
        return panelX + 8;
    }

    private int gridY() {
        return panelY + 36;
    }

    private int slotIndex(int mouseX, int mouseY) {
        int localX = mouseX - gridX();
        int localY = mouseY - gridY();
        if (localX < 0 || localY < 0) {
            return -1;
        }
        int col = localX / SLOT;
        int row = localY / SLOT;
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) {
            return -1;
        }
        if (localX % SLOT > 16 || localY % SLOT > 16) {
            return -1;
        }
        int index = (scroll + row) * COLS + col;
        if (index < 0 || index >= view.size()) {
            return -1;
        }
        return index;
    }

    private boolean overTab(int mouseX, int mouseY) {
        int x = panelX - TAB_W;
        return mouseX >= x && mouseX < x + TAB_W && mouseY >= panelY && mouseY < panelY + PANEL_H;
    }

    private boolean overScrollbar(int mouseX, int mouseY) {
        int x = gridX() + COLS * SLOT + 4;
        int y = gridY();
        return mouseX >= x && mouseX <= x + 8 && mouseY >= y && mouseY <= y + ROWS * SLOT;
    }

    private void updateScrollFromMouse(int mouseY) {
        int track = ROWS * SLOT;
        float relative = (mouseY - gridY()) / (float) track;
        if (relative < 0.0F) {
            relative = 0.0F;
        }
        if (relative > 1.0F) {
            relative = 1.0F;
        }
        scroll = Math.round(relative * maxScroll());
    }

    private void playClick() {
        mc.player.playSound(net.minecraft.init.SoundEvents.UI_BUTTON_CLICK, 0.6F, 1.0F);
    }
}
