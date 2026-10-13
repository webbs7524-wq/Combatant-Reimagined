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
import combatant.client.util.aiming.data.Rotation;
import combatant.client.util.target.TargetManager;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

@ModuleInfo(id = "autopearl", displayName = "AutoPearl", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.LEGIT, experimental = true,
        description = "module.autopearl.description")
public final class AutoPearl extends ExperimentalItemMacro {
    public AutoPearl() { super(stack -> stack.is(Items.ENDER_PEARL)); }

    /**
     * Automatic by design: it throws as soon as the crosshair is exactly on the selected target,
     * so no keybind is needed.
     */
    @Override
    protected boolean requiresKeyPress() {
        return false;
    }

    @Override
    protected boolean canUse(LocalPlayer player) {
        LivingEntity target = TargetManager.getTarget(false);
        if (target == null || !target.isAlive()) return false;
        Rotation rotation = Rotation.lookingAt(target.getEyePosition(), player.getEyePosition()).normalize();
        Vec3 direction = player.getLookAngle();
        float yawDiff = Math.abs(net.minecraft.util.Mth.wrapDegrees(rotation.yaw() - player.getYRot()));
        float pitchDiff = Math.abs(rotation.pitch() - player.getXRot());
        return yawDiff <= 3.0f && pitchDiff <= 3.0f && direction.dot(target.getEyePosition()
                .subtract(player.getEyePosition()).normalize()) > 0.995;
    }
}
