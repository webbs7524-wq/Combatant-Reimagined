/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.config.values.NumberValue;
import combatant.client.events.EventHandler;
import combatant.client.events.impl.GameTickEvent;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.features.module.Modules;
import combatant.client.features.module.modules.combat.AutoAnchor;
import net.minecraft.client.Minecraft;

@ModuleInfo(id = "safeanchor", displayName = "SafeAnchor", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.PROTECT, experimental = true,
        description = "module.safeanchor.description")
public final class SafeAnchor extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final NumberValue<Float> minimumHealth = num("minimum_health", 8.0f, 1.0f, 36.0f);

    @EventHandler
    private void onTick(GameTickEvent event) {
        if (!isEnabled() || mc.player == null) return;
        AutoAnchor anchor = Modules.get(AutoAnchor.class);
        if (anchor == null) return;
        boolean safe = mc.player.getHealth() + mc.player.getAbsorptionAmount() > minimumHealth.get();
        if (anchor.isEnabled() != safe) anchor.setEnabled(safe);
    }

    @Override
    public void onDisable() {
        AutoAnchor anchor = Modules.get(AutoAnchor.class);
        if (anchor != null) anchor.setEnabled(false);
    }
}
