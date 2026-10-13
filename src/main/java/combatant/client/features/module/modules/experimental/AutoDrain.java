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
import combatant.client.util.combat.AttackUtil;
import combatant.client.util.target.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

@ModuleInfo(id = "autodrain", displayName = "AutoDrain", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.ATTACK, experimental = true,
        description = "module.autodrain.description")
public final class AutoDrain extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private int retryTicks;
    private boolean attackWasDown;
    private boolean queuedAttack;

    @Override
    public void onEnable() { attackWasDown = false; queuedAttack = false; retryTicks = 0; }

    @EventHandler
    private void onTick(GameTickEvent event) {
        if (!isEnabled() || mc.player == null || mc.gameMode == null || mc.options == null) return;
        boolean attackDown = mc.options.keyAttack.isDown();
        if (attackDown && !attackWasDown) queuedAttack = true;
        attackWasDown = attackDown;
        if (!queuedAttack) return;
        if (retryTicks > 0) { retryTicks--; return; }
        LivingEntity target = TargetManager.getTarget(false);
        if (target == null || !target.isAlive() || target.isInvulnerable()
                || mc.player.distanceToSqr(target) > 16.0 * 16.0) return;
        if (AttackUtil.attack(mc, target)) {
            queuedAttack = false;
            retryTicks = 2;
        }
    }

    @Override
    public void onDisable() { attackWasDown = false; queuedAttack = false; retryTicks = 0; }
}
