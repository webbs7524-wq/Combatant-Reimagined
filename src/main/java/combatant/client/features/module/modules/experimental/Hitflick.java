/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;


import combatant.client.config.values.NumberValue;
import combatant.client.events.EventHandler;
import combatant.client.events.impl.AttackEntityEvent;
import combatant.client.events.impl.EventSync;
import combatant.client.events.impl.PacketEvent;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.util.aiming.RotationManager;
import combatant.client.util.aiming.RotationTarget;
import combatant.client.util.aiming.data.Rotation;
import combatant.client.util.aiming.features.MovementCorrection;
import combatant.client.util.player.inventory.InventoryActionKind;
import combatant.client.util.player.inventory.InventorySearchScope;
import combatant.client.util.player.inventory.InventorySwap;
import combatant.client.util.player.inventory.InventorySwapPolicy;
import combatant.client.util.player.inventory.InventorySwapRequest;
import combatant.client.util.player.inventory.InventorySwapVisibility;
import combatant.client.util.target.TargetingUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import java.util.function.Predicate;

@ModuleInfo(id = "hitflick", displayName = "Hitflick", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.LEGIT, experimental = true,
        description = "module.hitflick.description")
public final class Hitflick extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final NumberValue<Integer> flickTicks = num("flick_ticks", 1, 1, 4);
    private int remainingTicks;
    private Rotation flickRotation;

    @Override
    public void onDisable() { remainingTicks = 0; flickRotation = null; }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null) { remainingTicks = 0; flickRotation = null; return; }
        if (remainingTicks > 0 && --remainingTicks == 0) flickRotation = null;
        if (mc.options.keyAttack.isDown() && mc.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity target && TargetingUtil.isValidCombatTarget(target)) {
            flickRotation = Rotation.lookingAt(target.getEyePosition(), mc.player.getEyePosition()).normalize();
            remainingTicks = flickTicks.get();
        }
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (!isEnabled() || mc.player == null || event.getPlayer() != mc.player
                || !(event.getTarget() instanceof LivingEntity target)
                || !TargetingUtil.isValidCombatTarget(target)) return;
        flickRotation = Rotation.lookingAt(target.getEyePosition(), mc.player.getEyePosition()).normalize();
        remainingTicks = flickTicks.get();
    }

    @EventHandler(priority = 110)
    private void onSync(EventSync event) {
        if (isEnabled() && remainingTicks > 0 && flickRotation != null) {
            event.setRotation(flickRotation.yaw(), flickRotation.pitch(), true);
        }
    }
}

