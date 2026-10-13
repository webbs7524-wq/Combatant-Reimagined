/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.util.player.inventory.InventorySwap;
import combatant.client.util.player.inventory.InventorySwapPolicy;
import combatant.client.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

@ModuleInfo(id = "inventoryfill", displayName = "InventoryFill", category = ModuleCategory.PLAYER,
        subcategory = ModuleSubcategory.AUTOMATION, experimental = true,
        description = "module.inventoryfill.description")
public final class InventoryFill extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private int delay;

    @Override
    public void onTick() {
        LocalPlayer player = mc.player;
        if (!isEnabled() || player == null || player.inventoryMenu == null
                || !(ClientScreen.current() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        if (delay > 0) { delay--; return; }
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            if (!player.getInventory().getItem(hotbar).isEmpty()) continue;
            for (int inv = 9; inv < 36; inv++) {
                if (player.getInventory().getItem(inv).isEmpty()) continue;
                int from = InventorySwap.mapInventoryToScreenSlot(inv);
                int to = InventorySwap.mapHotbarToScreenSlot(hotbar);
                if (from >= 0 && to >= 0 && InventorySwap.INSTANCE.swapScreenSlots(from, to,
                        InventorySwapPolicy.NONE, this)) delay = 4;
                return;
            }
        }
    }

    @Override
    public void onDisable() { delay = 0; }
}

