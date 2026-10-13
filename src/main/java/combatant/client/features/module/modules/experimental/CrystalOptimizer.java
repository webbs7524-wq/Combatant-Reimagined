/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.config.values.BooleanValue;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.mixins.accessors.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Places end crystals without waiting for the vanilla right-click delay.
 */
@ModuleInfo(id = "crystaloptimizer", displayName = "CrystalOptimizer", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.ATTACK, experimental = true,
        description = "module.crystaloptimizer.description")
public final class CrystalOptimizer extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final BooleanValue requireBlockTarget = bool("require_block_target", true);

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.options == null || mc.gameMode == null) return;
        if (!mc.options.keyUse.isDown()) return;
        if (!holdsCrystal(mc.player)) return;
        if (requireBlockTarget.get() && !(mc.hitResult instanceof BlockHitResult)) return;
        ((MinecraftAccessor) mc).combatant$setItemUseCooldown(0);
    }

    private static boolean holdsCrystal(LocalPlayer player) {
        return player.getMainHandItem().is(Items.END_CRYSTAL)
                || player.getOffhandItem().is(Items.END_CRYSTAL);
    }
}
