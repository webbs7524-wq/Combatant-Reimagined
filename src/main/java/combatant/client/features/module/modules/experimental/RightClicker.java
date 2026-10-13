/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.config.values.NumberValue;
import combatant.client.events.EventHandler;
import combatant.client.events.impl.GameTickEvent;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.mixins.accessors.MultiPlayerGameModeAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;

@ModuleInfo(id = "rightclicker", displayName = "RightClicker", category = ModuleCategory.PLAYER,
        subcategory = ModuleSubcategory.AUTOMATION, experimental = true,
        description = "module.rightclicker.description")
public final class RightClicker extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final NumberValue<Integer> intervalTicks = num("interval_ticks", 4, 1, 20);
    private int ticks;

    @EventHandler
    private void onTick(GameTickEvent event) {
        if (!isEnabled() || mc.player == null || mc.gameMode == null || mc.player.isUsingItem()
                || mc.options == null || !mc.options.keyUse.isDown() || mc.hitResult instanceof EntityHitResult) {
            ticks = 0;
            return;
        }
        if (++ticks < intervalTicks.get()) return;
        ticks = 0;
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
            InteractionHand hand = mc.player.getMainHandItem().isEmpty()
                    ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            mc.gameMode.useItemOn(mc.player, hand, hit);
        } else {
            if (mc.player.getMainHandItem().isEmpty() && mc.player.getOffhandItem().isEmpty()) return;
            mc.gameMode.useItem(mc.player, mc.player.getMainHandItem().isEmpty()
                    ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public void onDisable() { ticks = 0; }
}
