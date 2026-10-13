/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.util.player.inventory.InventorySwap;
import combatant.client.util.player.inventory.InventorySwapPolicy;

@ModuleInfo(id = "autototem", displayName = "AutoTotem", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.PROTECT,
        description = "module.autototem.description")
public final class AutoTotem extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null) return;
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        // Don't pull the shield out of the offhand while it is actively blocking.
        if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND) return;
        for (int slot = 0; slot < 36; slot++) {
            if (!mc.player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) continue;
            InventorySwap.INSTANCE.swapInventoryToOffhand(slot, InventorySwapPolicy.NONE, null, null);
            return;
        }
    }
}
