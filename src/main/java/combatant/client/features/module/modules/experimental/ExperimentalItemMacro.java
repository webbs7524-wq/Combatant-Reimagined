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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.util.function.Predicate;

abstract class ExperimentalItemMacro extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final Predicate<ItemStack> item;

    ExperimentalItemMacro(Predicate<ItemStack> item) {
        this(item, "NONE");
    }

    ExperimentalItemMacro(Predicate<ItemStack> item, String defaultKey) {
        this.item = item;
        // A real default combo is required: KeyManager never registers an unbound ("NONE") action.
        addAction("use", defaultKey == null || defaultKey.isBlank() ? "NONE" : defaultKey);
    }

    @Override
    public void onTick() {
        LocalPlayer player = mc.player;
        if (!isEnabled() || player == null || mc.level == null || mc.gameMode == null
                || ClientScreen.current() != null) return;
        // Macros without a self-condition need a bound key; the rest run on their own condition.
        if (requiresKeyPress() && !isActionPressedOnce("use")) return;
        if (!canUse(player)) return;
        if (this instanceof AutoPearlCatch && mc.hitResult instanceof EntityHitResult) return;
        LocalPlayer expectedPlayer = player;
        InventorySwapRequest request = InventorySwapRequest.builder(item, () -> {
                    if (mc.player != expectedPlayer || mc.gameMode == null || ClientScreen.current() != null) return;
                    var result = InventorySwap.INSTANCE.useItem(InteractionHand.MAIN_HAND);
                    if (result != null && result.consumesAction()) expectedPlayer.swing(InteractionHand.MAIN_HAND);
                })
                .scope(InventorySearchScope.FULL)
                .visibility(InventorySwapVisibility.SILENT)
                .restore(true)
                .policy(InventorySwapPolicy.NONE)
                .actionKind(InventoryActionKind.USE_ITEM)
                .build();
        InventorySwap.INSTANCE.execute(request);
    }

    protected boolean canUse(LocalPlayer player) {
        return true;
    }

    /** False for macros whose description makes them automatic, such as AutoPearl. */
    protected boolean requiresKeyPress() {
        return true;
    }
}
