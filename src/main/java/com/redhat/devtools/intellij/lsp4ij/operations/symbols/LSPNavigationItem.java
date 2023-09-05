package com.redhat.devtools.intellij.lsp4ij.operations.symbols;

import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LSPNavigationItem implements NavigationItem  {


    private final String name;

    public LSPNavigationItem(String name) {
        this.name = name;
    }

    @Override
    public @Nullable @NlsSafe String getName() {
        return name;
    }

    @Override
    public @Nullable ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public @NlsSafe @Nullable String getPresentableText() {
                return name;
            }

            @Override
            public @Nullable Icon getIcon(boolean unused) {
                return null;
            }
        };
    }

    @Override
    public void navigate(boolean requestFocus) {

    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }
}
