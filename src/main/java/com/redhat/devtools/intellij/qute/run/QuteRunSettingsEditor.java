package com.redhat.devtools.intellij.qute.run;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class QuteRunSettingsEditor extends SettingsEditor<QuteRunConfiguration>  {

    private JPanel root = new JPanel();

    @Override
    protected void resetEditorFrom(@NotNull QuteRunConfiguration quteRunConfiguration) {

    }

    @Override
    protected void applyEditorTo(@NotNull QuteRunConfiguration quteRunConfiguration) throws ConfigurationException {

    }

    @Override
    protected @NotNull JComponent createEditor() {
        return root;
    }
}
