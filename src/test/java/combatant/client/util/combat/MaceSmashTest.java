package combatant.client.util.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import combatant.client.events.EventHandler;
import combatant.client.events.impl.GameTickEvent;
import combatant.client.features.module.modules.experimental.AutoMace;
import combatant.client.util.anticheat.AntiCheatPreset;

/**
 * The mace modules stay Grim-clean through two properties that are easy to break while editing:
 * the attack has to leave from the pre-tick hook, and the Grim preset must not forge movement packets.
 */
class MaceSmashTest {

    @Test
    void grimPresetForcesNoMovementPackets() {
        assertFalse(MaceSmash.forgesMovementPackets(AntiCheatPreset.GRIM));
        assertTrue(MaceSmash.forgesMovementPackets(AntiCheatPreset.CUSTOM));
        assertTrue(MaceSmash.forgesMovementPackets(AntiCheatPreset.VANILLA));
    }

    @Test
    void autoMaceNeverAttacksFromOnTick() {
        assertThrows(NoSuchMethodException.class, () -> AutoMace.class.getDeclaredMethod("onTick"),
                "onTick runs after the tick's movement packet, which is the order Grim's Post check flags");
    }

    @Test
    void autoMaceStrikesFromThePreTickHook() throws Exception {
        Method handler = AutoMace.class.getDeclaredMethod("onGameTick", GameTickEvent.class);
        assertTrue(handler.isAnnotationPresent(EventHandler.class));
    }
}
