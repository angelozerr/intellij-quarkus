/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.hasQuteSupport;

/**
 * Language substitutor for Roq templates located in target/roq-templates.
 *
 * These templates are associated with {@link QuteDebugOnlyLanguage} to enable debugging
 * (breakpoints) without activating LSP features (completion, validation, etc.).
 */
public class RoqTemplateLanguageSubstitutor extends LanguageSubstitutor {

    private static final String ROQ_TEMPLATES_PATH = "target/roq-templates";

    /**
     * Returns the QuteDebugOnlyLanguage if the given file is a Roq template in target/roq-templates.
     *
     * @param file    the virtual file to check, must not be null.
     * @param project the current project, must not be null.
     * @return {@link QuteDebugOnlyLanguage#INSTANCE} if the file is a Roq template, null otherwise.
     */
    @Override
    public @Nullable Language getLanguage(@NotNull VirtualFile file, @NotNull Project project) {
        if (!file.isInLocalFileSystem()) {
            return null;
        }

        // Check if file is in target/roq-templates directory
        if (!isRoqTemplate(file)) {
            return null;
        }

        // Check project-level Qute support (fast, cached)
        if (!hasQuteSupport(project)) {
            return null;
        }

        // Check module-level support
        Module module = LSPIJUtils.getModule(file, project);
        if (module != null && hasQuteSupport(module)) {
            return QuteDebugOnlyLanguage.INSTANCE;
        }

        return null;
    }

    /**
     * Checks if the given file is located in a target/roq-templates directory.
     *
     * @param file the file to check
     * @return true if the file is in target/roq-templates, false otherwise
     */
    private boolean isRoqTemplate(@NotNull VirtualFile file) {
        String path = file.getPath();
        return path.contains("/" + ROQ_TEMPLATES_PATH + "/") ||
               path.contains("\\" + ROQ_TEMPLATES_PATH + "\\");
    }
}
