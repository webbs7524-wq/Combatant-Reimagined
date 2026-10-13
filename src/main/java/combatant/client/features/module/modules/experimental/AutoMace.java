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
import combatant.client.features.module.Modules;
import combatant.client.features.module.modules.combat.MaceKill;
import combatant.client.util.combat.MaceSmash;
import net.minecraft.client.Minecraft;

@ModuleInfo(id = "automace", displayName = "AutoMace", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.ATTACK,
        description = "module.automace.description")
public final class AutoMace extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    /**
     * Pre-tick on purpose. {@code onTick()} runs from {@code END_CLIENT_TICK}, behind the player's movement
     * packet of that tick, and Grim's Post check flags an attack in that reversed order; a vanilla click is
     * sent in front of it.
     */
    @EventHandler
    private void onGameTick(GameTickEvent event) {
        if (!isEnabled()) return;

        MaceKill maceKill = Modules.get(MaceKill.class);
        if (maceKill != null && maceKill.isEnabled()) return;

        MaceSmash.INSTANCE.strike(mc);
    }
}
