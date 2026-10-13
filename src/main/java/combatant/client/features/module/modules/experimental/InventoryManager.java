/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.config.values.NumberValue;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.util.player.inventory.InventorySwap;
import combatant.client.util.player.inventory.InventorySwapPolicy;
import combatant.client.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.BlockItem;

@ModuleInfo(id = "inventorymanager", displayName = "InventoryManager", category = ModuleCategory.PLAYER,
        subcategory = ModuleSubcategory.AUTOMATION, experimental = true,
        description = "module.inventorymanager.description")
public final class InventoryManager extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final NumberValue<Integer> blockHotbarSlot = num("block_hotbar_slot", 7, 1, 9);
    private int delay;

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.player.inventoryMenu == null
                || !(ClientScreen.current() instanceof InventoryScreen)) return;
        if (delay > 0) { delay--; return; }
        int hotbar = blockHotbarSlot.get() - 1;
        if (!mc.player.getInventory().getItem(hotbar).isEmpty()) return;
        for (int slot = 9; slot < 36; slot++) {
            if (!(mc.player.getInventory().getItem(slot).getItem() instanceof BlockItem)) continue;
            int from = InventorySwap.mapInventoryToScreenSlot(slot);
            int to = InventorySwap.mapHotbarToScreenSlot(hotbar);
            if (from >= 0 && to >= 0 && InventorySwap.INSTANCE.swapScreenSlots(from, to,
                    InventorySwapPolicy.NONE, this)) delay = 4;
            return;
        }
    }

    @Override
    public void onDisable() { delay = 0; }
}
