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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;

@ModuleInfo(id = "throwpot", displayName = "ThrowPot", category = ModuleCategory.PLAYER,
        subcategory = ModuleSubcategory.AUTOMATION, experimental = true,
        description = "module.throwpot.description")
public final class ThrowPot extends ItemMacro {
    public ThrowPot() {
        super(ThrowPot::isHealingPotion, InteractionHand.MAIN_HAND, false, "K",
                stack -> stack.is(Items.SPLASH_POTION));
    }

    private static boolean isHealingPotion(ItemStack stack) {
        if (!stack.is(Items.SPLASH_POTION)) return false;
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
