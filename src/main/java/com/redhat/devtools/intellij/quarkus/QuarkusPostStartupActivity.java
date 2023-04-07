/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.textmate.TextMateService;
import org.jetbrains.plugins.textmate.configuration.BundleConfigBean;
import org.jetbrains.plugins.textmate.configuration.TextMateSettings.TextMateSettingsState;
import org.jetbrains.plugins.textmate.configuration.TextMateSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

public class QuarkusPostStartupActivity implements StartupActivity, DumbAware {

    public static final String PLUGIN_ID = "com.redhat.devtools.intellij.quarkus";

    @Override
    public void runActivity(@NotNull Project project) {
        QuarkusProjectService.getInstance(project);
    }

    public static final String PLUGIN_PATH = Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.findId(PLUGIN_ID))).getPluginPath().toString();

    public static synchronized boolean registerBicepTextMateBundle() {
        final TextMateSettingsState state = TextMateSettings.getInstance().getState();
        try {
            if (Objects.nonNull(state)) {
                final Lock registrationLock = (Lock) FieldUtils.readField(TextMateService.getInstance(), "myRegistrationLock", true);
                try {
                    registrationLock.lock();
                    final Path bicepTextmatePath = Path.of(PLUGIN_PATH, "bicep", "textmate", "bicep");
                    final Path bicepParamTextmatePath = Path.of(PLUGIN_PATH, "bicep", "textmate", "bicepparam");
                    final Collection<BundleConfigBean> bundles = state.getBundles();
                    if (bicepTextmatePath.toFile().exists() && bundles.stream().noneMatch(b -> "bicep".equals(b.getName()) && b.isEnabled() && Path.of(b.getPath()).equals(bicepTextmatePath))) {
                        final ArrayList<BundleConfigBean> newBundles = new ArrayList<>(bundles);
                        newBundles.removeIf(bundle -> StringUtils.equalsAnyIgnoreCase(bundle.getName(), "bicep", "bicepparam"));
                        newBundles.add(new BundleConfigBean("bicep", bicepTextmatePath.toString(), true));
                        newBundles.add(new BundleConfigBean("bicepparam", bicepParamTextmatePath.toString(), true));
                        state.setBundles(newBundles);
                        return true;
                    }
                } finally {
                    registrationLock.unlock();
                }
            }
        } catch (final IllegalAccessException e) {
            //throw new SystemException("can not acquire lock of 'TextMateService'.", e);
        }
        return false;
    }

    //@AzureOperation("boundary/bicep.unregister_textmate_bundles")
    public static synchronized void unregisterBicepTextMateBundle() {
        final TextMateSettings.TextMateSettingsState state = TextMateSettings.getInstance().getState();
        if (Objects.nonNull(state)) {
            final Path bicepParamTextmatePath = Path.of(PLUGIN_PATH, "bicep", "textmate", "bicepparam");
            final Collection<BundleConfigBean> bundles = state.getBundles();
            if (bundles.stream().anyMatch(b -> "bicep".equals(b.getName()))) {
                final ArrayList<BundleConfigBean> newBundles = new ArrayList<>(bundles);
                newBundles.removeIf(bundle -> StringUtils.equalsAnyIgnoreCase(bundle.getName(), "bicep", "bicepparam"));
                state.setBundles(newBundles);
            }
        }
    }

}
