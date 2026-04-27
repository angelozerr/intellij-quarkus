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
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;

/**
 * Qute debug-only language used for templates that should support debugging (breakpoints)
 * but not LSP features (completion, validation, etc.).
 *
 * This language is typically used for generated or compiled templates (e.g., Roq templates
 * in target/roq-templates) where we want debugging capabilities without IDE assistance.
 */
public class QuteDebugOnlyLanguage extends Language {

    private static final String QUTE_DEBUG_ONLY_LANGUAGE_ID = "QuteDebugOnly";

    @NotNull
    public static final QuteDebugOnlyLanguage INSTANCE = new QuteDebugOnlyLanguage();

    private QuteDebugOnlyLanguage() {
        super(QUTE_DEBUG_ONLY_LANGUAGE_ID);
    }

    @Override
    public @NotNull @NlsSafe String getDisplayName() {
        return "Qute (Debug Only)";
    }
}
