/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.features.module.Modules;
import combatant.client.features.module.modules.combat.AutoAnchor;

@ModuleInfo(id = "doubleanchor", displayName = "DoubleAnchor", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.ATTACK, experimental = true,
        description = "module.doubleanchor.description")
public final class DoubleAnchor extends Module {
    @Override
    public void onEnable() {
        AutoAnchor anchor = Modules.get(AutoAnchor.class);
        if (anchor != null) anchor.setEnabled(true);
    }

    @Override
    public void onDisable() {
        AutoAnchor anchor = Modules.get(AutoAnchor.class);
        if (anchor != null) anchor.setEnabled(false);
    }
}
