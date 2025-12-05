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
 * inside @TemplateContents annotations.
 */
public class QuteLanguageInjector implements MultiHostInjector {

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {

        // Only process literal expressions
        if (!(context instanceof PsiLiteralExpression literal)) {
            return;
        }

        // Ensure the literal can be used as a language injection host
        if (!(literal instanceof PsiLanguageInjectionHost host)) {
            return;
        }

        // Check if the literal is inside @TemplateContents annotation
        PsiElement parent = literal.getParent();
        if (parent == null || parent.getParent() == null) {
            return;
        }
        if (!parent.getParent().getText().startsWith("@TemplateContents")) {
            return;
        }

        // -------------------------
        // Calculate inner range of the text block
        // We must avoid including the triple quotes (""")
        // because IntelliJ requires rangeInsideHost to be strictly inside the host
        // -------------------------
        String text = host.getText();
        TextRange hostRange = host.getTextRange();

        int start = hostRange.getStartOffset();
        int end = hostRange.getEndOffset();

        int tripleQuoteStart = text.indexOf("\"\"\"");
        int tripleQuoteEnd = text.lastIndexOf("\"\"\"");

        if (tripleQuoteStart != -1 && tripleQuoteEnd != -1 && tripleQuoteEnd > tripleQuoteStart) {
            // Move start just after the opening triple quotes
            start = hostRange.getStartOffset() + tripleQuoteStart + 3;
            // Move end just before the closing triple quotes
            end = hostRange.getStartOffset() + tripleQuoteEnd;
            // Skip newline after opening """
            if (start < end && text.charAt(tripleQuoteStart + 3) == '\n') {
                start++;
            } else if (start < end && text.charAt(tripleQuoteStart + 3) == '\r') {
                start++;
                if (start < end && text.charAt(tripleQuoteStart + 4) == '\n') {
                    start++;
                }
            }
        }

        TextRange innerRange = new TextRange(start, end);

        // Inject Qute language into the inner range of the text block
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