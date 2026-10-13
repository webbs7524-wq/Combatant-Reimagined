/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.command.impl;

import combatant.client.features.command.ClientCommand;
import combatant.client.features.command.CommandContext;
import combatant.client.features.command.CommandInfo;
import combatant.client.features.command.CommandOutput;
import combatant.client.runtime.ClientRuntime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@CommandInfo(
        id = "combatantoff",
        aliases = {"combatantoff"},
        usage = "@combatantoff <password>",
        descriptionKey = "command.combatantoff.description"
)
public final class CombatantOffCommand implements ClientCommand {
    /** SHA-256 of the unlock password; the plaintext password is never stored in the client. */
    private static final String PASSWORD_SHA256 =
            "8f7f77ec31398848f8398388b83a358e79b8c735acf1dae00aaf21088eaad4d2";

    @Override
    public boolean execute(CommandContext ctx) {
        String attempt = ctx.arg(0);
        if (attempt == null || !matchesPassword(attempt)) {
            CommandOutput.send("Wrong password.");
            return true;
        }
        ClientRuntime.enterSoftPanic("combatantoff command");
        CommandOutput.send("Combatant client turned off. Use @runtime resume to turn it back on.");
        return true;
    }

    public static boolean matchesPassword(String attempt) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(attempt.getBytes(StandardCharsets.UTF_8));
            byte[] expected = HexFormat.of().parseHex(PASSWORD_SHA256);
            return MessageDigest.isEqual(digest, expected);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
