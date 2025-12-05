package com.redhat.devtools.intellij.qute.run.client;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightRecordField;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.dap.DAPDebugProcess;
import com.redhat.devtools.lsp4ij.dap.DebugMode;
import com.redhat.devtools.lsp4ij.dap.client.DAPClient;
import com.redhat.devtools.lsp4ij.settings.ServerTrace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QuteDAPClient extends DAPClient implements JavaFileInfoProvider {

    public QuteDAPClient(@NotNull DAPDebugProcess debugProcess,
                         @NotNull Map<String, Object> dapParameters,
                         boolean isDebug,
                         @NotNull DebugMode debugMode,
                         @NotNull ServerTrace serverTrace,
                         @Nullable DAPClient parentClient) {
        super(debugProcess, dapParameters, isDebug, debugMode, serverTrace, parentClient);
    }

    @Override
    public CompletableFuture<JavaFileInfoResponse> getJavaFileInfo(JavaFileInfoRequestArguments args) {

        String typeName = args.getTypeName();
        String method = args.getMethod();
        String annotation = args.getAnnotation();

        var project = getProject();
        CompletableFuture<JavaFileInfoResponse> result = new CompletableFuture<>();

        ReadAction.nonBlocking(() -> {

            PsiClass psiClass = ClassUtil.findPsiClass(
                    PsiManager.getInstance(project),
                    typeName,
                    null,
                    false,
                    GlobalSearchScope.allScope(project)
            );

            if (psiClass == null) {
                result.complete(null);
                return null;
            }

            PsiAnnotation psiAnnotation = psiClass.getAnnotation(annotation);
            if (psiAnnotation == null) {
                result.complete(null);
                return null;
            }

            PsiAnnotationMemberValue value =
                    psiAnnotation.getParameterList().getAttributes()[0].getValue();

            if (!(value instanceof PsiLiteralExpression literal)) {
                result.complete(null);
                return null;
            }

            PsiFile file = psiAnnotation.getContainingFile();
            Document document =
                    PsiDocumentManager.getInstance(project).getDocument(file);

            if (document == null) {
                result.complete(null);
                return null;
            }

            TextRange range = literal.getTextRange();
            String text = literal.getText();
            int startOffset = range.getStartOffset();

            // -----------------------
            // FIX: move inside the literal content """..."""
            // -----------------------
            int tripleIndex = text.indexOf("\"\"\"");
            if (tripleIndex != -1) {
                startOffset = range.getStartOffset() + tripleIndex + 3;

                // Skip the newline after """
                if (startOffset < document.getTextLength()) {
                    CharSequence chars = document.getCharsSequence();
                    char c = chars.charAt(startOffset);

                    if (c == '\n') {
                        startOffset++;
                    } else if (c == '\r') {
                        startOffset++;
                        if (startOffset < document.getTextLength()
                                && chars.charAt(startOffset) == '\n') {
                            startOffset++;
                        }
                    }
                }
            }
            // -----------------------

            int startLine = document.getLineNumber(startOffset);

            JavaFileInfoResponse response = new JavaFileInfoResponse();
            response.setJavaFileUri(LSPIJUtils.toUriAsString(file));
            response.setStartLine(startLine);

            result.complete(response);
            return null;

        }).submit(AppExecutorUtil.getAppExecutorService());

        return result;
    }
}
