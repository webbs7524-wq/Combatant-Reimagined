/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;

@ModuleInfo(id = "instantlunge", displayName = "InstantLunge", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.ATTACK, experimental = true,
        description = "module.instantlunge.description")
public final class InstantLunge extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private boolean wasUseDown;

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.options == null) return;
        boolean useDown = mc.options.keyUse.isDown();
        if (useDown && !wasUseDown && mc.player.getMainHandItem().is(ItemTags.SPEARS)) {
            mc.player.startAutoSpinAttack(20, 0.0f, mc.player.getMainHandItem());
        }
        wasUseDown = useDown;
    }

    @Override
    public void onDisable() { wasUseDown = false; }
}
