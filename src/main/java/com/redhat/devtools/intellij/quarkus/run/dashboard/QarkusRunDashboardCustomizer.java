/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run.dashboard;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.dashboard.RunDashboardCustomizer;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Dashboard customizer for Quarkus to provide:
 *
 * <ul>
 *     <li>Open quarkus application in a browser.</li>
 *     <li>Open quarkus DevUI in a browser.</li>
 * </ul>
 */
public class QarkusRunDashboardCustomizer extends RunDashboardCustomizer {

    @Override
    public boolean isApplicable(@NotNull RunnerAndConfigurationSettings settings, @Nullable RunContentDescriptor descriptor) {
        return settings.getConfiguration() instanceof QuarkusRunConfiguration;
    }

    @Override
    public boolean updatePresentation(@NotNull PresentationData presentation, @NotNull RunDashboardRunConfigurationNode node) {
        if (!(node.getConfigurationSettings().getConfiguration() instanceof QuarkusRunConfiguration)) {
            return false;
        }
        node.putUserData(RunDashboardCustomizer.NODE_LINKS, null);
        RunContentDescriptor descriptor = node.getDescriptor();
        if (descriptor != null) {
            ProcessHandler processHandler =  descriptor.getProcessHandler();
            if (processHandler != null && !processHandler.isProcessTerminated()) {
                // The Quarkus run configuration is running, add links for:
                // - Opening quarkus application in a browser
                // - Opening DevUI in a browser
                QuarkusRunConfiguration quarkusRunConfiguration = (QuarkusRunConfiguration) node.getConfigurationSettings().getConfiguration();
                QuarkusRunContext runContext = new QuarkusRunContext(quarkusRunConfiguration.getModule());

                final String applicationUrl = runContext.getApplicationURL();
                presentation.addText(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                presentation.addText(applicationUrl, SimpleTextAttributes.LINK_ATTRIBUTES);

                Map<Object, Object> links = new HashMap();
                links.put(applicationUrl, new Runnable() {
                    @Override
                    public void run() {
                        BrowserUtil.browse(applicationUrl);
                    }
                });

                final String devUIUrl = runContext.getDevUIURL();
                links.put(devUIUrl, new Runnable() {
                    @Override
                    public void run() {
                        BrowserUtil.browse(devUIUrl);
                    }
                });
                node.putUserData(RunDashboardCustomizer.NODE_LINKS, links);
            }
        }
        return true;
    }

    @Override
    public @Nullable Collection<? extends AbstractTreeNode<?>> getChildren(@NotNull RunDashboardRunConfigurationNode node) {
        List<AbstractTreeNode<?>> children = new ArrayList<>();
        RunContentDescriptor descriptor = node.getDescriptor();
        if (descriptor != null) {
            ProcessHandler processHandler = descriptor.getProcessHandler();
            if (processHandler != null && !processHandler.isProcessTerminated()) {
                OpenDevUITreeNode child = new OpenDevUITreeNode(node.getProject(), (QuarkusRunConfiguration) node.getConfigurationSettings().getConfiguration());
                children.add(child);
            }
        }
        return children;
    }


}
