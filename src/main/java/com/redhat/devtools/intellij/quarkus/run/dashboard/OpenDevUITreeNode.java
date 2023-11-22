package com.redhat.devtools.intellij.quarkus.run.dashboard;

import com.intellij.execution.dashboard.RunDashboardCustomizer;
import com.intellij.execution.services.ServiceViewContributor;
import com.intellij.execution.services.ServiceViewDescriptor;
import com.intellij.execution.services.SimpleServiceViewDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class OpenDevUITreeNode extends AbstractTreeNode<QuarkusRunConfiguration> implements ServiceViewContributor<Object> {
    private String applicationUrl;

    protected OpenDevUITreeNode(Project project, QuarkusRunConfiguration value) {
        super(project, value);
    }

    @Override
    public @NotNull Collection<? extends AbstractTreeNode<?>> getChildren() {
        return null;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        String newName = "Open DevUI";
        presentation.setIcon(AllIcons.RunConfigurations.Web_app);
        presentation.setPresentableText(newName);

        QuarkusRunContext runContext = new QuarkusRunContext(getValue().getModule());
        applicationUrl = runContext.getDevUIURL();
        presentation.addText(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.addText(applicationUrl, SimpleTextAttributes.LINK_ATTRIBUTES);
    }

    @Override
    public boolean isAlwaysExpand() {
        return true;
    }

    @Override
    public @NotNull ServiceViewDescriptor getViewDescriptor(@NotNull Project project) {
        return new SimpleServiceViewDescriptor("", null) {
            @Override
            public @Nullable Object getPresentationTag(Object fragment) {
                if(applicationUrl.equals(fragment)) {
                    return new Runnable() {
                        @Override
                        public void run() {
                            BrowserUtil.browse(applicationUrl);
                        }
                    };
                }
                return null;
            }
        };
    }

    @Override
    public @NotNull List<Object> getServices(@NotNull Project project) {
        return Collections.emptyList();
    }

    @Override
    public @NotNull ServiceViewDescriptor getServiceDescriptor(@NotNull Project project, @NotNull Object service) {
        return null;
    }
}
