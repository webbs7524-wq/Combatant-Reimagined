/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.gui.clickgui.sections;

import combatant.client.features.gui.clickgui.ClickGuiRenderer;
import combatant.client.features.gui.clickgui.ClickGuiSearch;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleManager;
import combatant.client.features.theme.Theme;
import combatant.client.render.engine.renderer.Renderer2D;
import combatant.client.render.engine.text.BuiltinFontCatalog;
import combatant.client.render.engine.text.TextRenderer;
import combatant.client.util.text.SingleLineTextInput;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Search tab: type to filter every registered module and click a result to toggle it.
 */
@ClickGuiSectionInfo(id = "combatant:search", label = "Search", order = 290)
public final class SearchSection implements ClickGuiSection {
    private static final int ROWS_PER_PAGE = 10;
    private static final float ROW_H = 34f;
    private static final float FIELD_H = 34f;

    private final SingleLineTextInput query = new SingleLineTextInput(48);
    private final List<Module> results = new ArrayList<>();

    private float x, y, w, h;
    private float fieldX, fieldY, fieldW;
    private float listX, listY, listW, listH;
    private int scroll;
    private boolean focused;
    private String lastQuery = null;

    @Override
    public void layout(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        fieldX = x + 28f;
        fieldY = y + 74f;
        fieldW = Math.max(220f, Math.min(w - 56f, 420f));
        listX = x + 28f;
        listY = fieldY + FIELD_H + 18f;
        listW = Math.max(240f, w - 56f);
        listH = Math.max(90f, y + h - 18f - listY);
    }

    @Override
    public void onSelected() {
        refresh(true);
    }

    @Override
    public void onDeselected() {
        focused = false;
        query.unfocus();
        // Release the global "typing" state so module keybinds work again.
        ClickGuiSearch.unfocus();
    }

    @Override
    public void render(float mouseX, float mouseY) {
        Renderer2D r = ClickGuiRenderer.currentRenderer();
        if (r == null) return;

        int text = Theme.theme().textPrimary();
        int muted = Theme.theme().textMuted();
        int accent = Theme.theme().accent();
        TextRenderer medium = BuiltinFontCatalog.ONEST_MEDIUM.renderer(ClickGuiRenderer.getInterRegular());
        TextRenderer regular = BuiltinFontCatalog.ONEST_REGULAR.renderer(ClickGuiRenderer.getInterRegular());

        r.roundedRect(x + 12f, y + 8f, w - 24f, h - 16f, 16f, 0xD91A1D24);
        ClickGuiRenderer.drawText(medium, "Search", listX, y + 28f, 21f, text, false);
        ClickGuiRenderer.drawText(regular, "Find any module by name, id or category", listX, y + 53f, 11f, muted, false);

        renderField(r, regular, text, muted, accent);

        refresh(false);

        r.roundedRect(listX, listY, listW, listH, 10f, 0x9923262E);

        int visible = visibleRows();
        int maxScroll = Math.max(0, results.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (results.isEmpty()) {
            String message = query.text().isBlank() ? "No modules registered." : "No modules match your search.";
            ClickGuiRenderer.drawText(regular, message, listX + 14f, listY + 24f, 12f, muted, false);
            return;
        }

        int start = scroll;
        for (int i = start; i < Math.min(results.size(), start + visible); i++) {
            Module module = results.get(i);
            float ry = listY + 6f + (i - start) * ROW_H;
            boolean hover = inside(mouseX, mouseY, listX + 4f, ry, listW - 8f, ROW_H - 2f);
            if (hover) r.roundedRect(listX + 4f, ry, listW - 8f, ROW_H - 2f, 7f, 0x553F4654);

            boolean enabled = module.isEnabled();
            int nameColor = enabled ? accent : text;
            String label = ClickGuiRenderer.fitText(medium, module.getDisplayName(), 13f, listW - 120f);
            ClickGuiRenderer.drawText(medium, label, listX + 14f, ry + 7f, 13f, nameColor, false);

            String meta = module.name() + "  •  " + module.getCategory().name().toLowerCase(Locale.ROOT);
            ClickGuiRenderer.drawText(regular, ClickGuiRenderer.fitText(regular, meta, 9f, listW - 40f),
                    listX + 14f, ry + 22f, 9f, muted, false);

            float pillW = 46f;
            float pillX = listX + listW - pillW - 12f;
            r.roundedRect(pillX, ry + 9f, pillW, 16f, 8f, enabled ? accent : 0xFF343A45);
            ClickGuiRenderer.drawText(regular, enabled ? "ON" : "OFF", pillX + 13f, ry + 12f, 9f,
                    enabled ? 0xFF14161B : muted, false);
        }

        String footer = results.size() + (results.size() == 1 ? " result" : " results")
                + "  •  click to toggle  •  Esc clears";
        ClickGuiRenderer.drawText(regular, ClickGuiRenderer.fitText(regular, footer, 9f, listW - 20f),
                listX + 9f, listY + listH + 3f, 9f, muted, false);
    }

    private void renderField(Renderer2D r, TextRenderer regular, int text, int muted, int accent) {
        r.roundedRect(fieldX, fieldY, fieldW, FIELD_H, 8f, focused ? 0xCC2C313B : 0x9923262E);
        r.roundedRect(fieldX, fieldY, 2.5f, FIELD_H, 1.25f, focused ? accent : 0x66464B56);
        String value = query.text();
        float textY = fieldY + (FIELD_H - 13f) * 0.5f;
        if (value.isEmpty() && !focused) {
            ClickGuiRenderer.drawText(regular, "Search modules...", fieldX + 12f, textY, 12f, muted, false);
            return;
        }
        query.render(fieldX + 12f, textY, fieldW - 24f, FIELD_H, regular, 12f,
                text, 0x805D8CFF, focused);
    }

    /**
     * Recomputes the result list when the query changed (or when forced on tab select).
     */
    private void refresh(boolean force) {
        String current = query.text();
        if (!force && lastQuery != null && lastQuery.equals(current)) return;
        lastQuery = current;

        String needle = compact(current);
        List<Module> matches = new ArrayList<>();
        for (Module module : ModuleManager.getModules()) {
            if (module == null) continue;
            if (needle.isEmpty() || matchesQuery(module, needle)) matches.add(module);
        }
        matches.sort(Comparator.comparing(module -> module.getDisplayName().toLowerCase(Locale.ROOT)));
        results.clear();
        results.addAll(matches);
        scroll = 0;
    }

    private static boolean matchesQuery(Module module, String needle) {
        String displayName = module.getDisplayName();
        String id = module.name();
        String category = module.getCategory() == null ? "" : module.getCategory().name();
        String description = module.getDescription();
        return compact(displayName).contains(needle)
                || compact(id).contains(needle)
                || compact(category).contains(needle)
                || compact(description).contains(needle);
    }

    private static String compact(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }

    private int visibleRows() {
        return Math.max(1, Math.min(ROWS_PER_PAGE, (int) ((listH - 10f) / ROW_H)));
    }

    @Override
    public boolean mousePressed(float mouseX, float mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        if (inside(mouseX, mouseY, fieldX, fieldY, fieldW, FIELD_H)) {
            focused = true;
            // KeyManager already ignores keybinds while the ClickGUI search is active, so typing
            // here cannot fire the module macros that now have default keys.
            ClickGuiSearch.setActive(true);
            query.layout(fieldX + 12f, fieldW - 24f, BuiltinFontCatalog.ONEST_REGULAR.renderer(
                    ClickGuiRenderer.getInterRegular()), 12f);
            query.beginDrag(mouseX, false);
            return true;
        }
        focused = false;
        query.unfocus();
        ClickGuiSearch.unfocus();

        if (!inside(mouseX, mouseY, listX, listY, listW, listH)) return false;
        int visible = visibleRows();
        int start = Math.max(0, Math.min(scroll, Math.max(0, results.size() - visible)));
        for (int i = start; i < Math.min(results.size(), start + visible); i++) {
            float ry = listY + 6f + (i - start) * ROW_H;
            if (inside(mouseX, mouseY, listX + 4f, ry, listW - 8f, ROW_H - 2f)) {
                Module module = results.get(i);
                if (module != null) module.toggle();
                return true;
            }
        }
        return true;
    }

    @Override
    public void mouseMoved(float mouseX, float mouseY) {
        if (focused) SingleLineTextInput.mouseMoved(mouseX);
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        SingleLineTextInput.mouseReleased();
    }

    @Override
    public boolean mouseScrolled(float mouseX, float mouseY, double amount) {
        if (!inside(mouseX, mouseY, listX, listY, listW, listH)) return false;
        int maxScroll = Math.max(0, results.size() - visibleRows());
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount) * 3));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && (focused || !query.text().isEmpty())) {
            query.clear();
            focused = false;
            query.unfocus();
            ClickGuiSearch.unfocus();
            refresh(true);
            return true;
        }
        if (!focused) return false;
        query.keyPressed(keyCode, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;
        query.type(chr);
        return true;
    }

    private static boolean inside(float px, float py, float bx, float by, float bw, float bh) {
        return px >= bx && px <= bx + bw && py >= by && py <= by + bh;
    }
}
