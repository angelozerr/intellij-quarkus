package com.redhat.devtools.intellij.lsp4ij.operations.diagnostics;

import com.intellij.codeInspection.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LSPVirtualFileWrapper;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LSPLocalInspectionTool extends LocalInspectionTool {

    @Override
    public ProblemDescriptor @Nullable [] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (isOnTheFly || !LSPVirtualFileWrapper.hasWrapper(virtualFile)) {
            return super.checkFile(file, manager, isOnTheFly);
        }
        LSPVirtualFileWrapper wrapper = LSPVirtualFileWrapper.getLSPVirtualFileWrapper(virtualFile);
        final Collection<LSPDiagnosticsForServer> diagnosticsPerServer = wrapper.getAllDiagnostics();
        Document document = LSPIJUtils.getDocument(file.getVirtualFile());

        List<ProblemDescriptor> problemDescriptors = new ArrayList<>();
        // Loop for language server which report diagnostics for the given file
        for (var ds :
                diagnosticsPerServer) {
            // Loop for LSP diagnostics to transform it to Intellij annotation.
            for (Diagnostic diagnostic : ds.getDiagnostics()) {
                ProgressManager.checkCanceled();
                Position start = diagnostic.getRange().getStart();
                int offset = LSPIJUtils.toOffset(start, document);
                PsiElement element = file.findElementAt(offset);
                ProblemDescriptor descriptor = manager.createProblemDescriptor(
                        element, // L'élément du code source concerné
                        diagnostic.getMessage(), // Le message à afficher
                        LocalQuickFix.EMPTY_ARRAY, // Tableau de correctifs rapides (s'il y en a)
                        ProblemHighlightType.ERROR, // Le type de mise en évidence (erreur, avertissement, etc.)
                        isOnTheFly, // Est-ce une inspection en temps réel ?
                        false
                );
                problemDescriptors.add(descriptor);
            }
        }


        return problemDescriptors.toArray(new ProblemDescriptor[problemDescriptors.size()]);
    }
}
