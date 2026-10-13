/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.combat;

import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;

@ModuleInfo(id = "swaphit", displayName = "SwapHit", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.LEGIT, experimental = true,
        description = "module.swaphit.description")
public final class SwapHit extends AutoAttack {
}
