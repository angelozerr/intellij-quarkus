/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.NonBlockingReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4ij.client.CoalesceByKey;
import com.redhat.devtools.intellij.lsp4ij.internal.PromiseToCompletableFuture;
import com.redhat.devtools.intellij.lsp4ij.lifecycle.LanguageServerLifecycleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Track file opened / closed to start language servers / disconnect file from language servers.
 */
public class ConnectDocumentToLanguageServerSetupParticipant implements ProjectManagerListener, FileEditorManagerListener {

    private static class ConnectToServer extends PromiseToCompletableFuture<Void> {

        public ConnectToServer(@NotNull VirtualFile file, @NotNull Project project) {
            super(monitor -> {
                startLanguageServer(file, project);
                return null;
            }, "Open file", project, null, new CoalesceByKey(ConnectDocumentToLanguageServerSetupParticipant.class.getName(), file.getUrl()));
        }
    }

    @Override
    public void projectOpened(@NotNull Project project) {
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
    }

    @Override
    public void projectClosing(@NotNull Project project) {
        LanguageServerLifecycleManager.getInstance(project).dispose();
        LanguageServiceAccessor.getInstance(project).projectClosing(project);
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            Project project = source.getProject();
            boolean readAccessAllowed = ApplicationManager.getApplication().isReadAccessAllowed();
            boolean dumb = DumbService.isDumb(project);
            if (readAccessAllowed && !dumb) {
                // Force the start of all languages servers mapped with the given file immediately
                startLanguageServer(file, source.getProject());
            } else {
                new ConnectToServer(file, source.getProject());
            }
        }
    }

    private static void startLanguageServer(@NotNull VirtualFile file, @NotNull Project project) {
        // Force the start of all languages servers mapped with the given file
        // Server capabilities filter is set to null to avoid waiting
        // for the start of the server when server capabilities are checked
        LanguageServiceAccessor.getInstance(project)
                .getLanguageServers(file, null);
    }

}
