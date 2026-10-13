/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.events.EventHandler;
import combatant.client.events.impl.EventSync;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.features.module.modules.combat.AutoAttack;
import combatant.client.features.module.Modules;
import net.minecraft.world.phys.HitResult;

@ModuleInfo(id = "nomissdelay", displayName = "NoMissDelay", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.LEGIT, experimental = true,
        description = "module.nomissdelay.description")
public final class NoMissDelay extends Module {
    @EventHandler
    private void onSync(EventSync event) {
        if (!isEnabled()) return;
        if (net.minecraft.client.Minecraft.getInstance().hitResult == null
                || net.minecraft.client.Minecraft.getInstance().hitResult.getType() != HitResult.Type.MISS) return;
        AutoAttack autoAttack = Modules.get(AutoAttack.class);
        if (autoAttack != null && autoAttack.isEnabled() && autoAttack.shouldBlockBreaking()) {
            autoAttack.triggerMissClick();
        }
    }
}
