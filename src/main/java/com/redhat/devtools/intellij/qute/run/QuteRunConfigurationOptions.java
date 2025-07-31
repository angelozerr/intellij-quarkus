package com.redhat.devtools.intellij.qute.run;

import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;
import org.jetbrains.annotations.Nullable;
import com.redhat.devtools.lsp4ij.dap.configurations.options.AttachConfigurable;

public class QuteRunConfigurationOptions extends RunConfigurationOptions implements AttachConfigurable {

    private final StoredProperty<String> attachAddress = string("")
            .provideDelegate(this, "attachAddress");

    private final StoredProperty<String> attachPort = string("")
            .provideDelegate(this, "attachPort");

    @Override
    public @Nullable String getAttachAddress() {
        return attachAddress.getValue(this);
    }

    @Override
    public void setAttachAddress(@Nullable String attachAddress) {
        this.attachAddress.setValue(this, attachAddress);
    }

    @Override
    public @Nullable String getAttachPort() {
        return attachPort.getValue(this);
    }

    @Override
    public void setAttachPort(@Nullable String attachPort) {
        this.attachPort.setValue(this, attachPort);
    }

}
