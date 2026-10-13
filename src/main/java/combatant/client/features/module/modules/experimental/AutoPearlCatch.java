/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.config.values.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

@ModuleInfo(id = "autopearlcatch", displayName = "AutoPearlCatch", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.LEGIT, experimental = true,
        description = "module.autopearlcatch.description")
public final class AutoPearlCatch extends ExperimentalItemMacro {
    private final Minecraft mc = Minecraft.getInstance();
    private final NumberValue<Float> minimumFallDistance = num("minimum_fall_distance", 8.0f, 3.0f, 24.0f);

    public AutoPearlCatch() { super(stack -> stack.is(Items.ENDER_PEARL), "H"); }

    @Override
    protected boolean canUse(net.minecraft.client.player.LocalPlayer player) {
        if (player.onGround() || player.getDeltaMovement().y >= -0.25) return false;
        return mc.level != null && mc.level.getMinY() < player.getY() - minimumFallDistance.get();
    }
}
