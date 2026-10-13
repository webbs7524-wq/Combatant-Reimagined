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
import combatant.client.mixins.accessors.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;

@ModuleInfo(id = "fastplace", displayName = "FastPlace", category = ModuleCategory.PLAYER,
        subcategory = ModuleSubcategory.EXPLOIT, experimental = true,
        description = "module.fastplace.description")
public final class FastPlace extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.options == null || mc.gameMode == null
                || !mc.options.keyUse.isDown() || !(mc.hitResult instanceof BlockHitResult)) return;
        boolean blockInHand = mc.player.getMainHandItem().getItem() instanceof BlockItem
                || mc.player.getOffhandItem().getItem() instanceof BlockItem;
        if (blockInHand) ((MinecraftAccessor) mc).combatant$setItemUseCooldown(0);
    }
}
