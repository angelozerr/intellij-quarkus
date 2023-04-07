package com.redhat.devtools.intellij.qute.lang.highlighting;

import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.QuarkusPostStartupActivity;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.textmate.TextMateService;
import org.jetbrains.plugins.textmate.language.TextMateLanguageDescriptor;
import org.jetbrains.plugins.textmate.language.syntax.highlighting.TextMateEditorHighlighterProvider;

import javax.annotation.Nonnull;
import java.util.Objects;

public class QuteEditorHighlighterProvider extends TextMateEditorHighlighterProvider {

    @Override
    public EditorHighlighter getEditorHighlighter(@Nullable Project project, @Nonnull FileType fileType, @Nullable VirtualFile virtualFile, @Nonnull EditorColorsScheme colors) {
        try {
            final TextMateLanguageDescriptor descriptor = TextMateService.getInstance().getLanguageDescriptorByExtension("bicep");
            if (Objects.isNull(descriptor)) { // register textmate if not registered
                if (QuarkusPostStartupActivity.registerBicepTextMateBundle()) {
                    TextMateService.getInstance().reloadEnabledBundles();
                }
            }
            return super.getEditorHighlighter(project, fileType, virtualFile, colors);
        } catch (final ProcessCanceledException e) {
            //LOG.warn(e);
            return new EmptyEditorHighlighter(null);
        }
    }
}
