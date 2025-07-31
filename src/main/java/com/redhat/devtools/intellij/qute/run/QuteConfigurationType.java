package com.redhat.devtools.intellij.qute.run;

import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NotNullLazyValue;
import com.redhat.devtools.intellij.quarkus.lang.QuarkusIcons;
import com.redhat.devtools.lsp4ij.dap.DAPBundle;
import com.redhat.devtools.lsp4ij.dap.configurations.DAPConfigurationFactory;

public class QuteConfigurationType extends ConfigurationTypeBase {

    public static final String ID = "QuteConfiguration";

    public static QuteConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(QuteConfigurationType.class);
    }

    QuteConfigurationType() {
        super(ID,
                "Qute",
                "Qute debugger",
                NotNullLazyValue.createValue(() -> QuarkusIcons.Quarkus));
        addFactory(new QuteConfigurationFactory(this));
    }
}
