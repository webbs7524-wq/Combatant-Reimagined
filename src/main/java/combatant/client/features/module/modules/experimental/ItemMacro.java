/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.features.module.Module;
import combatant.client.util.player.inventory.InventoryActionKind;
import combatant.client.util.player.inventory.InventorySearchScope;
import combatant.client.util.player.inventory.InventorySwap;
import combatant.client.util.player.inventory.InventorySwapPolicy;
import combatant.client.util.player.inventory.InventorySwapRequest;
import combatant.client.util.player.inventory.InventorySwapVisibility;
import combatant.client.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.function.Predicate;

/** Shared implementation for explicitly bound item-use macros. */
abstract class ItemMacro extends Module {
    final Minecraft mc = Minecraft.getInstance();
    private final InteractionHand preferredHand;
    private final boolean requiresFallFlying;
    private final Predicate<ItemStack> handPredicate;
    private final Predicate<ItemStack> inventoryPredicate;

    ItemMacro(Predicate<ItemStack> item, InteractionHand preferredHand, boolean requiresFallFlying,
              String defaultKey) {
        this(item, preferredHand, requiresFallFlying, defaultKey, item, item);
    }

    ItemMacro(Predicate<ItemStack> item, InteractionHand preferredHand, boolean requiresFallFlying,
              String defaultKey, Predicate<ItemStack> inventoryPredicate) {
        this(item, preferredHand, requiresFallFlying, defaultKey, item, inventoryPredicate);
    }

    private ItemMacro(Predicate<ItemStack> item, InteractionHand preferredHand, boolean requiresFallFlying,
                      String defaultKey, Predicate<ItemStack> handPredicate,
                      Predicate<ItemStack> inventoryPredicate) {
        this.preferredHand = preferredHand;
        this.requiresFallFlying = requiresFallFlying;
        this.handPredicate = handPredicate;
        this.inventoryPredicate = inventoryPredicate;
        // A real default combo is required: KeyManager never registers an unbound ("NONE") action,
        // so a NONE default makes the macro permanently dead instead of merely unbound.
        addAction("use", defaultKey == null || defaultKey.isBlank() ? "NONE" : defaultKey);
    }

    @Override
    public void onTick() {
        LocalPlayer player = mc.player;
        if (!isEnabled() || player == null || mc.level == null || mc.gameMode == null
                || ClientScreen.current() != null || !isActionPressedOnce("use")
                || requiresFallFlying && !player.isFallFlying()) return;
        if (this instanceof ThrowPot && !hasHealingPotion(player)) return;

        InteractionHand hand = preferredHand;
        if (!handPredicate.test(hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem())) {
            InteractionHand alternate = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (handPredicate.test(alternate == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem())) {
                hand = alternate;
            }
        }
        InteractionHand selectedHand = hand;
        LocalPlayer expectedPlayer = player;
        InventorySwapRequest request = InventorySwapRequest.builder(inventoryPredicate, () -> {
                    if (mc.player != expectedPlayer || mc.gameMode == null || ClientScreen.current() != null) return;
                    InteractionResult result = InventorySwap.INSTANCE.useItem(selectedHand);
                    if (result != null && result.consumesAction()) expectedPlayer.swing(selectedHand);
                })
                .scope(InventorySearchScope.FULL)
                .visibility(InventorySwapVisibility.SILENT)
                .restore(true)
                .policy(InventorySwapPolicy.NONE)
                .actionKind(InventoryActionKind.USE_ITEM)
                .build();
        InventorySwap.INSTANCE.execute(request);
    }

    private boolean hasHealingPotion(LocalPlayer player) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isHealingPotion(stack)) return true;
        }
        return isHealingPotion(player.getMainHandItem()) || isHealingPotion(player.getOffhandItem());
    }

    private boolean isHealingPotion(ItemStack stack) {
        if (!stack.is(net.minecraft.world.item.Items.SPLASH_POTION)) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) return false;
        for (var effect : contents.potion().get().value().getEffects()) {
            var type = effect.getEffect();
            if (type.equals(net.minecraft.world.effect.MobEffects.INSTANT_HEALTH)
                    || type.equals(net.minecraft.world.effect.MobEffects.REGENERATION)) return true;
        }
        return false;
    }
}
