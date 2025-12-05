package com.redhat.devtools.intellij.qute.lang;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Injector that applies Qute language syntax highlighting to text blocks
 * inside PsiLiteralExpressions.
 *
 * This implementation safely handles:
 * - Triple-quoted strings ("""...""")
 * - Newlines (\n and \r\n)
 * - Empty or malformed literals
 */
public class QuteLanguageInjector implements MultiHostInjector {

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {

        // Only process literal expressions
        if (!(context instanceof PsiLiteralExpression literal)) {
            return;
        }

        // Ensure the literal can host language injections
        if (!(literal instanceof PsiLanguageInjectionHost host)) {
            return;
        }

        String text = host.getText();
        if (text == null || text.length() < 6) { // minimum length for """ """
            return;
        }

        // Find the first and last triple quotes
        int tripleQuoteStart = text.indexOf("\"\"\"");
        int tripleQuoteEnd = text.lastIndexOf("\"\"\"");
        if (tripleQuoteStart == -1 || tripleQuoteEnd == -1 || tripleQuoteEnd <= tripleQuoteStart) {
            return;
        }

        // Compute inner range inside the triple quotes
        int innerStart = tripleQuoteStart + 3;
        int innerEnd = tripleQuoteEnd;

        // Handle newline just after the opening triple quotes
        if (innerStart < text.length()) {
            char c = text.charAt(innerStart);
            if (c == '\n') {
                innerStart++;
            } else if (c == '\r') {
                innerStart++;
                if (innerStart < text.length() && text.charAt(innerStart) == '\n') {
                    innerStart++;
                }
            }
        }

        // Ensure start < end
        if (innerStart >= innerEnd) {
            return;
        }

        // Create a TextRange relative to the host
        TextRange innerRange = new TextRange(innerStart, innerEnd);

        // Inject Qute language
        registrar.startInjecting(QuteLanguage.INSTANCE)
                .addPlace(null, null, host, innerRange)
                .doneInjecting();
    }

    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        // Only inject into PsiLiteralExpression
        return Collections.singletonList(PsiLiteralExpression.class);
    }
}
