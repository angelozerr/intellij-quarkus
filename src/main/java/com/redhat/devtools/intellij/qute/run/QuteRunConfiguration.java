package com.redhat.devtools.intellij.qute.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.dap.DebugAdapterManager;
import com.redhat.devtools.lsp4ij.dap.configurations.DAPRunConfigurationBase;
import com.redhat.devtools.lsp4ij.dap.configurations.DAPRunConfigurationOptions;
import com.redhat.devtools.lsp4ij.dap.configurations.options.AttachConfigurable;
import com.redhat.devtools.lsp4ij.dap.definitions.DebugAdapterServerDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuteRunConfiguration extends DAPRunConfigurationBase<QuteRunConfigurationOptions> implements AttachConfigurable {

    protected QuteRunConfiguration(@NotNull Project project, @Nullable ConfigurationFactory factory, @Nullable String name) {
        super(project, factory, name);
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new QuteRunSettingsEditor();
    }

    @Override
    protected @Nullable DebugAdapterServerDefinition getDebugAdapterServer() {
        return DebugAdapterManager.getInstance().getDebugAdapterServerById("qute");
    }

    @Override
    public @Nullable String getAttachAddress() {
        return getOptions().getAttachAddress();
    }

    @Override
    public void setAttachAddress(@Nullable String attachAddress) {
        getOptions().setAttachAddress(attachAddress);
    }

    @Override
    public @Nullable String getAttachPort() {
        return getOptions().getAttachPort();
    }

    @Override
    public void setAttachPort(@Nullable String attachPort) {
        getOptions().setAttachPort(attachPort);
    }

    @Override
    protected @NotNull QuteRunConfigurationOptions getOptions() {
        return (QuteRunConfigurationOptions) super.getOptions();
    }
}
