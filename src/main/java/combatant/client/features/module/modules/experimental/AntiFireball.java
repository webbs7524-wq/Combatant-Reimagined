/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.events.EventHandler;
import combatant.client.events.impl.GameTickEvent;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

@ModuleInfo(id = "antifireball", displayName = "AntiFireball", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.PROTECT, experimental = true,
        description = "module.antifireball.description")
public final class AntiFireball extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private int delay;

    @EventHandler
    private void onTick(GameTickEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (delay > 0) { delay--; return; }
        for (Projectile projectile : mc.level.getEntitiesOfClass(Projectile.class,
                mc.player.getBoundingBox().inflate(3.0))) {
            if (projectile.getType() != EntityTypes.SMALL_FIREBALL || !projectile.isAlive()) continue;
            if (!mc.player.hasLineOfSight(projectile)) continue;
            if (projectile.position().distanceToSqr(mc.player.position()) > 9.0) continue;
            mc.gameMode.attack(mc.player, projectile);
            mc.player.swing(InteractionHand.MAIN_HAND);
            delay = 4;
            return;
        }
    }

    @Override
    public void onDisable() { delay = 0; }
}
