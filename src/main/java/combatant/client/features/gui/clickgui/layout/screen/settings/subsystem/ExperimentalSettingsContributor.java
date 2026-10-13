/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.gui.clickgui.layout.screen.settings.subsystem;

import combatant.client.config.SettingDef;
import combatant.client.config.SettingOwner;
import combatant.client.config.subsystem.RuntimeConfig;
import combatant.client.config.values.BooleanValue;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main Settings group for experimental modules. The master switch gates everything: experimental
 * modules can only be enabled while it is on, and the per-module rows only appear when it is on.
 */
public final class ExperimentalSettingsContributor implements MainSettingsContributor {
    private static final Map<Module, BooleanValue> MODULE_TOGGLES = new IdentityHashMap<>();

    @Override public String id() { return "experimental"; }
    @Override public String titleKey() { return "clickgui.settings.main.experimental"; }
    @Override public String fallbackTitle() { return "Experimental"; }
    @Override public String icon() { return "settings-2"; }
    @Override public int order() { return 350; }
    @Override public SettingOwner owner() { return RuntimeConfig.get(); }

    @Override
    public List<SettingDef> settingDefs() {
        RuntimeConfig config = RuntimeConfig.get();
        List<SettingDef> defs = new ArrayList<>();
        defs.add(SettingDef.bool(config.experimentalModulesValue()));

        List<Module> experimental = new ArrayList<>();
        for (Module module : ModuleManager.getModules()) {
            if (module != null && module.isExperimental()) experimental.add(module);
        }
        experimental.sort(Comparator.comparing(Module::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        for (Module module : experimental) {
            defs.add(SettingDef.bool(module.name(), toggleFor(module))
                    .visibleWhen(config::isExperimentalModulesEnabled));
        }
        return List.copyOf(defs);
    }

    /** Mirrors the module's own enabled state so the row and the module can never disagree. */
    private static synchronized BooleanValue toggleFor(Module module) {
        return MODULE_TOGGLES.computeIfAbsent(module, m -> new BooleanValue(m.name(), m.isEnabled()) {
            @Override
            public Boolean get() {
                return m.isEnabled();
            }

            @Override
            public void set(Boolean enabled) {
                m.setEnabled(Boolean.TRUE.equals(enabled));
            }
        });
    }
}
