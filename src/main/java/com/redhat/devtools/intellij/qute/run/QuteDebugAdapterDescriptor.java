package com.redhat.devtools.intellij.qute.run;


import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.fileTypes.FileType;
import com.redhat.devtools.intellij.qute.lang.QuteFileType;
import com.redhat.devtools.lsp4ij.dap.DebugMode;
import com.redhat.devtools.lsp4ij.dap.client.LaunchUtils;
import com.redhat.devtools.lsp4ij.dap.definitions.DebugAdapterServerDefinition;
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class QuteDebugAdapterDescriptor extends DebugAdapterDescriptor {

    public QuteDebugAdapterDescriptor(@NotNull RunConfigurationOptions options,
                                      @NotNull ExecutionEnvironment environment,
                                      @Nullable DebugAdapterServerDefinition serverDefinition) {
        super(options, environment, serverDefinition);
    }

    @Override
    public ProcessHandler startServer() throws ExecutionException {
        return null;
    }

    @Override
    public @NotNull Map<String, Object> getDapParameters() {
        // language=JSON
        String launchJson = """                
                {
                  "type": "qute",
                  "name": "Attach Qute template",
                  "request": "attach"
                }
                """;
        LaunchUtils.LaunchContext context = new LaunchUtils.LaunchContext();
        return LaunchUtils.getDapParameters(launchJson, context);
    }

    @Override
    public @Nullable FileType getFileType() {
        return QuteFileType.QUTE;
    }

    @Override
    public @NotNull DebugMode getDebugMode() {
        return DebugMode.ATTACH;
    }
}
