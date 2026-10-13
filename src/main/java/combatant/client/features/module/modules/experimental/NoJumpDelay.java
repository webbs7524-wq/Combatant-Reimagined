/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import net.minecraft.client.Minecraft;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.mixins.accessors.LivingEntityAccessor;

@ModuleInfo(id = "nojumpdelay", displayName = "NoJumpDelay", category = ModuleCategory.MOVEMENT,
        subcategory = ModuleSubcategory.BASIC, experimental = true,
        description = "module.nojumpdelay.description")
public final class NoJumpDelay extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.options == null || !mc.options.keyJump.isDown()) return;
        ((LivingEntityAccessor) mc.player).combatant$setJumpingCooldown(0);
    }
}
