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

@ModuleInfo(id = "armorswitch", displayName = "ArmorSwitch", category = ModuleCategory.PLAYER,
        subcategory = ModuleSubcategory.AUTOMATION, experimental = true,
        description = "module.armorswitch.description")
public final class ArmorSwitch extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    ArmorSwitch() { addAction("switch", "Z"); }

    @Override
    public void onTick() {
        LocalPlayer player = mc.player;
        if (!isEnabled() || player == null || player.inventoryMenu == null || !isActionPressedOnce("switch")) return;
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) return;
        boolean wearingElytra = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .is(net.minecraft.world.item.Items.ELYTRA);
        Predicate<ItemStack> wanted = wearingElytra
                ? stack -> stack.is(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE)
                    || stack.is(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE)
                    || stack.is(net.minecraft.world.item.Items.IRON_CHESTPLATE)
                    || stack.is(net.minecraft.world.item.Items.GOLDEN_CHESTPLATE)
                    || stack.is(net.minecraft.world.item.Items.CHAINMAIL_CHESTPLATE)
                    || stack.is(net.minecraft.world.item.Items.LEATHER_CHESTPLATE)
                : stack -> stack.is(net.minecraft.world.item.Items.ELYTRA);
        int slot = -1;
        for (int i = 0; i < 36; i++) if (wanted.test(player.getInventory().getItem(i))) { slot = i; break; }
        if (slot < 0) return;
        int from = InventorySwap.mapInventoryToScreenSlot(slot);
        if (from >= 0) InventorySwap.INSTANCE.swapScreenSlots(from, 6, InventorySwapPolicy.NONE, this);
    }
}
