/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.gui.hud.draggable.impl;

import combatant.client.features.gui.clickgui.ClickGuiScreen;
import combatant.client.features.gui.hud.HudElementRegister;
import combatant.client.features.gui.hud.draggable.DraggableHudElement;
import combatant.client.render.engine.math.HudScale;
import combatant.client.render.engine.renderer.Renderer2D;
import combatant.client.render.engine.text.TextRenderer;
import combatant.client.util.screen.ClientScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Combatant logo pinned to the bottom-right corner; hidden while the ClickGUI is open.
 *
 * <p>This is an ordinary draggable HUD element. It is turned off and on like any other element,
 * with no password and no lock prompt: the enabled flag is read and written normally and persists
 * through the element's own config value.</p>
 */
@HudElementRegister(order = 90)
public final class CombatantLogo extends DraggableHudElement {
    /** Element id. */
    public static final String ID = "combatant_logo";
    private static final float BASE_SIZE = 170f;
    /** Width / height of logo.png. Keep in sync with the asset or the logo renders stretched. */
    private static final float LOGO_ASPECT = 1003f / 265f;
    private static final float BASE_MARGIN = 12f;
    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath("combatant", "textures/hud/elements/logo.png");

    public CombatantLogo() {
        super(ID, "Combatant Logo", "hud.draggable.combatant_logo.description", true);
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        float scale = HudScale.scale(screenW, screenH);
        float logoW = BASE_SIZE * scale;
        float logoH = logoW / LOGO_ASPECT;
        float margin = BASE_MARGIN * scale;
        this.x = screenW - logoW - margin;
        this.y = screenH - logoH - margin;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public void renderEngine(Renderer2D renderer,
                             TextRenderer textRenderer,
                             GuiGraphicsExtractor ctx,
                             float tickDelta,
                             int screenW,
                             int screenH) {
        if (ClientScreen.current() instanceof ClickGuiScreen) {
            width = 0f;
            height = 0f;
            return;
        }
        float logoW = BASE_SIZE * HudScale.scale(screenW, screenH);
        float logoH = logoW / LOGO_ASPECT;
        width = logoW;
        height = logoH;
        Renderer2D.TEXTURE.roundedTexRect(x, y, logoW, logoH, 0f, 1.0f, 0xFFFFFFFF, LOGO);
    }
}
