/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.config.values.NumberValue;
import combatant.client.events.EventHandler;
import combatant.client.events.impl.PacketEvent;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

@ModuleInfo(id = "autojumpreset", displayName = "AutoJumpReset",
        category = ModuleCategory.MOVEMENT, subcategory = ModuleSubcategory.BASIC, experimental = true,
        description = "module.autojumpreset.description")
public final class AutoJumpReset extends Module {
    private final NumberValue<Integer> resetWindowTicks = num("reset_window_ticks", 3, 1, 6);
    private int resetWindow;
    private boolean wasJumpDown;
    private final Minecraft mc = Minecraft.getInstance();
    private boolean motionReceived;

    @EventHandler
    private void onMotion(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet
                && mc.player != null && packet.id() == mc.player.getId()) {
            motionReceived = true;
            resetWindow = resetWindowTicks.get();
        }
    }

    @Override
    public void onTick() {
        LocalPlayer player = mc.player;
        if (!isEnabled() || player == null || player.input == null || player.input.keyPresses == null) return;
        if (motionReceived) motionReceived = false;
        boolean jumpDown = player.input.keyPresses.jump();
        if (resetWindow > 0 && player.onGround() && player.hurtTime > 0 && jumpDown && !wasJumpDown) {
            player.jumpFromGround();
            resetWindow = 0;
        } else if (resetWindow > 0) {
            resetWindow--;
        }
        wasJumpDown = jumpDown;
    }

    @Override
    public void onDisable() { motionReceived = false; resetWindow = 0; wasJumpDown = false; }
}

