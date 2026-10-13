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
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;

@ModuleInfo(id = "autohitcrystal", displayName = "AutoHitCrystal", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.ATTACK, experimental = true,
        description = "module.autohitcrystal.description")
public final class AutoHitCrystal extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final NumberValue<Integer> range = num("range", 4, 1, 6);
    private int delay;

    @EventHandler
    private void onTick(GameTickEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (delay > 0) { delay--; return; }
        double reach = range.get();
        AABB area = mc.player.getBoundingBox().inflate(reach);
        EndCrystal crystal = mc.level.getEntitiesOfClass(EndCrystal.class, area,
                        candidate -> candidate.isAlive() && mc.player.hasLineOfSight(candidate))
                .stream().min(java.util.Comparator.comparingDouble(mc.player::distanceToSqr)).orElse(null);
        if (crystal == null || mc.player.distanceToSqr(crystal) > reach * reach) return;
        mc.gameMode.attack(mc.player, crystal);
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        delay = 2;
    }

    @Override
    public void onDisable() { delay = 0; }
}
