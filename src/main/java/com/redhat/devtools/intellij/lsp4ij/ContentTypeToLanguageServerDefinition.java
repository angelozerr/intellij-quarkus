package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.concurrent.CompletableFuture;

public class ContentTypeToLanguageServerDefinition extends AbstractMap.SimpleEntry<Language, LanguageServersRegistry.LanguageServerDefinition> {

    private DocumentMatcher documentMatcher;

    private final DocumentMatcherProvider documentMatcherProvider;

    public ContentTypeToLanguageServerDefinition(@NotNull Language language,
                                                 @NotNull LanguageServersRegistry.LanguageServerDefinition provider,
                                                 @NotNull DocumentMatcherProvider documentMatcherProvider) {
        super(language, provider);
        this.documentMatcherProvider = documentMatcherProvider;
    }

    public boolean match(VirtualFile file, Project project) {
        return getDocumentMatcher().match(file, project);
    }

    public boolean shouldBeMatchedAsynchronously(Project project) {
        return getDocumentMatcher().shouldBeMatchedAsynchronously(project);
    }

    public boolean isEnabled() {
        return getValue().isEnabled();
    }

    public @NotNull <R> CompletableFuture<Boolean> matchAsync(VirtualFile file, Project project) {
        return getDocumentMatcher().matchAsync(file, project);
    }

    public DocumentMatcher getDocumentMatcher() {
        if (documentMatcher == null) {
            documentMatcher = documentMatcherProvider.getDocumentMatcher();
        }
        return documentMatcher;
    }
}
