/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package combatant.client.mixins;

import combatant.client.util.media.LocalMediaPlayerService;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Minecraft background music stopped while the local media player plays; sound effects are untouched. */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
    @Shadow public abstract void stopPlaying();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void combatant$muteWhileLocalMediaPlays(CallbackInfo ci) {
        if (!LocalMediaPlayerService.isPlaying()) return;
        this.stopPlaying();
        ci.cancel();
    }
}
